package org.opennms.provisioner.ocs;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.opennms.provisioner.ocs.driver.Driver;
import org.opennms.provisioner.ocs.driver.HttpServerDriver;
import org.opennms.provisioner.ocs.driver.OneshotDriver;
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

  // All known working modes
  private static final Map<String, Driver.Factory> WORKING_MODES = ImmutableMap.<String, Driver.Factory>builder()
          .put("http", new HttpServerDriver.Factory())
          .put("oneshot", new OneshotDriver.Factory())
          .build();

  // The global config manger instance
  private static ConfigManager configManager;

  public static void main(final String[] args) throws Exception {
    // Create a config manager
    Starter.configManager = new ConfigManager();

    // Load the global configuration
    final Configuration config = configManager.getGlobal();

    // Get the driver for the selected working mode
    final String modeName = config.getString("mode");
    final Driver.Factory driverFactory = WORKING_MODES.get(modeName);
    if (driverFactory != null) {
      // Create the driver for the working mode
      final Driver driver = driverFactory.create(config);

      // Execute the working mode implementation
      driver.run();

    } else {
      throw new IllegalArgumentException("Invalid working mode specified: " + modeName);
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
