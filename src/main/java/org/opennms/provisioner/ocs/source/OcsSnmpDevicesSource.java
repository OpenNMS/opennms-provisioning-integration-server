package org.opennms.provisioner.ocs.source;

import org.apache.commons.configuration.Configuration;
import org.opennms.ocs.inventory.client.request.logic.GetSnmpDevicesLogic;
import org.opennms.provisioner.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsSnmpDevicesSource extends AbstractOcsSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(OcsSnmpDevicesSource.class);
  
  public static class Factory implements Source.Factory {

    @Override
    public Source create(final String instance,
                         final Configuration config) {
      return new OcsSnmpDevicesSource(instance, config);
    }
  }
  
  public OcsSnmpDevicesSource(final String instance,
                              final Configuration config) {
    super(instance,
          config);
  }

  @Override
  public Object dump() throws Exception {
    final GetSnmpDevicesLogic ocsClient = new GetSnmpDevicesLogic();

    ocsClient.init(this.getUrl(),
                   this.getUsername(),
                   this.getPassword(),
                   this.getChecksum(),
                   this.getTags());
    
    return ocsClient.getSnmpDevices();
  }
  
}
