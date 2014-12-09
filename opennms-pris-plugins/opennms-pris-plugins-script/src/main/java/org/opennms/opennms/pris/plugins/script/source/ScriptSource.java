/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
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
package org.opennms.opennms.pris.plugins.script.source;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kohsuke.MetaInfServices;
import org.opennms.opennms.pris.plugins.script.util.ScriptManager;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;

/**
 * <p>ScriptSource class.</p>
 *
 * @author <a href="mailto:ronny@opennms.org">Ronny Trommer</a>
 * @version $Id: $
 * @since 1.0-SNAPSHOT
 */
public class ScriptSource implements Source {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptSource.class);

    private final InstanceConfiguration config;

    public ScriptSource(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        return ScriptManager.executeToObject(this.config,
                                     ImmutableMap.<String, Object>builder().build());
    }
    
    @MetaInfServices
    public static class Factory implements Source.Factory {

        @Override
        public String getIdentifier() {
            return "script";
        }
        
        @Override
        public Source create(final InstanceConfiguration config) {
            return new ScriptSource(config);
        }
    }
}
