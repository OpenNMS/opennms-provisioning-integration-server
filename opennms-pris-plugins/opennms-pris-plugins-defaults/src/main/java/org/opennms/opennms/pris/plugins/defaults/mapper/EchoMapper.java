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

package org.opennms.opennms.pris.plugins.defaults.mapper;

import org.kohsuke.MetaInfServices;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.api.Mapper;
import org.opennms.pris.api.InstanceConfiguration;

public class EchoMapper implements Mapper {

    private final InstanceConfiguration config;

    public EchoMapper(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Requisition map(Object data, Requisition requisition) throws Exception {
        return (Requisition) data;
    }
    
    @MetaInfServices
    public static class Factory implements Mapper.Factory {

        @Override
        public String getIdentifier() {
            return "echo";
        }

        @Override
        public Mapper create(final InstanceConfiguration config) {
            return new EchoMapper(config);
        }
    }
}
