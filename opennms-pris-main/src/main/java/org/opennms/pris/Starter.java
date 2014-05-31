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
 ******************************************************************************
 */
package org.opennms.pris;

import ch.qos.logback.classic.Level;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.opennms.pris.api.Configuration;
import org.opennms.pris.driver.Driver;
import org.opennms.pris.driver.FileDriver;
import org.opennms.pris.driver.HttpServerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 *
 * The main class loads a driver for the configured working and runs it.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class Starter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);

    // All known working drivers
    private static final Map<String, Driver.Factory> WORKING_DRIVERS = ImmutableMap.<String, Driver.Factory>builder()
            .put("http", new HttpServerDriver.Factory())
            .put("file", new FileDriver.Factory())
            .build();

    // The global config manger instance
    private static ConfigManager configManager;

    public static void main(final String[] args) throws Exception {
                
        // Create a config manager
        Starter.configManager = new ConfigManager();

        // Load the global configuration
        final Configuration config = configManager.getGlobalConfig();

        setLoggingLevel(Level.toLevel(config.getString("loglevel", "INFO")));

        // Get the driver for the selected working driver
        final String driverName = config.getString("driver");
        final Driver.Factory driverFactory = WORKING_DRIVERS.get(driverName);
        if (driverFactory != null) {
            // Create the driver for the working driver
            final Driver driver = driverFactory.create(config);

            // Execute the working driver implementation
            driver.run();

        } else {
            throw new IllegalArgumentException("Invalid working driver specified: " + driverName);
        }
    }

    /**
     * Returns the global config manager instance.
     *
     * @return a config manager
     */
    public static ConfigManager getConfigManager() {
        return Starter.configManager;
    }

    public static void setLoggingLevel(Level level) {
        // TODO: Get rid of direct logback reference (fooker)
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);
    }
}
