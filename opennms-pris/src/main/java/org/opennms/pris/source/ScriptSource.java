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

package org.opennms.pris.source;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FilenameUtils;
import org.opennms.pris.IpInterfaceHelper;
import org.opennms.pris.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>ScriptSource class.</p>
 *
 * @author <a href="mailto:ronny@opennms.org">Ronny Trommer</a>
 * @version $Id: $
 * @since 1.0-SNAPSHOT
 */
public class ScriptSource implements Source {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequisitionSource.class);

    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();

    /**
     * the name of the resulting requisition
     */
    private final String instance;

    private final Configuration config;

    public ScriptSource(final String instance,
                        final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        String scriptName = this.config.getString("source.file");

        // Get the path to the script
        final Path script = Starter.getConfigManager().getInstancePath(this.instance).resolve(scriptName);

        // Get the script engine by language defined in config or by extension if it
        // is not defined in the config
        final ScriptEngine scriptEngine = this.config.containsKey("lang")
                ? SCRIPT_ENGINE_MANAGER.getEngineByName(this.config.getString("lang"))
                : SCRIPT_ENGINE_MANAGER.getEngineByExtension(FilenameUtils.getExtension(script.toString()));

        // Create some bindings for values available in the script
        final Bindings scriptBindings = scriptEngine.createBindings();
        scriptBindings.put("script", script);
        scriptBindings.put("logger", LoggerFactory.getLogger(script.toString()));
        scriptBindings.put("config", this.config);
        scriptBindings.put("instance", instance);
        scriptBindings.put("ipInterfaceHelper", new IpInterfaceHelper());

        // Evaluate the script and return the requisition created in the script
        try (final Reader scriptReader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
            LOGGER.debug("Start Script {}", scriptName);
            final Object data = scriptEngine.eval(scriptReader, scriptBindings);
            LOGGER.debug("Done  Script {}", scriptName);
            return data;
        }
    }

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance,
                             final Configuration config) {
            return new ScriptSource(instance, config);
        }
    }
}
