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

package org.opennms.pris;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.opennms.pris.api.Configuration;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.config.GlobalApacheConfiguration;
import org.opennms.pris.config.InstanceApacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration manager.
 *
 * The manager provides a global configuration and a configuration for each instance that represents the requisition name.
 *
 * The configuration base path is the current working directory and can be overwritten by the {@literal config} system property.
 *
 * The global configuration is loaded from the {@literal config.properties} in the base path. These properties can be overwritten by the system properties.
 *
 * The instance configurations are loaded from sub-folders of the {@literal requisitions} folder relative to the configuration base path where the instance name is the folder name.
 *
 * @author Dustin Frisch &lt;fooker@lab.sh&gt;
 */
public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    // The folder relative to the config base path holding the instance configurations
    private static final String REQUISITIONS_FOLDER = "requisitions";

    // The base path of the config
    private final Path base;

    // The global configuration
    private GlobalApacheConfiguration globalConfig;

    public ConfigManager() {
        // Get the config base folder and fall back to the CWD
        final String cwd = System.getProperty("user.dir");
        this.base = Paths.get(System.getProperty("pris.config", cwd));

        // Build the global configuration
        this.globalConfig = new GlobalApacheConfiguration(base);
    }

    public Configuration getGlobalConfig() {
        return this.globalConfig;
    }

    /**
     * Returns all known instance names.
     *
     * All sub-folders of the {@literal requisitions} folder having a {@literal requisition.properties} file are interpreted as instance configurations.
     *
     * @return a collection of instance names
     */
    public Collection<String> getInstances() {
        return this.getInstances("*");
    }

    /**
     * Returns all known instance names matching the provided glob.
     *
     * All sub-folders of the {@literal requisitions} folder matching the passed glob and having a {@literal requisition.properties} file are interpreted as instance configurations.
     *
     * The provided glob pattern is used to limit the returned instances to those matching the glob. To return all instances, {@code "*"} can be passed.
     *
     * @param glob the glob pattern
     *
     * @return a collection of instance names
     */
    public Collection<String> getInstances(final String glob) {
        final Path requisitions = this.getRequisitionsPath();

        // A missing requisitions folder is an operator state, not an error - there are just no instances
        if (!Files.isDirectory(requisitions)) {
            LOGGER.warn("Requisitions folder does not exist: {}", requisitions.toAbsolutePath());
            return Collections.emptyList();
        }

        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(requisitions, glob)) {

            // The list of found instances
            final Collection<String> instances = new ArrayList<>();

            // Loop over the stream of child files to find all instances
            for (final Path path : stream) {
                // An instance must be a directory and must contain the properties file
                if (!Files.isDirectory(path) ||
                    !Files.exists(path.resolve("requisition.properties"))) {
                    continue;
                }

                // Get the name of the folder relative to the requisitions folder and add it to
                // the list of known instances
                instances.add(requisitions.relativize(path).toString());
            }

            return instances;

        } catch (final IOException ex) {
            throw new RuntimeException("Unable to traverse config folder", ex);
        }
    }

    /**
     * Return the instance configuration.
     *
     * The instance configuration is loaded from the folder specified by the parameter {@literal instance}.
     *
     * @param instance the instance name
     *
     * @return the instance configuration
     */
    public InstanceConfiguration getInstanceConfig(final String instance) {
        return new InstanceApacheConfiguration(this.getRequisitionsPath().resolve(instance),
                                               instance);
    }

    /**
     * Returns the folder holding the instance configurations.
     *
     * Instance discovery ({@link #getInstances}) and instance loading ({@link #getInstanceConfig}) MUST resolve
     * against this same path.
     *
     * @return the requisitions folder
     */
    private Path getRequisitionsPath() {
        return this.base.resolve(REQUISITIONS_FOLDER);
    }
    
    public InstanceConfiguration getInstanceConfigWithGlobals(final String instance) {
        return this.mergeGlobals(this.getInstanceConfig(instance), instance);
    }

    /**
     * Builds an instance configuration from an in-memory candidate property
     * map - the same view {@link #getInstanceConfigWithGlobals} provides for a
     * saved configuration, without anything being written to disk.
     *
     * @param instance the name of the instance
     * @param properties the candidate properties
     *
     * @return the candidate configuration with global properties merged in
     */
    public InstanceConfiguration getCandidateConfigWithGlobals(final String instance,
                                                               final Map<String, String> properties) {
        final InstanceConfiguration instanceConfig = InstanceApacheConfiguration.fromMap(
                this.getRequisitionsPath().resolve(instance),
                instance,
                properties);

        return this.mergeGlobals(instanceConfig, instance);
    }

    private InstanceConfiguration mergeGlobals(final InstanceConfiguration instanceConfig,
                                               final String instance) {
        instanceConfig.addProperty("requisition", instance);

        globalConfig = new GlobalApacheConfiguration(this.base);
        Iterator<String> keys = globalConfig.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!instanceConfig.containsKey(key)) {
                instanceConfig.addProperty(key, globalConfig.getString(key));
            }
        }

        return instanceConfig;
    }
}
