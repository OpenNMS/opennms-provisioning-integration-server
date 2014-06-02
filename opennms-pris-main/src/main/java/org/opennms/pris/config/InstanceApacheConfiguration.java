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
 * http://www.opennms.org/ http://www.opennms.com/ *****************************************************************************
 */
package org.opennms.pris.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.opennms.pris.api.InstanceConfiguration;

public class InstanceApacheConfiguration extends AbstractApacheConfiguration implements InstanceConfiguration {

    private static org.apache.commons.configuration.Configuration createConfig(final Path base,
                                                                               final String instance) {
        final Path path = base.resolve(instance).resolve("requisition.properties");

        // Raise wrapped file not found exception if the config file does not exist
        if (!Files.exists(path)) {
            throw new RuntimeException("Config file not found: " + path);
        }
        
        // Load system and file properties
        final org.apache.commons.configuration.PropertiesConfiguration propertiesConfig;
        final org.apache.commons.configuration.MapConfiguration mapConfig;
        try {
            propertiesConfig = new org.apache.commons.configuration.PropertiesConfiguration(path.toFile()) {{
                setThrowExceptionOnMissing(true);
                setReloadingStrategy(new FileChangedReloadingStrategy());
            }};
            
            mapConfig = new org.apache.commons.configuration.MapConfiguration(Collections.singletonMap("requisition",
                                                                              (Object) instance));
            
        } catch (final ConfigurationException ex) {
            throw new RuntimeException(ex);
        }

        return new CompositeConfiguration() {{
                addConfiguration(propertiesConfig);
                addConfiguration(mapConfig);
            }
        };
    }

    private final String instance;

    public InstanceApacheConfiguration(final Path base,
                                       final String instance) {
        this(createConfig(base,
                           instance),
             instance);
    }

    private InstanceApacheConfiguration(final org.apache.commons.configuration.Configuration config,
                                        final String instance) {
        super(config);

        this.instance = instance;
    }

    @Override
    public String getInstanceIdentifier() {
        return this.instance;
    }

    @Override
    public InstanceConfiguration subset(final String prefix) {
        return new InstanceApacheConfiguration(this.getConfig().subset(prefix),
                                               instance);
    }
}
