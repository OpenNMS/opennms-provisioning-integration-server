package org.opennms.provisioner.ocs;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpInterfaceHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IpInterfaceHelper.class);
    
    private List<String> ipBlackList = new ArrayList<>();
    private List<String> ipWhiteList = new ArrayList<>();
    
    public Network selectManagementNetworkWhiteAndBlackOnly(Computer computer) {
        List<Network> possibleNetworks = new ArrayList<>();
        for (Network network : computer.getNetworks()) {
            if (isIpWhiteListed(network.getIPAddress()) && !isIpBlackListed(network.getIPAddress())) {
                possibleNetworks.add(network);
            }
        }
        if (possibleNetworks.size() >= 1) {
            return possibleNetworks.get(0);
        } else {
            String ocsPickedIp = computer.getHardware().getIpaddr();
            if(isIpWhiteListed(ocsPickedIp) && !isIpBlackListed(ocsPickedIp)) {
                Network fakeNetwork = new Network();
                fakeNetwork.setIPAddress(ocsPickedIp);
                return fakeNetwork;
            } else {
                return null;
            }
        }
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
            if (ipAddress.startsWith(blackedIp)) {
                return true;
            }
        }
        return false;
    }

    private Boolean isIpWhiteListed(String ipAddress) {
        for (String whiteIp : ipWhiteList) {
            if (ipAddress.startsWith(whiteIp)) {
                return true;
            }
        }
        return false;
    }
    
    public void addIpWhite(String ip) {
        ipWhiteList.add(ip);
    }
    
    public void addIpBlack(String ip) {
        ipBlackList.add(ip);
    }
    
    public void initListsFromConfigs() {
        //TODO make the black- and white-lists configurable in a nice way
        try {
            ipBlackList = Files.readAllLines(Paths.get("./", "blackList.properties"), Charset.forName("UTF-8"));
            ipWhiteList = Files.readAllLines(Paths.get("./", "whiteList.properties"), Charset.forName("UTF-8"));
        } catch (IOException ex) {
            LOGGER.error("blackList and or whiteList could not be read from files, using empty lists.", ex);
            ipBlackList = new ArrayList<>();
            ipWhiteList = new ArrayList<>();
        } 
    }
}