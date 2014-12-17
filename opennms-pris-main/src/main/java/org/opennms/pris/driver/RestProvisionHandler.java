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
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opennms.pris.RequisitionGenerator;
import org.opennms.pris.Starter;
import org.opennms.pris.api.EndpointConfiguration;
import org.opennms.pris.model.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestProvisionHandler extends AbstractHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RestProvisionHandler.class);
   
    @Override
    public void handle(final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException {

        String[] pathParts = request.getPathInfo().substring(1).split("/");

        baseRequest.setHandled(true);

        // Get the instance for the request path
        String instance = pathParts[0];
        String endpoint = pathParts[1];
        String parameter = null;
        if (pathParts.length > 2) {
            parameter = pathParts[2];
        }

        if (instance == null || instance.isEmpty() || instance.contains("favicon.ico")) {
            response.sendError(404, "No instance specified");
        } else {
            try {
                LOGGER.debug("Handling request for instance: {}", instance);
                // check if instance exists
                // get config for endpoint
                // check if requisition is valid?
                // build the url for the event
                
                EndpointConfiguration endpointConfig = Starter.getConfigManager().getEndpointConfig(endpoint);
                String url = endpointConfig.getString("url");
                String user = endpointConfig.getString("user");
                String password = endpointConfig.getString("password");
                
                final RequisitionGenerator requisitionProvider = new RequisitionGenerator(instance);

                final Requisition requisition = requisitionProvider.generate(instance);
                
                //send the event
                send(url, user, password, requisition);
                //return 200 ok
                response.setStatus(200);
                response.getWriter().append("All Good!").append(request.getRequestURL().toString() + " | " + request.getRequestURI()).close();
            } catch (final Exception ex) {
                response.sendError(500, ex.getMessage());
                LOGGER.warn("Request failed", ex);
            }
        }
    }
    
    public void send(String url, String user, String password, Requisition requisition) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("url", url);
        //do the rest put with requisition data to url
    }
}