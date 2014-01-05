package org.opennms.provisioner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.opennms.core.utils.IPLike;
import org.opennms.netmgt.provision.persist.requisition.RequisitionCategory;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Entry;
import org.opennms.ocs.inventory.client.response.Network;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpInterfaceHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(IpInterfaceHelper.class);
    private List<String> ipBlackList = new ArrayList<>();
    private List<String> ipWhiteList = new ArrayList<>();

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
        if (possibleNetworks.size() >= 1) {
            return possibleNetworks.get(0);
        } else {
            String ocsPickedIp = computer.getHardware().getIpaddr();
            if (!ocsPickedIp.isEmpty()) {
                if (isIpWhiteListed(ocsPickedIp) && !isIpBlackListed(ocsPickedIp)) {
                    Network fakeNetwork = new Network();
                    fakeNetwork.setIPAddress(ocsPickedIp);
                    return fakeNetwork;
                } else {
                    return null;
                }
            } else {
                LOGGER.debug("FALL BACK TO OCS PICKED IP BUT IP WAS EMPTY FOR COMPUTER ID:{} NAME:{}", computer.getHardware().getId(), computer.getHardware().getName());
                return null;
            }
        }
    }

    public String selectIpAddressWhiteAndBlackOnly(SnmpDevice snmpDevice) {
        String ipAddr = snmpDevice.getSNMP().getIPAddr();
        if (isIpWhiteListed(ipAddr) && !isIpBlackListed(ipAddr)) {
            return ipAddr;
        }
        return null;
    }

    public String selectIpAddress(SnmpDevice snmpDevice) {
        String ipAddr = snmpDevice.getSNMP().getIPAddr();
        if (isIpBlackListed(ipAddr)) {
            return null;
        }
        return ipAddr;
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

    private Boolean isIpBlackListed(String ipAddress) {
        for (String blackedIp : ipBlackList) {
            if (IPLike.matches(ipAddress, blackedIp)) {
                LOGGER.debug("IpAddress Black: {} \t vs \t {} \t OK", ipAddress, blackedIp);
                return true;
            } else {
                LOGGER.debug("IpAddress Black: {} \t vs \t {} \t no", ipAddress, blackedIp);
            }
        }
        return false;
    }

    private Boolean isIpWhiteListed(String ipAddress) {
        for (String whiteIp : ipWhiteList) {
            if (IPLike.matches(ipAddress, whiteIp)) {
                LOGGER.debug("IpAddress White: {} \t vs \t {} \t OK", ipAddress, whiteIp);
                return true;
            } else {
                LOGGER.debug("IpAddress White: {} \t vs \t {} \t no", ipAddress, whiteIp);
            }
        }
        return false;
    }

    public void addIpWhite(String ip) {
        try {
            //This check forces valid IPLike syntax
            IPLike.matches("1.1.1.1", ip);
            ipWhiteList.add(ip);
        } catch (Exception ex) {
            LOGGER.error("WhiteList rejected illegal entry {}", ip, ex);
        }
    }

    public void addIpBlack(String ip) {
        try {
            //This check forces valid IPLike syntax
            IPLike.matches("1.1.1.1", ip);
            ipBlackList.add(ip);
        } catch (Exception ex) {
            LOGGER.error("BlackList rejected illegal entry {}", ip, ex);
        }
    }

    public void initListsFromConfigs() {
        try {
            List<String> rawBlackedList = Files.readAllLines(Paths.get("./", "blackList.properties"), Charset.forName("UTF-8"));
            for (String rawBlacked : rawBlackedList) {
                addIpBlack(rawBlacked);
            }

            List<String> rawWhiteList = Files.readAllLines(Paths.get("./", "whiteList.properties"), Charset.forName("UTF-8"));
            for (String rawWhite : rawWhiteList) {
                addIpWhite(rawWhite);
            }

        } catch (IOException ex) {
            LOGGER.error("blackList and or whiteList could not be read from files, using empty lists.", ex);
            ipBlackList = new ArrayList<>();
            ipWhiteList = new ArrayList<>();
        }
    }

//TODO Move to OCS-Helper
    public List<RequisitionCategory> populateCategories(Computer myComputer, Configuration config, String instance) {
        List<RequisitionCategory> categories = new ArrayList<>();

        if (!config.getString("categoryMap").isEmpty()) {
            Properties catMap = new Properties();
            try {
                File categoryMap = new File(config.getString("categoryMap"));
                catMap.load(new FileInputStream(instance + File.separator+ categoryMap));
                LOGGER.info("Loaded properties from {}", categoryMap.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Could not read category mappings from", e);
                throw new RuntimeException(e);
            }

            for (Entry entry : myComputer.getAccountInfo().getEntries()) {
                if ("".equals(entry.getValue())) continue;
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

    public String assetStringCleaner(String assetString, Integer maxSize) {
        String result = "";
        if (assetString != null) {
            result = assetString;
            //Trademarks
            result = result.replace("®", "");
            result = result.replace("(R)", "");
            result = result.replace("(tm)", "");

            //OperatingSystems
            result = result.replace("Microsoft", "MS");
            result = result.replace("Service Pack", "SP");
            result = result.replace("CentOS release", "CentOS");
            result = result.replace("Red Hat Enterprise Linux Server release", "Red Hat Linux");

            //duplicate spaces
            result = result.replaceAll("\\s+", " ");

            if (result.length() > maxSize) {
                result = result.substring(0, maxSize);
            }
        }
        return result;
    }
}