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
 * http://www.opennms.org/ http://www.opennms.com/
 ******************************************************************************
 */
package org.opennms.opennms.pris.plugins.script.mapper;

import com.google.common.collect.ImmutableMap;
import org.opennms.pris.model.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kohsuke.MetaInfServices;
import org.opennms.opennms.pris.plugins.script.util.ScriptManager;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Mapper;

/**
 * A mapper passing the data to a script.
 *
 * The mapper implementation creates a requisition by passing the data from the source to a script. The Script has to create the requisition.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class ScriptMapper implements Mapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptMapper.class);

    // The instance configuration
    private final InstanceConfiguration config;

    private ScriptMapper(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Requisition map(final Object data,
                           final Requisition requisition) throws Exception {
        return (Requisition) ScriptManager.executeToRequisition(this.config,
                                                   ImmutableMap.<String, Object>builder()
                                                           .put("data", data)
                                                           .put("requisition", requisition)
                                                           .build());
    }

    @MetaInfServices
    public static class Factory implements Mapper.Factory {

        @Override
        public String getIdentifier() {
            return "script";
        }

        @Override
        public Mapper create(InstanceConfiguration config) {
            return new ScriptMapper(config);
        }
    }
}
