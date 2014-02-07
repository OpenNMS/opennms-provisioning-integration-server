/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/
package org.opennms.pris.ocs.mapper;

import com.google.common.collect.Sets;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Entry;
import org.opennms.ocs.inventory.client.response.Network;
import org.opennms.pris.IpInterfaceHelper;
import org.opennms.pris.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class DefaultOcsComputerMapper implements Mapper {

    public static class Factory implements Mapper.Factory {

        @Override
        public Mapper create(final String instance,
                             final Configuration config) {
            return new DefaultOcsComputerMapper(instance, config);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOcsComputerMapper.class);
    private static final String OCS_ACCOUNTINFO = "ocs.accountinfo";
    private final String instance;
    private final Configuration config;

    private final IpInterfaceHelper ipInterfaceHelper = new IpInterfaceHelper();

    public DefaultOcsComputerMapper(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Requisition map(Object data, Requisition requisition) throws Exception {

        for (final Computer computer : ((Computers) data).getComputers()) {

            final RequisitionNode requisitionNode = mapComputerToRequisitionNode(computer);
            if (requisitionNode != null) {
                requisition.getNodes().add(requisitionNode);
            }
        }

        return requisition;
    }

    private RequisitionNode mapComputerToRequisitionNode(Computer computer) {
        final RequisitionNode requisitionNode = new RequisitionNode();
        requisitionNode.setForeignId(computer.getHardware().getName());
        requisitionNode.setNodeLabel(computer.getHardware().getName());

        if (config.containsKey(OCS_ACCOUNTINFO) && !config.getString(OCS_ACCOUNTINFO).isEmpty()) {
            Set<String> requiredAccountInfos = Sets.newHashSet(config.getString(OCS_ACCOUNTINFO).split("\\s+"));
            Set<String> availableAccountInfos = new HashSet<>();
            for (Entry accountInfo : computer.getAccountInfo().getEntries()) {
                availableAccountInfos.add(accountInfo.getName() + "." + accountInfo.getValue());
            }
            boolean matches = availableAccountInfos.containsAll(requiredAccountInfos);

            if (!matches) {
                LOGGER.debug("skip computer {}, does not match accountinfo filter", computer.getHardware().getName());
                return null;
            }
        }

        final RequisitionInterface requisitionInterface = new RequisitionInterface();

        final Network managementNetwork = this.ipInterfaceHelper.selectManagementNetwork(computer);
        if (managementNetwork != null) {
            requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
            requisitionInterface.setDescr(managementNetwork.getDescription());
            requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
            requisitionInterface.setStatus(1);
            requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
            requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
            requisitionNode.getInterfaces().add(requisitionInterface);
        } else {
            LOGGER.warn("computer '{}' named '{}' has no electable ip-address following the black- and whitelists.", computer.getHardware().getId(), computer.getHardware().getName());
        }

        requisitionNode.getCategories().addAll(ipInterfaceHelper.populateCategories(computer, config, instance));

        requisitionNode.getAssets().add(new RequisitionAsset("operatingSystem", ipInterfaceHelper.assetStringCleaner(computer.getHardware().getOsname(), 64)));
        requisitionNode.getAssets().add(new RequisitionAsset("cpu", ipInterfaceHelper.assetStringCleaner(computer.getHardware().getProcessort(), 64)));

        final String ocsComputerLink = "<a href=" + this.config.getString("ocs.url") + "/index.php?function=computer&head=1&systemid=" + computer.getHardware().getId() + ">OCS-Inventory</a>";
        requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsComputerLink));

        return requisitionNode;
    }
}
