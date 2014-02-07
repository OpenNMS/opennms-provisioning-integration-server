package org.opennms.pris.source;

import org.apache.commons.configuration.Configuration;

/**
 * A source for information used to create a requisition.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public interface Source {
  
  public static interface Factory {
    public abstract Source create(final String instance,
                                  final Configuration config);
  }

  /**
   * Loads some information and returns them.
   * 
   * There is no requirement to the returned object as long as there is a mapper
   * available for mapping this data.
   * 
   * @return the loaded information
   * 
   * @throws Exception 
   */
  public abstract Object dump() throws Exception;
}
