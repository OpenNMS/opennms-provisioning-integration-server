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
package org.opennms.pris.api;

import java.nio.file.Path;
import java.util.List;

/**
 * Container for a configuration.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public interface Configuration {
    
    /**
     * Returns the path to the configuration.
     * 
     * @return the absolute path to the configuration folder.
     */
    Path getBasePath();

    /**
     * Check if the configuration is empty.
     *
     * @return {@code true} if the configuration contains no property,
     *         {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Check if the configuration contains the specified key.
     *
     * @param key the key whose presence in this configuration is to be tested
     *
     * @return {@literal true} if the configuration contains a value for this
     *         key, {@literal false} otherwise
     */
    boolean containsKey(final String key);

    /**
     * Get a boolean associated with the given configuration key.
     *
     * @param key The configuration key
     *
     * @return The associated boolean
     */
    boolean getBoolean(final String key);

    /**
     * Get a boolean associated with the given configuration key.
     *
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key The configuration key
     * @param defaultValue The default value
     *
     * @return The associated boolean
     */
    boolean getBoolean(final String key,
                       final boolean defaultValue);

    /**
     * Get a int associated with the given configuration key.
     *
     * @param key The configuration key
     * 
     * @return The associated int
     */
    int getInt(final String key);

    /**
     * Get a int associated with the given configuration key.
     * 
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key The configuration key
     * @param defaultValue The default value
     * 
     * @return The associated int
     */
    int getInt(final String key,
               final int defaultValue);

   /**
     * Get a path associated with the given configuration key.
     *
     * The returned path is resolved to the base directory of the instance
     * configuration.
     *
     * @param key The configuration key
     *
     * @return The associated path
     */
    Path getPath(final String key);

   /**
     * Get all pathes associated with the given configuration key.
     * 
     * The returned pathes are resolved to the base directory of the instance
     * configuration.
     *
     * @param key The configuration key
     *
     * @return A List of the associated pathes
     */
    List<Path> getPathes(final String key);

        
    /**
     * Get a path associated with the given configuration key.
     *
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * The returned path is resolved to the base directory if the instance
     * configuration.
     *
     * @param key The configuration key
     * @param defaultValue The default value
     *
     * @return The associated path if key is found and has valid
     *         format, default value otherwise
     */
    Path getPath(final String key,
                 final Path defaultValue);

    /**
     * Get a string associated with the given configuration key.
     *
     * @param key The configuration key
     *
     * @return The associated string
     */
    String getString(final String key);

    /**
     * Get a string associated with the given configuration key.
     *
     * If the key doesn't map to an existing object, the default value
     * is returned.
     *
     * @param key The configuration key
     * @param defaultValue The default value
     *
     * @return The associated string if key is found and has valid
     *         format, default value otherwise
     */
    String getString(final String key,
                     final String defaultValue);

    /**
     * Get an array of strings associated with the given configuration key.
     *
     * If the key doesn't map to an existing object an empty array is returned.
     *
     * @param key The configuration key
     *
     * @return The associated string array if key is found
     */
    String[] getStringArray(final String key);
    
}
