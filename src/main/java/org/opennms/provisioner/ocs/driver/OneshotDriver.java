package org.opennms.provisioner.ocs.driver;

import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.provisioner.ocs.RequisitionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A working mode used to create a single XML serialized requisition.
 *
 * The instance name is passed as an parameter. The created requisition is
 * printed to standard output or to a given file name.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class OneshotDriver implements Driver {

  private static final Logger LOGGER = LoggerFactory.getLogger(OneshotDriver.class);

  public static final class Factory implements Driver.Factory {

    @Override
    public Driver create(final Configuration config) {
      return new OneshotDriver(config);
    }
  }

  // The global configuration
  private final Configuration config;

  private OneshotDriver(final Configuration config) {
    this.config = config;
  }

  @Override
  public void run() throws Exception {
    // Get the instance name to load
    final String instance = this.config.getString("instance");

    // Generate the requisition
    final RequisitionProvider requisitionProvider = new RequisitionProvider(instance);
    final Requisition requisition = requisitionProvider.generate();

    // Create a XML serializer
    final JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);

    final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    // Serialize the generated requisition to the configured target or the
    // standard output
    try (final OutputStream target = this.config.containsKey("target")
                                     ? new FileOutputStream(this.config.getString("target"))
                                     : System.out) {
      jaxbMarshaller.marshal(requisition, target);
    }
  }
}
