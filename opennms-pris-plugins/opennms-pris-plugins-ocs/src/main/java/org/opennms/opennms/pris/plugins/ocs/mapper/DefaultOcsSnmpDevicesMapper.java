/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/
 ******************************************************************************
 */
package org.opennms.opennms.pris.plugins.ocs.mapper;

import org.kohsuke.MetaInfServices;
import org.opennms.pris.model.PrimaryType;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionAsset;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.RequisitionNode;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevice;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;
import org.opennms.opennms.pris.plugins.ocs.util.OcsInterfaceUtils;
import org.opennms.pris.api.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.pris.api.InstanceConfiguration;

public class DefaultOcsSnmpDevicesMapper implements Mapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOcsSnmpDevicesMapper.class);

    private final InstanceConfiguration config;

    private final OcsInterfaceUtils interfaceUtils;

    public DefaultOcsSnmpDevicesMapper(final InstanceConfiguration config) {
        this.config = config;
        this.interfaceUtils = new OcsInterfaceUtils(config);
    }

    @Override
    public Requisition map(Object data,
                           Requisition requisition) throws Exception {

        for (final SnmpDevice snmpDevice : ((SnmpDevices) data).getSNMPDevices()) {
            final RequisitionNode requisitionNode = mapSnmpDeviceToRequisitionNode(snmpDevice);
            if (requisitionNode != null) {
                requisition.getNodes().add(requisitionNode);
            }
        }

        return requisition;
    }

    private RequisitionNode mapSnmpDeviceToRequisitionNode(SnmpDevice snmpDevice) {
        final RequisitionNode requisitionNode = new RequisitionNode();
        requisitionNode.setForeignId(snmpDevice.getSNMP().getName());
        requisitionNode.setNodeLabel(snmpDevice.getSNMP().getName());

        String ipAddress = this.interfaceUtils.selectIpAddress(snmpDevice);
        if (ipAddress != null) {
            final RequisitionInterface requisitionInterface = new RequisitionInterface();
            requisitionInterface.setIpAddr(ipAddress);
            requisitionInterface.setDescr("OCS");
            requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
            requisitionInterface.setStatus(1);
            requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService().withServiceName("SNMP"));
            requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService().withServiceName("ICMP"));
            requisitionNode.getInterfaces().add(requisitionInterface);
        } else {
            LOGGER.warn("snmpDevice '{}' named '{}' whith ipAddress '{}', is not valid follworing black- and whitelists.", snmpDevice.getSNMP().getId(), snmpDevice.getSNMP().getName(), snmpDevice
                        .getSNMP().getIPAddr());
        }
        final String ocsSnmpDeviceLink = "<a href=" + this.config.getString("url") + "/ocsreports/index.php?function=snmp_detail&head=1&id=" + snmpDevice.getSNMP().getId() + ">OCS-Inventory</a>";
        requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsSnmpDeviceLink));

        return requisitionNode;
    }

    @MetaInfServices
    public static class Factory implements Mapper.Factory {

        @Override
        public String getIdentifier() {
            return "ocs.devices";
        }

        @Override
        public Mapper create(final InstanceConfiguration config) {
            return new DefaultOcsSnmpDevicesMapper(config);
        }
    }
}
