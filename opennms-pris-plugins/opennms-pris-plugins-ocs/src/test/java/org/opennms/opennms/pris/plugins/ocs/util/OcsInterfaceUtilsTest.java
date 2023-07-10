/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.opennms.pris.plugins.ocs.util;

import org.junit.Before;
import org.junit.Test;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Hardware;
import org.opennms.ocs.inventory.client.response.Network;

import static org.junit.Assert.*;
import org.opennms.pris.api.MockInstanceConfiguration;

public class OcsInterfaceUtilsTest {

    private OcsInterfaceUtils helper;
    
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
    
    private final String IP_WHITE = "1.1.1.1";
    private final String IP_BLACK = "2.2.2.2";
    private final String IP_DEFAULT = "3.3.3.3";

    @Before
    public void setup() {
        helper = new OcsInterfaceUtils(new MockInstanceConfiguration("test"));
        generateNetworks();
        generateComputers();
    }
    
    @Test
    public void selectManagementNetworkTest() {

        assertNotNull(helper.selectManagementNetwork(computerWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetwork(computerWhite).getIPAddress());

        assertNotNull(helper.selectManagementNetwork(computerDefault));
        assertEquals(IP_DEFAULT, helper.selectManagementNetwork(computerDefault).getIPAddress());

        assertNull(helper.selectManagementNetwork(computerBlack));

        assertNotNull(helper.selectManagementNetwork(computerDefaultWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetwork(computerDefaultWhite).getIPAddress());

        assertNotNull(helper.selectManagementNetwork(computerDefaultBlack));
        assertEquals(IP_DEFAULT, helper.selectManagementNetwork(computerDefaultBlack).getIPAddress());

        assertNotNull(helper.selectManagementNetwork(computerBlackWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetwork(computerBlackWhite).getIPAddress());

        assertNotNull(helper.selectManagementNetwork(computerDefaultBlackWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetwork(computerDefaultBlackWhite).getIPAddress());
    }

    @Test
    public void selectManagementNetworkWhiteAndBlackOnlyTest() {

        assertNotNull(helper.selectManagementNetworkWhiteAndBlackOnly(computerWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetworkWhiteAndBlackOnly(computerWhite).getIPAddress());

        assertNull(helper.selectManagementNetworkWhiteAndBlackOnly(computerDefault));
        
        assertNull(helper.selectManagementNetworkWhiteAndBlackOnly(computerBlack));

        assertNotNull(helper.selectManagementNetworkWhiteAndBlackOnly(computerDefaultWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetworkWhiteAndBlackOnly(computerDefaultWhite).getIPAddress());

        assertNull(helper.selectManagementNetworkWhiteAndBlackOnly(computerDefaultBlack));
        
        assertNotNull(helper.selectManagementNetworkWhiteAndBlackOnly(computerBlackWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetworkWhiteAndBlackOnly(computerBlackWhite).getIPAddress());

        assertNotNull(helper.selectManagementNetworkWhiteAndBlackOnly(computerDefaultBlackWhite));
        assertEquals(IP_WHITE, helper.selectManagementNetworkWhiteAndBlackOnly(computerDefaultBlackWhite).getIPAddress());
    }
    
    private void generateNetworks() {
        helper.addIpWhite(IP_WHITE);
        networkWhite = new Network();
        networkWhite.setIPAddress(IP_WHITE);
        networkWhite.setDescription("NetworkWhite");

        helper.addIpBlack(IP_BLACK);
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
        computerWhite.getHardware().setIpaddr(IP_WHITE);
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
        computerDefaultBlack.getHardware().setIpaddr(IP_DEFAULT);
        computerDefaultBlack.getHardware().setName("ComputerDefaultBlack");
        computerDefaultBlack.getNetworks().add(networkDefault);
        computerDefaultBlack.getNetworks().add(networkBlack);

        computerDefaultWhite = new Computer();
        computerDefaultWhite.setHardware(new Hardware());
        computerDefaultWhite.getHardware().setIpaddr(IP_WHITE);
        computerDefaultWhite.getHardware().setName("ComputerDefaultWhite");
        computerDefaultWhite.getNetworks().add(networkDefault);
        computerDefaultWhite.getNetworks().add(networkWhite);

        computerDefaultBlackWhite = new Computer();
        computerDefaultBlackWhite.setHardware(new Hardware());
        computerDefaultBlackWhite.getHardware().setIpaddr(IP_WHITE);
        computerDefaultBlackWhite.getHardware().setName("ComputerDefaultBlackWhite");
        computerDefaultBlackWhite.getNetworks().add(networkDefault);
        computerDefaultBlackWhite.getNetworks().add(networkBlack);
        computerDefaultBlackWhite.getNetworks().add(networkWhite);

        computerBlackWhite = new Computer();
        computerBlackWhite.setHardware(new Hardware());
        computerBlackWhite.getHardware().setIpaddr(IP_WHITE);
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
