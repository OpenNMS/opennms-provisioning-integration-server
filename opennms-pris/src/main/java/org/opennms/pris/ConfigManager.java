package org.opennms.pris;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
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
 * overwritten by the {@literal config} system property.
 * 
 * The global configuration is loaded from the {@literal config.properties} in the
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
      addConfiguration(new PropertiesConfiguration(ConfigManager.this.base.resolve("global.properties").toFile()));
      
      setThrowExceptionOnMissing(true);
    }};
  }

  /**
   * Returns the global configuration.
   * 
   * @return global configuration
   */
  public Configuration getGlobalConfig() {
    return this.globalConfig;
  }
  
    /**
   * Returns all known instance names.
   * 
   * The all sub-folders of the base directory whereas a
   * {@literal  requisition.properties} file exists are interpreted as instance
   * configurations.
   * 
   * @return a collection of instance names
   * 
   * @throws ConfigurationException
   */
  public Collection<String> getInstances() throws ConfigurationException {
    return this.getInstances("*");
  }
  
  /**
   * Returns all known instance names matching the provided glob.
   * 
   * The all sub-folders of the base directory whereas a
   * {@literal  requisition.properties} file exists are interpreted as instance
   * configurations.
   * 
   * The provided glob pattern is used to limit the returned instances to those
   * matching the glob. To return all instances, {@code "*"} can be passed.
   * 
   * @param glob the glob pattern
   * 
   * @return a collection of instance names
   * 
   * @throws ConfigurationException
   */
  public Collection<String> getInstances(final String glob) throws ConfigurationException {
    try (final DirectoryStream<Path> stream = Files.newDirectoryStream(this.base,
                                                                       glob)) {
      
      // The list of found instances
      Collection<String> instances = new ArrayList<>();
      
      // Loop over the stream of child files to find all instances
      for (final Path path : stream) {
        // An instance must be a directory and must contain the properties file
        if (!Files.isDirectory(path) ||
            !Files.exists(path.resolve("requisition.properties"))) {
          continue;
        }
        
        // Get the name of the folder relative to the base folder and add it to
        // the list of known instances
        instances.add(this.base.relativize(path).toString());
      }
      
      return instances;
      
    } catch (final IOException ex) {
      throw new ConfigurationException("Unable to traverse config folder", ex);
    }
  }

  /**
   * Return the instance configuration.
   * 
   * The instance configuration is loaded from the folder specified by the
   * parameter {@literal instance}.
   * 
   * @param instance the instance name
   * 
   * @return the instance configuration
   * 
   * @throws ConfigurationException 
   */
  public Configuration getInstanceConfig(final String instance) throws ConfigurationException {
    final Path path = this.getInstancePath(instance).resolve("requisition.properties");
    
    // Raise wrapped file not found exception if the config file does not exist
    if (!Files.exists(path)) {
      throw new ConfigurationException("Config file not found: " + path);
    }
    
    return new CompositeConfiguration() {{
      addConfiguration(new PropertiesConfiguration(path.toFile()) {{
        setThrowExceptionOnMissing(true);
        setReloadingStrategy(new FileChangedReloadingStrategy());
      }});
      
      addConfiguration(new MapConfiguration(Collections.singletonMap("requisition",
                                                                     (Object) instance)));
      
      addConfiguration(ConfigManager.this.globalConfig);
    }};
  }
  
  /**
   * The base path for the instance.
   * 
   * @param instance the instance name
   * 
   * @return the instance base path
   */
  public Path getInstancePath(final String instance) {
    return this.base.resolve(instance);
  }
}
