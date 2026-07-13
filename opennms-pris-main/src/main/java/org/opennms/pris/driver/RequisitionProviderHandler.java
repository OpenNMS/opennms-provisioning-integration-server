/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.pris.driver;

import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.opennms.pris.RequisitionGenerator;
import org.opennms.pris.model.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequisitionProviderHandler extends Handler.Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionProviderHandler.class);

    @Override
    public boolean handle(final Request request,
            final Response response,
            final Callback callback) throws Exception {

        // The path within the "/requisitions" context, e.g. "/example" for /requisitions/example
        final String pathInContext = Request.getPathInContext(request);

        // Get the instance for the request path (first path segment)
        final String instance = pathInContext.isEmpty()
                ? ""
                : pathInContext.substring(1).split("/", 2)[0];

        if (instance.isEmpty() || instance.contains("favicon.ico")) {
            Response.writeError(request, response, callback, 404, "No instance specified");
            return true;
        }

        try {
            LOGGER.debug("Handling request for instance: {}", instance);
            // Create the requisition provider for the instance
            final RequisitionGenerator requisitionProvider = new RequisitionGenerator(instance);

            // Generate the requisition
            final Requisition requisition = requisitionProvider.generate(instance);

            // Create the marshaller for the requisition
            final JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
            final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            response.setStatus(200);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/xml");

            // Marshall the requisition and stream it to the response
            try (OutputStream out = Content.Sink.asOutputStream(response)) {
                jaxbMarshaller.marshal(requisition, out);
            }
            callback.succeeded();
        } catch (final Exception ex) {
            LOGGER.warn("Request failed", ex);
            Response.writeError(request, response, callback, 500, ex.getMessage());
        }

        return true;
    }
}
