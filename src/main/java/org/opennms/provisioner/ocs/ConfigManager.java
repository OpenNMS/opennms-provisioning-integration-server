package org.opennms.provisioner.ocs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

/**
 * The configuration manager.
 * 
 * The manager provides a global configuration and a configuration for each
 * instance.
 * 
 * The configuration base path is the current working directory and can be
 * overwritten by the {@code config} system property.
 * 
 * The global configuration is loaded from the {@code config.properties} in the
 * base path. These properties can be overwritten by the system properties.
 * 
 * The instance configurations are loaded from sub-folders relative to the
 * configuration base path where the instance name in the folder name.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class ConfigManager {

  // The base path of the config
  private final Path base;
  
  // The system properties
  private final Configuration systemConfig;
  
  // The global configuration
  private final Configuration globalConfig;

  public ConfigManager() throws ConfigurationException {
    // Load system properties
    this.systemConfig = new SystemConfiguration();

    // Get the config base folder and fall back to the CWD
    final String cwd = this.systemConfig.getString("user.dir");
    this.base = Paths.get(this.systemConfig.getString("config", cwd));

    // Build composition of system properties and config file
    this.globalConfig = new CompositeConfiguration() {{
      addConfiguration(new SystemConfiguration());
      addConfiguration(new PropertiesConfiguration(ConfigManager.this.base.resolve("config.properties").toFile()));
      
      setThrowExceptionOnMissing(true);
    }};
  }

  /**
   * Returns the global configuration.
   * 
   * @return global configuration
   */
  public Configuration getGlobal() {
    return this.globalConfig;
  }

  /**
   * Return the instance configuration.
   * 
   * The instance configuration is loaded from the folder specified by the
   * parameter {@code instance}.
   * 
   * @param instance the instance name
   * 
   * @return the instance configuration
   * 
   * @throws ConfigurationException 
   */
  public Configuration getInstances(final String instance) throws ConfigurationException {
    final File file = this.getPath(instance).resolve("config.properties").toFile();
    
    // Raise wrapped file not found exception if the config file does not exist
    if (!file.exists()) {
      throw new ConfigurationException("Config file not found: " + file);
    }
    
    return new PropertiesConfiguration(file) {{
      setThrowExceptionOnMissing(true);
      setReloadingStrategy(new FileChangedReloadingStrategy());
    }};
  }
  
  /**
   * The path to the instance configuration.
   * 
   * @param instance the instance name
   * 
   * @return the instance base path
   */
  public Path getPath(final String instance) {
    return this.base.resolve(instance);
  }
}
