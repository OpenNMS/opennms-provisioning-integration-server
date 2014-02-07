package org.opennms.pris.mapper;

import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;

/**
 * A mapper to map information to a requisition.
 * 
 * The data loaded by a source implementation are passed the the map function
 * and a new requisition must be returned.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public interface Mapper {
  
  public static interface Factory {
    public abstract Mapper create(final String instance,
                                  final Configuration config);
  }
  
  /**
   * Maps information to a requisition.
   * 
   * The passed data is the data returned by the source implementation.
   * 
   * @param data the information used to create the requisition.
   * 
   * @param requisition Requisition already filled by a mapper, or an empty requistion.
   * 
   * @return the mapped requisition
   * 
   * @throws Exception 
   */
  public abstract Requisition map(final Object data, Requisition requisition) throws Exception;
}
