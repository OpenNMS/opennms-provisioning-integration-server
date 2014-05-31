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

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.kohsuke.MetaInfServices;
import org.opennms.pris.api.Configuration;
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

    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();

    private final InstanceConfiguration config;

    public ScriptSource(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        String scriptName = this.config.getString("file");

        // Get the path to the script
        final Path script = this.getFile();

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
        scriptBindings.put("instance", this.config.getInstanceIdentifier());
        // TODO (fooker): Rewrite the Helper to be not OCS-dependent and re-add
//        scriptBindings.put("ipInterfaceHelper", new IpInterfaceHelper());

        // Evaluate the script and return the requisition created in the script
        try (final Reader scriptReader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
            LOGGER.debug("Start Script {}", scriptName);
            final Object data = scriptEngine.eval(scriptReader, scriptBindings);
            LOGGER.debug("Done  Script {}", scriptName);
            return data;
        }
    }
    
    public Path getFile() {
        return this.config.getPath("file");
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
