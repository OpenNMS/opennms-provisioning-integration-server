package org.opennms.provisioner.ocs;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.opennms.provisioner.ocs.driver.Driver;
import org.opennms.provisioner.ocs.driver.HttpServerDriver;
import org.opennms.provisioner.ocs.driver.FileDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 * 
 * The main class loads a driver for the configured working and runs it.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class Starter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);

  // All known working drivers
  private static final Map<String, Driver.Factory> WORKING_DRIVERS = ImmutableMap.<String, Driver.Factory>builder()
          .put("http", new HttpServerDriver.Factory())
          .put("file", new FileDriver.Factory())
          .build();

  // The global config manger instance
  private static ConfigManager configManager;

  public static void main(final String[] args) throws Exception {
    // Create a config manager
    Starter.configManager = new ConfigManager();

    // Load the global configuration
    final Configuration config = configManager.getGlobalConfig();

    // Get the driver for the selected working driver
    final String driverName = config.getString("driver");
    final Driver.Factory driverFactory = WORKING_DRIVERS.get(driverName);
    if (driverFactory != null) {
      // Create the driver for the working driver
      final Driver driver = driverFactory.create(config);

      // Execute the working driver implementation
      driver.run();

    } else {
      throw new IllegalArgumentException("Invalid working driver specified: " + driverName);
    }
  }

  /**
   * Returns the global config manager instance.
   *
   * @return a config manager
   */
  public static ConfigManager getConfigManager() {
    return Starter.configManager;
  }
}
