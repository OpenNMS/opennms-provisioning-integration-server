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

package org.opennms.pris.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.convert.LegacyListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.opennms.pris.api.Configuration;

public class GlobalApacheConfiguration extends AbstractApacheConfiguration implements Configuration {

    private static org.apache.commons.configuration2.Configuration createConfig(final Path base) {
        // Load system and file properties
        final SystemConfiguration systemConfig = new SystemConfiguration();
        systemConfig.setListDelimiterHandler(new LegacyListDelimiterHandler(','));

        // The global.properties stays optional like with Commons
        // Configuration 1.x, which created an empty configuration when the
        // file was absent
        final Path path = base.resolve("global.properties");
        final PropertiesConfiguration propertiesConfig = new PropertiesConfiguration();
        propertiesConfig.setListDelimiterHandler(new LegacyListDelimiterHandler(','));
        if (Files.exists(path)) {
            try {
                new FileHandler(propertiesConfig).load(path.toFile());
            } catch (final ConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        }

        // Build composition of system properties and config file
        final CompositeConfiguration config = new CompositeConfiguration();
        config.addConfiguration(systemConfig);
        config.addConfiguration(propertiesConfig);
        config.setListDelimiterHandler(new LegacyListDelimiterHandler(','));
        config.setThrowExceptionOnMissing(true);
        return config;
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
