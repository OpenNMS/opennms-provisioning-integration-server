/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C)
 * 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/
 * *****************************************************************************
 */
package org.opennms.pris.driver;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opennms.pris.RequisitionGenerator;
import org.opennms.pris.api.Configuration;
import org.opennms.pris.model.Requisition;
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

        // Create the marshaller for the requisition
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

                String[] pathParts = request.getPathInfo().substring(1).split("/");

                //Check if a requisition or something else was requested
                final String requestType = pathParts[0];
                LOGGER.debug("requestType is '{}'", requestType);

                if (requestType != null) {
                    if (requestType.equalsIgnoreCase("requisitions")) {

                        // Get the instance for the request path
                        String instance = null;
                        if (pathParts.length > 1) {
                            instance = pathParts[1];
                        }

                        if (instance == null || instance.isEmpty() || instance.contains("favicon.ico")) {
                            response.sendError(404, "No instance specified");
                        } else {
                            LOGGER.debug("Handling request for instance: {}", instance);
                            try {
                                // Create the requisition provider for the instance
                                final RequisitionGenerator requisitionProvider = new RequisitionGenerator(instance);

                                // Generate the requisition
                                final Requisition requisition = requisitionProvider.generate(instance);

                                // Marshall the requisition and write it to the response stream
                                jaxbMarshaller.marshal(requisition, response.getOutputStream());
                            } catch (final Exception ex) {
                                response.sendError(500, ex.getMessage());
                                LOGGER.warn("Request failed", ex);
                            }
                        }
                    } else {
                        if (requestType.equalsIgnoreCase("documentation")) {
                            response.sendError(500, "documentations is not supported at the moment");
                        } else {
                            response.sendError(500, "requestType " + requestType + " causes a problem. try requisitions/myInstance");
                            LOGGER.error("requestType '{}' of path '{}'", requestType, request.getPathInfo());
                        }
                    }
                } else {
                    response.sendError(404, "No requisitions/instance specified");
                }
            }
        });
        // Start the server
        server.start();

        server.join();
    }
}
