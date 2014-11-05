/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.
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
package org.opennms.opennms.pris.plugins.script.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.io.FilenameUtils;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.util.InterfaceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptManager.class);

    public static Object execute(final InstanceConfiguration config,
                                 final Map<String, Object> bindings) throws IOException, ScriptException {
        
        Requisition requisition = null;
        // Get the path to the script
        final List<Path> scripts = config.getPaths("file");
        
        // Get the script engine by language defined in config or by extension if it
        // is not defined in the config
        final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager(ScriptManager.class.getClassLoader());

        for (Path script : scripts) {
            
            final ScriptEngine scriptEngine = config.containsKey("lang")
                                          ? SCRIPT_ENGINE_MANAGER.getEngineByName(config.getString("lang"))
                                          : SCRIPT_ENGINE_MANAGER.getEngineByExtension(FilenameUtils.getExtension(script.toString()));

        if (scriptEngine == null) {
            throw new RuntimeException("Script engine implementation not found");
        }
        
        // Create some bindings for values available in the script
        final Bindings scriptBindings = scriptEngine.createBindings();
        scriptBindings.put("script", script);
        scriptBindings.put("logger", LoggerFactory.getLogger(script.toString()));
        scriptBindings.put("config", config);
        scriptBindings.put("instance", config.getInstanceIdentifier());
        scriptBindings.put("interfaceUtils", new InterfaceUtils(config));
        scriptBindings.putAll(bindings);
        
        // Overwrite initial requisition with the requisition from the previous script, if there was any.
        if (requisition != null) {
            scriptBindings.put("requisition", requisition);
        }

        // Evaluate the script and return the requisition created in the script
        try (final Reader scriptReader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
            LOGGER.debug("Start Script {}", script);
            requisition = (Requisition) scriptEngine.eval(scriptReader, scriptBindings);
            LOGGER.debug("Done  Script {}", script);
        }
    }
        return requisition;
    }

}
