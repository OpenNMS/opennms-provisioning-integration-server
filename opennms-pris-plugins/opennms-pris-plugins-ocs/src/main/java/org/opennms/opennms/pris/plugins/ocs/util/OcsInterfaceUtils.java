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
package org.opennms.opennms.pris.plugins.ocs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Entry;
import org.opennms.ocs.inventory.client.response.Network;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevice;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.model.RequisitionAsset;
import org.opennms.pris.model.RequisitionCategory;
import org.opennms.pris.util.InterfaceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsInterfaceUtils extends InterfaceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcsInterfaceUtils.class);
    private final String CONFIG_PARAMETER_ASSET_MAP = "assetMap";
    private final String CONFIG_PARAMETER_CATEGORY_MAP = "categoryMap";
    
    public OcsInterfaceUtils(final InstanceConfiguration config) {
        super(config);
    }

    /**
     * Returns null if no Network was found that is whitelisted and not
     * blacklisted.
     */
    public Network selectManagementNetworkWhiteAndBlackOnly(Computer computer) {
        List<Network> possibleNetworks = new ArrayList<>();
        for (Network network : computer.getNetworks()) {
            if (!network.getIPAddress().isEmpty()) {
                if (isIpWhiteListed(network.getIPAddress()) && !isIpBlackListed(network.getIPAddress())) {
                    possibleNetworks.add(network);
                }
            } else {
                LOGGER.debug("FOUND A NETWORK WITHOUT IPADDRESS FOR COMPUTER ID:{} NAME:{}", computer.getHardware().getId(), computer.getHardware().getName());
            }
        }
        //multiple networks are valid options. pick the first one.
        if (possibleNetworks.size() >= 1) {
            return possibleNetworks.get(0);
        } else {
            //no valid network is listed. check the ip that was selected by ocs it self.
            String ocsPickedIp = computer.getHardware().getIpaddr();
            if (!ocsPickedIp.isEmpty()) {
                if (isIpWhiteListed(ocsPickedIp) && !isIpBlackListed(ocsPickedIp)) {
                    //the ocs selected ip is not listed in the networks. create a fake netork and return it.
                    Network fakeNetwork = new Network();
                    fakeNetwork.setIPAddress(ocsPickedIp);
                    return fakeNetwork;
                } else {
                    //no valid network or address found.
                    return null;
                }
            } else {
                LOGGER.debug("FALL BACK TO OCS PICKED IP BUT IP WAS EMPTY FOR COMPUTER ID:{} NAME:{}", computer.getHardware().getId(), computer.getHardware().getName());
                return null;
            }
        }
    }

    public String selectIpAddressWhiteAndBlackOnly(SnmpDevice snmpDevice) {
        if (snmpDevice != null) {
            String ipAddr = snmpDevice.getSNMP().getIPAddr();
            if (isIpWhiteListed(ipAddr) && !isIpBlackListed(ipAddr)) {
                return ipAddr;
            }
        }
        return null;
    }

    /**
     * return null if the ipAddress of the snmpdevice is blacklisted. And if the
     * snmpDevice is null.
     */
    public String selectIpAddress(SnmpDevice snmpDevice) {
        if (snmpDevice != null) {
            String ipAddr = snmpDevice.getSNMP().getIPAddr();
            if (!isIpBlackListed(ipAddr)) {
                return ipAddr;
            }
        }
        return null;
    }

    public Network selectManagementNetwork(Computer computer) {
        List<Network> possibleNetworks = new ArrayList<>();
        for (Network network : computer.getNetworks()) {
            //check for a whitelisted interface
            if (isIpWhiteListed(network.getIPAddress())) {
                LOGGER.debug("Match White for: " + computer.getHardware().getName() + "\t" + network.getIPAddress());
                return network;
            } else if (!isIpBlackListed(network.getIPAddress())) {
                possibleNetworks.add(network);
            }
        }

        if (possibleNetworks.isEmpty()) {
            if (isIpBlackListed(computer.getHardware().getIpaddr())) {
                return null;
            } else {
                Network dummyNetwork = new Network();
                dummyNetwork.setIPAddress(computer.getHardware().getIpaddr());
                LOGGER.debug("Match dummy for: " + computer.getHardware().getName() + "\t" + dummyNetwork.getIPAddress());
                return dummyNetwork;
            }
        } else {
            // use any not whitelisted or blacklisted interface
            LOGGER.debug("Use any for: " + computer.getHardware().getName() + "\t" + possibleNetworks.get(0).getIPAddress());
            return possibleNetworks.get(0);
        }
    }

    public List<RequisitionCategory> populateCategories(Computer myComputer, InstanceConfiguration config, String instance) {
        LOGGER.info("Mapping OCS-Accountinfo to Categories");
        
        List<RequisitionCategory> categories = new ArrayList<>();

        if (config.getString(CONFIG_PARAMETER_CATEGORY_MAP) != null && !config.getString(CONFIG_PARAMETER_CATEGORY_MAP).isEmpty()) {
            Properties catMap = new Properties();
            try {
                File categoryMap =  config.getPath(CONFIG_PARAMETER_CATEGORY_MAP).toFile();
                catMap.load(new FileInputStream(categoryMap));
                LOGGER.info("Loaded properties from {}", categoryMap.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not read category mappings from", e);
                throw new RuntimeException(e);
            }

            for (Entry entry : myComputer.getAccountInfo().getEntries()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                LOGGER.info("On computer {} got an accountinfo entry called {} with value {}", myComputer.getHardware().getName(), entry.getName(), entry.getValue());
                if (catMap.containsKey(entry.getName() + "." + entry.getValue())) {
                    categories.add(new RequisitionCategory(catMap.get(entry.getName() + "." + entry.getValue()).toString()));
                } else {
                    LOGGER.info("NOT Adding category {}.{} to node {}", entry.getName(), entry.getValue(), myComputer.getHardware().getName());
                }
            }
        }
        return categories;
    }

    public List<RequisitionAsset> populateAssets(Computer myComputer, InstanceConfiguration config, String instance) {
        LOGGER.info("Mapping OCS-Accountinfo to Assets");
        
        List<RequisitionAsset> assets = new ArrayList<>();
        
        if (config.getString(CONFIG_PARAMETER_ASSET_MAP) != null && !config.getString(CONFIG_PARAMETER_ASSET_MAP).isEmpty()) {
            Properties categoryMap = new Properties();
            try {
                File assetMappingFile = config.getPath(CONFIG_PARAMETER_ASSET_MAP).toFile();
                categoryMap.load(new FileInputStream(assetMappingFile));
                LOGGER.info("Loaded properties from {}", assetMappingFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not read asset mappings from", e);
                throw new RuntimeException(e);
            }

            for (Entry ocsAccountInfoEntry : myComputer.getAccountInfo().getEntries()) {
                if (ocsAccountInfoEntry.getValue().isEmpty()) {
                    continue;
                }
                LOGGER.info("On computer {} got an accountinfo entry called {} with value {}", myComputer.getHardware().getName(), ocsAccountInfoEntry.getName(), ocsAccountInfoEntry.getValue());
                if (categoryMap.containsKey(ocsAccountInfoEntry.getName())) {
                    RequisitionAsset asset = new RequisitionAsset(categoryMap.getProperty(ocsAccountInfoEntry.getName()), ocsAccountInfoEntry.getValue());
                    assets.add(asset);
                } else {
                    LOGGER.info("NOT Adding accountinfo data {}.{} as asset to node {}", ocsAccountInfoEntry.getName(), ocsAccountInfoEntry.getValue(), myComputer.getHardware().getName());
                }
            }
        }
        return assets;
    }
}
