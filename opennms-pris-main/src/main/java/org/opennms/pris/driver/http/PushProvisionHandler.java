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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opennms.pris.Starter;
import org.opennms.pris.api.EndpointConfiguration;
import org.opennms.pris.api.InstanceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PushProvisionHandler extends AbstractHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PushProvisionHandler.class);
    protected String instance;
    protected String endpoint;
    protected String rescanExisting;

    protected void initializeParameters(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String[] pathParts = request.getPathInfo().substring(1).split("/");
            instance = pathParts[0];
            endpoint = pathParts[1];
            LOGGER.debug("instance '{}'", instance);
            LOGGER.debug("endpoint '{}'", endpoint);
            if (pathParts.length >= 3 && !pathParts[2].isEmpty()) {
                if (pathParts[2].equals("true") || pathParts[2].equals("false") || pathParts[2].equals("dbonly")) {
                    rescanExisting = pathParts[2];
                } else {
                    LOGGER.warn("Value '{}' for rescanExisting not valid. Ignoring parameter.", rescanExisting);
                    rescanExisting = "true";
                }
            } else {
                rescanExisting = "true";
            }
        } catch (final Exception ex) {
            //TODO message is Event based
            response.sendError(500, "wrong format of URL. Provide an http://ip:port/provisionEvent/instance/endpoint format");
        }
    }

    protected EndpointConfiguration verifyEndpoint(String endpoint, HttpServletRequest request, HttpServletResponse response) throws IOException {
        EndpointConfiguration endpointConfig = null;
        try {
            endpointConfig = Starter.getConfigManager().getEndpointConfig(endpoint);
        } catch (final Exception ex) {
            response.sendError(500, "Endpoint " + endpoint + " is unknown");
        }
        if (endpointConfig == null || endpointConfig.isEmpty()) {
            response.sendError(500, "EndpointConfig for '" + endpoint + "' does not exist or is empty");
        }
        return endpointConfig;
    }

    protected InstanceConfiguration verifyInstance(String instance, HttpServletRequest request, HttpServletResponse response) throws IOException {
        InstanceConfiguration instanceConfig = null;
        try {
            instanceConfig = Starter.getConfigManager().getInstanceConfig(instance);
        } catch (final Exception ex) {
            response.sendError(500, "Instance " + instance + " is unknown");
        }
        if (instanceConfig == null || instanceConfig.isEmpty()) {
            response.sendError(500, "InstanceConfig for '" + instance + "' does not exist or is empty");
        }
        return instanceConfig;
    }
    
}
