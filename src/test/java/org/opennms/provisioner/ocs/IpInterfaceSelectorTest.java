package org.opennms.provisioner.ocs;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Hardware;
import org.opennms.ocs.inventory.client.response.Network;

public class IpInterfaceSelectorTest {

    private Computers computers;
    private Computer computerWhite;
    private Computer computerBlack;
    private Computer computerDefault;
    private Computer computerDefaultWhite;
    private Computer computerBlackWhite;
    private Computer computerDefaultBlack;
    private Computer computerDefaultBlackWhite;
    private Network networkWhite;
    private Network networkBlack;
    private Network networkDefault;
    private List<String> ipBlackList = new ArrayList<>();
    private List<String> ipWhiteList = new ArrayList<>();
    
    private String IP_WHITE = "1.1.1.1";
    private String IP_BLACK = "2.2.2.2";
    private String IP_DEFAULT = "3.3.3.3";

    @Before
    public void setup() {
        generateNetworks();
        generateComputers();
    }

    @Test
    public void ipSelectionTest() {

        Assert.assertNotNull(selectManagementNetwork(computerWhite));
        Assert.assertEquals(IP_WHITE, selectManagementNetwork(computerWhite).getIPAddress());

        Assert.assertNotNull(selectManagementNetwork(computerDefault));
        Assert.assertEquals(IP_DEFAULT, selectManagementNetwork(computerDefault).getIPAddress());

        Assert.assertNull(selectManagementNetwork(computerBlack));

        Assert.assertNotNull(selectManagementNetwork(computerDefaultWhite));
        Assert.assertEquals(IP_WHITE, selectManagementNetwork(computerDefaultWhite).getIPAddress());

        Assert.assertNotNull(selectManagementNetwork(computerDefaultBlack));
        Assert.assertEquals(IP_DEFAULT, selectManagementNetwork(computerDefaultBlack).getIPAddress());

        Assert.assertNotNull(selectManagementNetwork(computerBlackWhite));
        Assert.assertEquals(IP_WHITE, selectManagementNetwork(computerBlackWhite).getIPAddress());

        Assert.assertNotNull(selectManagementNetwork(computerDefaultBlackWhite));
        Assert.assertEquals(IP_WHITE, selectManagementNetwork(computerDefaultBlackWhite).getIPAddress());
    }

    private Network selectManagementNetwork(Computer computer) {

        List<Network> possibleNetworks = new ArrayList<>();
        for (Network network : computer.getNetworks()) {
            //check for a whitelisted interface
            if (isIpWhiteListed(network.getIPAddress())) {
                System.out.println("Match White for: " + computer.getHardware().getName() + "\t" + network.getIPAddress());
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
                System.out.println("Match dummy for: " + computer.getHardware().getName() + "\t" + dummyNetwork.getIPAddress());
                return dummyNetwork;
            }
        } else {
            // use any not whitelisted or blacklisted interface
            System.out.println("Use any for: " + computer.getHardware().getName() + "\t" + possibleNetworks.get(0).getIPAddress());
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

    private void generateNetworks() {
        ipWhiteList.add(IP_WHITE);
        networkWhite = new Network();
        networkWhite.setIPAddress(IP_WHITE);
        networkWhite.setDescription("NetworkWhite");

        ipBlackList.add(IP_BLACK);
        networkBlack = new Network();
        networkBlack.setIPAddress(IP_BLACK);
        networkBlack.setDescription("NetworkBlack");

        networkDefault = new Network();
        networkDefault.setIPAddress(IP_DEFAULT);
        networkDefault.setDescription("NetworkDefault");
    }

    private void generateComputers() {
        computerWhite = new Computer();
        computerWhite.setHardware(new Hardware());
        computerWhite.getHardware().setName("ComputerWhite");
        computerWhite.getNetworks().add(networkWhite);

        computerBlack = new Computer();
        computerBlack.setHardware(new Hardware());
        computerBlack.getHardware().setIpaddr(IP_BLACK);
        computerBlack.getHardware().setName("ComputerBlack");
        computerBlack.getNetworks().add(networkBlack);

        computerDefault = new Computer();
        computerDefault.setHardware(new Hardware());
        computerDefault.getHardware().setIpaddr(IP_DEFAULT);
        computerDefault.getHardware().setName("ComputerDefault");
        computerDefault.getNetworks().add(networkDefault);

        computerDefaultBlack = new Computer();
        computerDefaultBlack.setHardware(new Hardware());
        computerDefaultBlack.getHardware().setName("ComputerDefaultBlack");
        computerDefaultBlack.getNetworks().add(networkDefault);
        computerDefaultBlack.getNetworks().add(networkBlack);

        computerDefaultWhite = new Computer();
        computerDefaultWhite.setHardware(new Hardware());
        computerDefaultWhite.getHardware().setName("ComputerDefaultWhite");
        computerDefaultWhite.getNetworks().add(networkDefault);
        computerDefaultWhite.getNetworks().add(networkWhite);

        computerDefaultBlackWhite = new Computer();
        computerDefaultBlackWhite.setHardware(new Hardware());
        computerDefaultBlackWhite.getHardware().setName("ComputerDefaultBlackWhite");
        computerDefaultBlackWhite.getNetworks().add(networkDefault);
        computerDefaultBlackWhite.getNetworks().add(networkBlack);
        computerDefaultBlackWhite.getNetworks().add(networkWhite);

        computerBlackWhite = new Computer();
        computerBlackWhite.setHardware(new Hardware());
        computerBlackWhite.getHardware().setName("ComputerBlackWhite");
        computerBlackWhite.getNetworks().add(networkBlack);
        computerBlackWhite.getNetworks().add(networkWhite);

        computers = new Computers();
        computers.getComputers().add(computerWhite);
        computers.getComputers().add(computerBlack);
        computers.getComputers().add(computerDefaultBlack);
        computers.getComputers().add(computerDefaultBlackWhite);
        computers.getComputers().add(computerBlackWhite);
        computers.getComputers().add(computerDefault);
    }
}