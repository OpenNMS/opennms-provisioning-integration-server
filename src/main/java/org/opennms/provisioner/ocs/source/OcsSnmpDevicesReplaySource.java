package org.opennms.provisioner.ocs.source;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.configuration.Configuration;
import org.opennms.ocs.inventory.client.request.logic.GetSnmpDevicesLogic;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsSnmpDevicesReplaySource implements Source {

  private static final Logger LOGGER = LoggerFactory.getLogger(OcsSnmpDevicesReplaySource.class);
  
  public static class Factory implements Source.Factory {

    @Override
    public Source create(final String instance,
                         final Configuration config) {
      return new OcsSnmpDevicesReplaySource(instance, config);
    }
  }
  
  private final String instance;
  private final Configuration config;

  public OcsSnmpDevicesReplaySource(final String instance,
                                    final Configuration config) {
    this.instance = instance;
    this.config = config;
  }

  @Override
  public Object dump() throws Exception {
    final File replaySource = new File(this.config.getString("file"));
    
    final JAXBContext jaxbContext = JAXBContext.newInstance(SnmpDevices.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    return jaxbUnmarshaller.unmarshal(replaySource);
  }
}
