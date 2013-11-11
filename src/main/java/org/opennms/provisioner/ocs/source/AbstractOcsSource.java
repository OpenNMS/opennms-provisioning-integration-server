package org.opennms.provisioner.ocs.source;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.configuration.Configuration;

public abstract class AbstractOcsSource implements Source {
  
  private final String instance;
  private final Configuration config;

  public AbstractOcsSource(final String instance,
                           final Configuration config) {
    this.instance = instance;
    this.config = config;
  }

  public final Configuration getConfig() {
    return this.config;
  }
          
  public final String  getUrl() {
    return this.config.getString("ocs.url");
  };
  
  public final String  getUsername() {
    return this.config.getString("ocs.username");
  };
  
  public final String  getPassword() {
    return this.config.getString("ocs.password");
  };
  
  public final String  getChecksum() {
    return this.config.getString("ocs.checksum");
  };
  
  public final List<String>  getTags() {
    return Arrays.asList(this.config.getStringArray("ocs.tags"));
  };
}
