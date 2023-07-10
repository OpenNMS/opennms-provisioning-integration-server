/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014-2023 The OpenNMS Group, Inc.
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

package org.opennms.pris.config;

import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.opennms.pris.api.Configuration;

public class GlobalApacheConfiguration extends AbstractApacheConfiguration implements Configuration {

    private static org.apache.commons.configuration.Configuration createConfig(final Path base) {
        // Load system and file properties
        final org.apache.commons.configuration.SystemConfiguration systemConfig;
        final org.apache.commons.configuration.PropertiesConfiguration propertiesConfig;
        try {
            systemConfig = new org.apache.commons.configuration.SystemConfiguration();
            
            propertiesConfig = new org.apache.commons.configuration.PropertiesConfiguration(base.resolve("global.properties").toFile());
            
        } catch (final ConfigurationException ex) {
            throw new RuntimeException(ex);
        }

        // Build composition of system properties and config file
        final var ret = new CompositeConfiguration(Arrays.asList(systemConfig, propertiesConfig));
        ret.setThrowExceptionOnMissing(true);
        return ret;
    }

    private final Path basePath;
    
    public GlobalApacheConfiguration(final Path basePath) {
        super(createConfig(basePath));
        
        this.basePath = basePath;
    }

    @Override
    public Path getBasePath() {
        return this.basePath;
    }
}
