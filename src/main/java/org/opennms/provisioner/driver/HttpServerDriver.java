package org.opennms.provisioner.driver;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.provisioner.RequisitionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A working mode providing a HTTP server publishing generated requisitions.
 * 
 * The server exports the configured instances as URLs. The requested path is
 * used as instance name and the the returned result is the XML serialized
 * requisition.
 * 
 * @author Dustin Frisch <fooker@lab.sh>
 */
public class HttpServerDriver implements Driver {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerDriver.class);

  public static final class Factory implements Driver.Factory {

    @Override
    public Driver create(final Configuration config) {
      return new HttpServerDriver(config);
    }
  }
  
  // The global configuration
  private final Configuration config;

  private HttpServerDriver(final Configuration config) {
    this.config = config;
  }

  @Override
  public void run() throws Exception {
    // Create an embedded jetty instance
    final Server server = new Server(new InetSocketAddress(this.config.getString("host", "127.0.0.1"),
                                                           this.config.getInt("port", 8686)));

    // Create the marshaller for the requision
    final JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);

    final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    // Create a handler for the HTTP requests returning a requisition
    server.setHandler(new AbstractHandler() {

      @Override
      public void handle(final String target,
                         final Request baseRequest,
                         final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException,
                                                                    ServletException {
        // As this is the only handler, we mark every request as handled
        baseRequest.setHandled(true);
        
        // Get the instance fro the request path
        final String instance = request.getPathInfo().substring(1);

        LOGGER.debug("Handling request for instance: {}", instance);
        
        // Check if the instance name was passed and valid
        if (instance == null
            || instance.isEmpty()) {
          response.sendError(404, "No instance specified");
          return;
        }

        try {
          // Get the requisition provider for the instance
          final RequisitionProvider requisitionProvider = new RequisitionProvider(instance);

          // Generate the requisition
          final Requisition requisition = requisitionProvider.generate(instance);

          // Marshall the requisition and write it to the response stream
          jaxbMarshaller.marshal(requisition, response.getOutputStream());

        } catch (final Exception ex) {
          response.sendError(500, ex.getMessage());
          
          LOGGER.warn(null, ex);
        }
      }
    });
    
    // Start the server
    server.start();
    server.join();
  }
}
