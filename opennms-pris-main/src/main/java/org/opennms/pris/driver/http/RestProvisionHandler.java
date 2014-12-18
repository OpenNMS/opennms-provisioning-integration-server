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
package org.opennms.pris.driver.http;

import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Request;
import org.opennms.pris.RequisitionGenerator;
import org.opennms.pris.api.EndpointConfiguration;
import org.opennms.pris.model.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestProvisionHandler extends PushProvisionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestProvisionHandler.class);

    @Override
    public void handle(final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException {

        baseRequest.setHandled(true);

        this.initializeParameters(request, response);
        EndpointConfiguration endpointConfiguration = this.verifyEndpoint(endpoint, request, response);
        this.verifyInstance(instance, request, response);

        try {
            LOGGER.debug("Handling request for instance: {}", instance);

            final RequisitionGenerator requisitionProvider = new RequisitionGenerator(instance);

            // Generate the requisition
            final Requisition requisition = requisitionProvider.generate(instance);

            String url = endpointConfiguration.getString("url");
            String user = endpointConfiguration.getString("user");
            String password = endpointConfiguration.getString("password");
            send(url, user, password, requisition, rescanExisting);
            response.setStatus(200);
            response.getWriter().append("Send requisition to OpenNMS via rest").close();
        } catch (final Exception ex) {
            response.sendError(500, "Sending requisition to OpenNMS via Rest failed: " + ex.getMessage());
        }
    }

    private void send(String url, String user, String password, Requisition requisition, String rescanExisting) throws JAXBException, IOException {

        //TODO http code to send the requisition and trigger the sync
        // Create the marshaller for the requisition
        final JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);
        final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(requisition, sw);
        StringEntity requisitionString = new StringEntity(sw.toString());
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(requisitionString);
        
        try (CloseableHttpResponse response2 = httpclient.execute(httpPost)) {
            System.out.println(response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity2);
        }
    }
}
