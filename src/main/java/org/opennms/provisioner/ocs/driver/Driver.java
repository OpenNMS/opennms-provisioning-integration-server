package org.opennms.provisioner.ocs.driver;

import org.apache.commons.configuration.Configuration;

/**
 * A driver to handle a working mode.
 * 
 * The driver runs a working mode.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public interface Driver {

  public static interface Factory {

    public abstract Driver create(final Configuration config);
  }
  
  /**
   * Runs the working mode implementation.
   * 
   * @throws Exception 
   */
  public abstract void run() throws Exception;
}
