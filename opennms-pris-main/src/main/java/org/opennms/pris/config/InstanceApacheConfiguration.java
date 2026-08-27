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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.convert.LegacyListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.opennms.pris.api.InstanceConfiguration;

public class InstanceApacheConfiguration extends AbstractApacheConfiguration implements InstanceConfiguration {

    private static org.apache.commons.configuration2.Configuration createConfig(final Path basePath) {
        final Path path = basePath.resolve("requisition.properties");

        // Raise wrapped file not found exception if the config file does not exist
        if (!Files.exists(path)) {
            throw new RuntimeException("Config file not found: " + path);
        }

        // Load via FileHandler directly (no builder, which would require
        // commons-beanutils). No reloading strategy needed: the ConfigManager
        // builds a fresh configuration for every request anyway.
        try {
            final PropertiesConfiguration config = new PropertiesConfiguration();
            config.setListDelimiterHandler(new LegacyListDelimiterHandler(','));
            config.setThrowExceptionOnMissing(true);
            new FileHandler(config).load(path.toFile());
            return config;

        } catch (final ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Builds an instance configuration from an in-memory property map instead
     * of a requisition.properties file.
     *
     * Used to dry-run a candidate configuration that has not been saved yet:
     * the base path still points at the instance folder so relative paths
     * (like {@code source.file = ../inventory.xls}) resolve exactly as they
     * would for the saved configuration - whether or not the folder exists.
     *
     * @param basePath the folder the instance configuration would live in
     * @param instance the name of the instance
     * @param properties the candidate properties
     *
     * @return an instance configuration backed by the given map
     */
    public static InstanceApacheConfiguration fromMap(final Path basePath,
                                                      final String instance,
                                                      final Map<String, String> properties) {
        final MapConfiguration config = new MapConfiguration(new HashMap<String, Object>(properties));
        config.setListDelimiterHandler(new LegacyListDelimiterHandler(','));
        config.setThrowExceptionOnMissing(true);

        return new InstanceApacheConfiguration(basePath, instance, config);
    }

    private final Path basePath;

    private final String instance;

    public InstanceApacheConfiguration(final Path basePath,
                                       final String instance) {
        this(basePath,
             instance,
             createConfig(basePath));
    }

    private InstanceApacheConfiguration(final Path basePath,
                                        final String instance,
                                        final org.apache.commons.configuration2.Configuration config) {
        super(config);

        this.basePath = basePath;
        this.instance = instance;
    }

    @Override
    public Path getBasePath() {
        return this.basePath;
    }

    @Override
    public String getInstanceIdentifier() {
        return this.instance;
    }

    @Override
    public InstanceConfiguration subset(final String prefix) {
        return new InstanceApacheConfiguration(this.basePath,
                                               this.instance,
                                               this.getConfig().subset(prefix));
    }
}
