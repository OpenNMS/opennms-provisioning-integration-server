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

import java.nio.file.Path;
import java.nio.file.Paths;
import org.opennms.pris.api.Configuration;

public abstract class AbstractApacheConfiguration implements Configuration {
    
    // The configuration
    private final org.apache.commons.configuration.Configuration config;

    protected AbstractApacheConfiguration(final org.apache.commons.configuration.Configuration config) {
        this.config = config;
    }

    protected org.apache.commons.configuration.Configuration getConfig() {
        return config;
    }
    
    @Override
    public boolean containsKey(final String key) {
        return this.config.containsKey(key);
    }

    @Override
    public String getString(final String key) {
        return this.config.getString(key);
    }

    @Override
    public String getString(final String key,
                            final String defaultValue) {
        return this.config.getString(key,
                                     defaultValue);
    }

    @Override
    public String[] getStringArray(final String key) {
        return this.config.getStringArray(key);
    }

    @Override
    public Path getPath(final String key) {
        return Paths.get(this.config.getString(key));
        
    }

    @Override
    public Path getPath(final String key,
                        final Path defaultValue) {
        return Paths.get(this.config.getString(key,
                                               defaultValue.toString()));
    }

    @Override
    public boolean getBoolean(final String key) {
        return this.config.getBoolean(key);
    }

    @Override
    public boolean getBoolean(final String key,
                              final boolean defaultValue) {
        return this.config.getBoolean(key,
                                      defaultValue);
    }
    
    @Override
    public int getInt(final String key) {
        return this.config.getInt(key);
    }

    @Override
    public int getInt(final String key,
                      final int defaultValue) {
        return this.config.getInt(key,
                                  defaultValue);
    }
}
