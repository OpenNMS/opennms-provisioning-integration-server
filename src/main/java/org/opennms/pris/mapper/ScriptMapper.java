package org.opennms.pris.mapper;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FilenameUtils;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.pris.IpInterfaceHelper;
import org.opennms.pris.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mapper passing the data to a script.
 *
 * The mapper implementation creates a requisition by passing the data from the source to a script. The Script has to
 * create the requisition.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class ScriptMapper implements Mapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptMapper.class);

    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();

    public static class Factory implements Mapper.Factory {

        @Override
        public Mapper create(final String instance, final Configuration config) {
            return new ScriptMapper(instance, config);
        }
    }

    // The name of the instance
    private final String instance;

    // The instance configuration
    private final Configuration config;

    public ScriptMapper(final String instance,
            final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Requisition map(Object data, Requisition requisition) throws Exception {
        // Get the path to the script
        final Path script = Starter.getConfigManager().getInstancePath(this.instance).resolve(this.config.getString("script"));

        // Get the script engine by language defined in config or by extension if it
        // is not defined in the config
        final ScriptEngine scriptEngine = this.config.containsKey("lang")
                ? SCRIPT_ENGINE_MANAGER.getEngineByName(this.config.getString("lang"))
                : SCRIPT_ENGINE_MANAGER.getEngineByExtension(FilenameUtils.getExtension(script.toString()));

        // Create some bindings for values available in the script
        final Bindings scriptBindings = scriptEngine.createBindings();
        scriptBindings.put("script", script);
        scriptBindings.put("data", data);
        scriptBindings.put("logger", LoggerFactory.getLogger(script.toString()));
        scriptBindings.put("config", this.config);
        scriptBindings.put("instance", instance);
        scriptBindings.put("ipInterfaceHelper", new IpInterfaceHelper());
        scriptBindings.put("requisition", requisition);

        // Evaluate the script and return the requisition created in the script
        try (final Reader scriptReader = Files.newBufferedReader(script, StandardCharsets.UTF_8)) {
            return (Requisition) scriptEngine.eval(scriptReader, scriptBindings);
        }
    }
}
