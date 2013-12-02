package org.opennms.provisioner.ocs.driver;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.provisioner.ocs.RequisitionProvider;
import org.opennms.provisioner.ocs.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A working driver used to create a single XML serialized requisition.
 *
 * The instance name is passed as an parameter. The created requisition is
 * printed to standard output or to a given file name.
 *
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class FileDriver implements Driver {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileDriver.class);

  public static final class Factory implements Driver.Factory {

    @Override
    public Driver create(final Configuration config) {
      return new FileDriver(config);
    }
  }

  // The global configuration
  private final Configuration config;

  private FileDriver(final Configuration config) {
    this.config = config;
  }

  @Override
  public void run() throws Exception {
    // Get the instance matching glob and find all matching instances
    final String instanceGlob = this.config.getString("requisitions", "*");
    final Collection<String> instances = Starter.getConfigManager().getInstances(instanceGlob);
    
    // Get the target directory and ensure it exitst
    final Path targetBase = Paths.get(this.config.getString("target"));
    Files.createDirectories(targetBase);
    
    // Loop over all instances
    for (final String instance : instances) {
      // Get the target path for this instance
      final Path target = targetBase.resolve(instance + ".xml");
      
      // Generate the requisition
      final RequisitionProvider requisitionProvider = new RequisitionProvider(instance);
      final Requisition requisition = requisitionProvider.generate();
      
      // Create a XML serializer
      final JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
      
      final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      
      // Serialize the generated requisition to a the configured target
      try (final OutputStream os = Files.newOutputStream(target)) {
        jaxbMarshaller.marshal(requisition, os);
      }
    }
  }
}
