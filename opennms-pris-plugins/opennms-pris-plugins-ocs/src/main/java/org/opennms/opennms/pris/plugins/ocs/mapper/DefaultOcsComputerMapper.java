/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C)
 * 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/
 * *****************************************************************************
 */
package org.opennms.opennms.pris.plugins.ocs.mapper;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kohsuke.MetaInfServices;
import org.opennms.pris.model.PrimaryType;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionAsset;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.RequisitionNode;
import org.opennms.ocs.inventory.client.response.Bios;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Drive;
import org.opennms.ocs.inventory.client.response.Entry;
import org.opennms.ocs.inventory.client.response.Network;
import org.opennms.ocs.inventory.client.response.Sound;
import org.opennms.ocs.inventory.client.response.Storage;
import org.opennms.ocs.inventory.client.response.Video;
import org.opennms.opennms.pris.plugins.ocs.util.OcsInterfaceUtils;
import org.opennms.pris.api.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.util.AssetUtils;

public class DefaultOcsComputerMapper implements Mapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOcsComputerMapper.class);

    private static final String OCS_ACCOUNTINFO = "accountinfo";
    
    private final InstanceConfiguration config;

    private final OcsInterfaceUtils interfaceUtils;

    public DefaultOcsComputerMapper(final InstanceConfiguration config) {
        this.config = config;
        this.interfaceUtils = new OcsInterfaceUtils(config);
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

        final Network managementNetwork = this.interfaceUtils.selectManagementNetwork(computer);
        if (managementNetwork != null) {
            requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
            requisitionInterface.setDescr(managementNetwork.getDescription());
            requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
            requisitionInterface.setStatus(1);
            requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService().withServiceName("SNMP"));
            requisitionInterface.getMonitoredServices().add(new RequisitionMonitoredService().withServiceName("ICMP"));
            requisitionNode.getInterfaces().add(requisitionInterface);
        } else {
            LOGGER.warn("computer '{}' named '{}' has no electable ip-address following the black- and whitelists.", computer.getHardware().getId(), computer.getHardware().getName());
        }

        requisitionNode.getCategories().addAll(interfaceUtils.populateCategories(computer, config, config.getInstanceIdentifier()));

        requisitionNode.getAssets().add(new RequisitionAsset("operatingSystem", AssetUtils.assetStringCleaner(computer.getHardware().getOsname(), 64)));
        requisitionNode.getAssets().add(new RequisitionAsset("cpu", AssetUtils.assetStringCleaner(computer.getHardware().getProcessort(), 64)));
        requisitionNode.getAssets().add(new RequisitionAsset("ram", AssetUtils.assetStringCleaner(computer.getHardware().getMemory() + " MB", 10)));

        Bios biosData = computer.getBios();
        if (biosData != null) {
            requisitionNode.getAssets().add(new RequisitionAsset("manufacturer", AssetUtils.assetStringCleaner(biosData.getSManufacturer(), 64)));
            requisitionNode.getAssets().add(new RequisitionAsset("modelNumber", AssetUtils.assetStringCleaner(biosData.getSModel(), 64)));
            requisitionNode.getAssets().add(new RequisitionAsset("serialNumber", AssetUtils.assetStringCleaner(biosData.getSSN(), 64)));
        }

        List<Drive> drives = computer.getDrives();
        if (drives != null) {

            int i = 1;
            for (Drive drive : drives) {

                LOGGER.debug("drive '{}'", drive);
                String hddAsset = "";

                // Windows drives
                if (drive.getType().equalsIgnoreCase("Hard Drive")) {
                    hddAsset = drive.getLetter() + " is " + drive.getFilesystem() + " as " + drive.getVolumn() + " " + drive.getFree() + "/" + drive.getTotal() + "MB";
                } // Linxu/Unix drives
                else if (drive.getType().startsWith("/")) {
                    hddAsset = drive.getType() + " is " + drive.getFilesystem() + " " + drive.getFree() + "/" + drive.getTotal() + "MB";
                }

                if (i <= 6 && !hddAsset.isEmpty()) {
                    LOGGER.debug("Adding Asset hdd{} to Node '{}' as '{}'", i, requisitionNode.getNodeLabel(), hddAsset);
                    requisitionNode.getAssets().add(new RequisitionAsset("hdd" + i, AssetUtils.assetStringCleaner(hddAsset, 64)));
                } else {
                    LOGGER.debug("node {} ignorring drive {} as hdd{} - string is empty or no hdd asset filed left", requisitionNode.getNodeLabel(), drive.toString(), i);
                }
                i++;
            }
        }

        List<Storage> storages = computer.getStorages();
        if (storages != null) {
            for (Storage storage : storages) {
                LOGGER.debug("Node '{}' Storage '{}'", requisitionNode.getNodeLabel(), storage.toString());
            }
        }
        
        List<Video> videos = computer.getVideos();
        if (videos != null) {
            for (Video video : videos) {
                LOGGER.debug("Node '{}' Video '{}'", requisitionNode.getNodeLabel(), video.toString());
            }
        }
        
        List<Sound> sounds = computer.getSounds();
        for (Sound sound : sounds) {
            sound.getDescription();
        }
        
	final String ocsComputerLink = "<a href=" + this.config.getString("ocs.url") + "/ocsreports/index.php?function=computer&head=1&systemid=" + computer.getHardware().getId() + ">OCS-Inventory</a>";
        requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsComputerLink));

        return requisitionNode;
    }

    @MetaInfServices
    public static class Factory implements Mapper.Factory {

        @Override
        public String getIdentifier() {
            return "ocs.computers";
        }

        @Override
        public Mapper create(final InstanceConfiguration config) {
            return new DefaultOcsComputerMapper(config);
        }
    }
}
