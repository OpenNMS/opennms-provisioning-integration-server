/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2026 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2026 The OpenNMS Group, Inc.
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

package org.opennms.pris.api;

/**
 * Describes one configuration parameter of a source or mapper.
 *
 * The descriptors let tooling - like a configuration UI - render proper
 * input fields per implementation and validate a configuration before it is
 * written. The parameter name is relative to the implementation's config
 * prefix: a source parameter {@code file} is stored as {@code source.file}
 * in the requisition configuration.
 */
public final class ParameterDescriptor {

    private final String name;
    private final String description;
    private final boolean required;
    private final boolean secret;

    private ParameterDescriptor(final String name,
                                final String description,
                                final boolean required,
                                final boolean secret) {
        this.name = name;
        this.description = description;
        this.required = required;
        this.secret = secret;
    }

    public static ParameterDescriptor required(final String name, final String description) {
        return new ParameterDescriptor(name, description, true, false);
    }

    public static ParameterDescriptor optional(final String name, final String description) {
        return new ParameterDescriptor(name, description, false, false);
    }

    public static ParameterDescriptor secret(final String name, final String description) {
        return new ParameterDescriptor(name, description, false, true);
    }

    public static ParameterDescriptor requiredSecret(final String name, final String description) {
        return new ParameterDescriptor(name, description, true, true);
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isRequired() {
        return this.required;
    }

    public boolean isSecret() {
        return this.secret;
    }
}
