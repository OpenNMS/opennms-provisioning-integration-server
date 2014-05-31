/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/
package org.opennms.opennms.pris.plugins.vmware.mapper;

import org.kohsuke.MetaInfServices;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVmwareMapper implements Mapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVmwareMapper.class);
    
    private final InstanceConfiguration config;

    public DefaultVmwareMapper(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Requisition map(final Object data,
                           final Requisition requisition) throws Exception {
        // TODO: DO THE MAGIC
        return requisition;
    }

    @MetaInfServices
    public static class Factory implements Mapper.Factory {

        @Override
        public String getMapperIdentifier() {
            return "vmware";
        }

        @Override
        public Mapper create(final InstanceConfiguration config) {
            return new DefaultVmwareMapper(config);
        }
    }
}
