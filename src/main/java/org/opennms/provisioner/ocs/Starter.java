package org.opennms.provisioner.ocs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Starter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);
    private static String requisitionFile;
    private static String mode;
    private static String ocsUrl;
    private static String ocsUsername;
    private static String ocsPassword;
    private static String foreignSource;
    private static Integer port;
    private static OcsRequisitionProvider ocsRequisitionProvider;
    private static Requisition requisition;
    private static String mapper;
    private static String checksum;
    private static List<String> tags;

    public static void main(String[] args) throws JAXBException, IOException {

        loadProperties();

        ocsRequisitionProvider = new OcsRequisitionProvider(ocsUrl, ocsUsername, ocsPassword, foreignSource, mapper, checksum, tags);

        switch (mode) {
            case "writeToFileMode":
                requisition = ocsRequisitionProvider.generateRequisition();
                JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.marshal(requisition, new File(requisitionFile));
                break;
                
            case "httpServerMode":
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/ocs", new MyHandler());
                server.setExecutor(null);
                server.start();
                LOGGER.debug("get Requisition from: http://localhost:" + port + "/ocs/");
                break;
                
            case "restPushMode":
                LOGGER.debug("This mode is not supported yet");
                break;
                
            default:
                LOGGER.error("No propper mode defined: " + mode);
                break;
        }
    }

    private static void loadProperties() {
        Properties prop = new Properties();

        try {
            LOGGER.debug("Reading properties from: " + new File("config.properties").getAbsolutePath());
            prop.load(new FileInputStream("config.properties"));

            mode = prop.getProperty("mode");
            LOGGER.debug("Run in " + mode);

            mapper = prop.getProperty("mapper", "default");

            ocsUrl = prop.getProperty("ocsUrl");
            ocsUsername = prop.getProperty("ocsUsername");
            ocsPassword = prop.getProperty("ocsPassword");
            foreignSource = prop.getProperty("foreignSource");

            requisitionFile = prop.getProperty("requisitionFile");

            port = Integer.parseInt(prop.getProperty("port"));
            
            checksum = prop.getProperty("checksum");
            
            final String tagCsv = prop.getProperty("tags");
            tags = new ArrayList<String>();
            if (tagCsv != null && tagCsv.trim().length() > 0) {
                for (String aTag : tagCsv.split("\\s*,\\s*")) {
                    tags.add(aTag.trim());
                }
            }

        } catch (IOException ex) {
            LOGGER.error("loading config failed", ex);
            System.exit(1);
        }
    }

    static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            loadProperties();
            ocsRequisitionProvider = new OcsRequisitionProvider(ocsUrl, ocsUsername, ocsPassword, foreignSource, mapper, checksum, tags);

            requisition = ocsRequisitionProvider.generateRequisition();
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                StringWriter stringWriter = new StringWriter();
                jaxbMarshaller.marshal(requisition, stringWriter);
                String response = stringWriter.toString();
                t.sendResponseHeaders(200, response.length());
                try (OutputStream os = t.getResponseBody()) {
                    os.write(response.getBytes());
                }
                LOGGER.debug("delivered requisition: " + foreignSource + " with " + requisition.getNodes().size() + " nodes");
            } catch (JAXBException ex) {
                t.sendResponseHeaders(500, ex.getMessage().length());
                try (OutputStream os = t.getResponseBody()) {
                    os.write(ex.getMessage().getBytes());
                }
            }
        }
    }
}
