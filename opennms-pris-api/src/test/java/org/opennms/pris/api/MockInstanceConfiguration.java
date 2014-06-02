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
package org.opennms.pris.api;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.opennms.pris.api.InstanceConfiguration;

public class MockInstanceConfiguration implements InstanceConfiguration {

    private final String instance;
    
    private final Map<String, Object> values = new HashMap<>();

    public MockInstanceConfiguration(final String instance) {
        this.instance = instance;
    }
    
    @Override
    public String getInstanceIdentifier() {
        return this.instance;
    }

    @Override
    public boolean containsKey(final String key) {
        return this.values.containsKey(key);
    }
    
    private <T> T get(final String key) {
        return (T) this.values.get(key);
    }
    
    private <T> T get(final String key,
                      final T defaultValue) {
        if (!this.values.containsKey(key)) {
            return defaultValue;
        }
        
        return (T) this.values.get(key);
    }
    
    public void set(final String key,
                    final Object value) {
        this.values.put(key, value);
    }

    @Override
    public boolean getBoolean(final String key) {
        return this.get(key);
    }

    @Override
    public boolean getBoolean(final String key,
                              final boolean defaultValue) {
        return this.get(key,
                        defaultValue);
    }

    @Override
    public int getInt(final String key) {
        return this.get(key);
    }

    @Override
    public int getInt(final String key,
                      final int defaultValue) {
        return this.get(key,
                        defaultValue);
    }

    @Override
    public Path getPath(final String key) {
        return this.get(key);
    }

    @Override
    public Path getPath(final String key,
                        final Path defaultValue) {
        return this.get(key,
                        defaultValue);
    }

    @Override
    public String getString(final String key) {
        return this.get(key);
    }

    @Override
    public String getString(final String key,
                            final String defaultValue) {
        return this.get(key,
                        defaultValue);
    }

    @Override
    public String[] getStringArray(final String key) {
        return this.get(key);
    }

    @Override
    public InstanceConfiguration subset(String prefix) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
