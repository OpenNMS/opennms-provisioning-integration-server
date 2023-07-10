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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.opennms.pris.api.Configuration;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.config.GlobalApacheConfiguration;
import org.opennms.pris.config.InstanceApacheConfiguration;

/**
 * The configuration manager.
 *
 * The manager provides a global configuration and a configuration for each instance that represents the requisition name.
 *
 * The configuration base path is the current working directory and can be overwritten by the {@literal config} system property.
 *
 * The global configuration is loaded from the {@literal config.properties} in the base path. These properties can be overwritten by the system properties.
 *
 * The instance configurations are loaded from sub-folders relative to the configuration base path where the instance name in the folder name.
 *
 * @author Dustin Frisch &lt;fooker@lab.sh&gt;
 */
public class ConfigManager {

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
     * All sub-folders of the base directory having a {@literal requisition.properties} file are interpreted as instance configurations.
     *
     * @return a collection of instance names
     */
    public Collection<String> getInstances() {
        return this.getInstances("*");
    }

    /**
     * Returns all known instance names matching the provided glob.
     *
     * All sub-folders of the base directory matching the passed glob and having a {@literal  requisition.properties} file are interpreted as instance configurations.
     *
     * The provided glob pattern is used to limit the returned instances to those matching the glob. To return all instances, {@code "*"} can be passed.
     *
     * @param glob the glob pattern
     *
     * @return a collection of instance names
     */
    public Collection<String> getInstances(final String glob) {
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(this.base, glob)) {

            // The list of found instances
            final Collection<String> instances = new ArrayList<>();

            // Loop over the stream of child files to find all instances
            for (final Path path : stream) {
                // An instance must be a directory and must contain the properties file
                if (!Files.isDirectory(path) ||
                    !Files.exists(path.resolve("requisition.properties"))) {
                    continue;
                }

                // Get the name of the folder relative to the base folder and add it to
                // the list of known instances
                instances.add(this.base.relativize(path).toString());
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
        return new InstanceApacheConfiguration(this.base.resolve("requisitions" + File.separator + instance),
                                               instance);
    }
    
    public InstanceConfiguration getInstanceConfigWithGlobals(final String instance) {
        InstanceConfiguration instanceConfig = this.getInstanceConfig(instance);
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
