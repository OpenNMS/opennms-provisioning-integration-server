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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.opennms.pris.api.EndpointConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProvisionHandler extends PushProvisionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProvisionHandler.class);

    private final String UEI = "uei.opennms.org/internal/importer/reloadImport";

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
            String provisionUrl = request.getRequestURL().toString().replaceFirst("provisionEvent", "requisitions");
            provisionUrl = provisionUrl.substring(0, provisionUrl.length() - rescanExisting.length());
            
            String host = endpointConfiguration.getString("host");
            Integer port = endpointConfiguration.getInt("port");
            send(provisionUrl, host, port);
            response.setStatus(200);
            response.getWriter().append("Send event " + UEI + " to endpint: " + endpoint + " at " + host + ":" + port + " to provision instance " + instance).close();
        } catch (final Exception ex) {
            response.sendError(500, "Sending event to OpenNMS failed: " + ex.getMessage());
        }
    }

    public void send(String url, String host, Integer port) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("url", url);
        params.put("rescanExisting", rescanExisting);
        String eventXML = createEvent(params);
        try (Socket clientSocket = new Socket(host, port)) {
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            outToServer.writeBytes(eventXML);
        }
    }

    private String createEvent(Map<String, String> parms) {
        StringBuilder sb = new StringBuilder();
        sb.append("<log>");
        sb.append("<events>");
        sb.append("<event>");
        sb.append("<uei>");
        sb.append(UEI);
        sb.append("</uei>");
        sb.append("<source>PRIS</source>");
        if (parms != null && !parms.isEmpty()) {
            sb.append("<parms>");
            for (Map.Entry<String, String> parm : parms.entrySet()) {
                sb.append("<parm>");

                sb.append("<parmName><![CDATA[");
                sb.append(parm.getKey());
                sb.append("]]></parmName>");

                sb.append("<value type=\"string\" encoding=\"text\"><![CDATA[");
                sb.append(parm.getValue());
                sb.append("]]></value>");

                sb.append("</parm>");
            }
            sb.append("</parms>");
        }
        sb.append("</event>");
        sb.append("</events>");
        sb.append("</log>");
        LOGGER.debug("Event to send '{}'", sb.toString());
        return sb.toString();
    }

}