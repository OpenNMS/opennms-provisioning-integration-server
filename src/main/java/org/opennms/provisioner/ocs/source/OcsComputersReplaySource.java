package org.opennms.provisioner.ocs.source;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.ocs.inventory.client.request.logic.GetComputersLogic;
import org.opennms.ocs.inventory.client.response.Computers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsComputersReplaySource implements Source{

  private static final Logger LOGGER = LoggerFactory.getLogger(OcsComputersReplaySource.class);

  public static class Factory implements Source.Factory {

    @Override
    public Source create(final String instance,
                         final Configuration config) {
      return new OcsComputersReplaySource(instance, config);
    }
  }
  
  private final String instance;
  private final Configuration config;

  public OcsComputersReplaySource(final String instance,
                                  final Configuration config) {
    this.instance = instance;
    this.config = config;
  }

  @Override
  public Object dump() throws Exception {
    final File replaySource = new File(this.config.getString("file"));
    
    final JAXBContext jaxbContext = JAXBContext.newInstance(Computers.class);
    final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    return jaxbUnmarshaller.unmarshal(replaySource);
  }
}
