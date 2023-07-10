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
package org.opennms.pris.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.integration.api.utils.IPLike;
import org.opennms.pris.api.InstanceConfiguration;

public class InterfaceUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterfaceUtils.class);
    
    private List<String> ipBlackList = new ArrayList<>();
    private List<String> ipWhiteList = new ArrayList<>();

    private final InstanceConfiguration config;
    
    public InterfaceUtils(final InstanceConfiguration config) {
        this.config = config;
    }

    public Boolean isIpBlackListed(String ipAddress) {
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

    public Boolean isIpWhiteListed(String ipAddress) {
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
}
