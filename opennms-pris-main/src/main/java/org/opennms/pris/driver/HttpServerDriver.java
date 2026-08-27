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

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.opennms.pris.api.Configuration;
import org.opennms.pris.configapi.ConfigApiHandler;
import org.opennms.pris.configapi.RequisitionConfigRepository;

/**
 * A working mode providing a HTTP server publishing generated requisitions.
 *
 * The server exports the configured instances as URLs. The requested path is
 * used as instance name and the the returned result is the XML serialized
 * requisition.
 *
 * @author Dustin Frisch &lt;fooker@lab.sh&gt;
 */
public class HttpServerDriver implements Driver {

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
        this.start().join();
    }

    /**
     * Builds and starts the server without joining it.
     *
     * Kept separate from {@link #run()} so tests can start an instance on an
     * ephemeral port, talk to it and stop it again.
     *
     * @return the started server
     */
    public Server start() throws Exception {
        // Create an embedded jetty instance
        final Server server = new Server(new InetSocketAddress(this.config.getString("host", "127.0.0.1"),
                this.config.getInt("port", 8000)));


        // custom handling for requisitions
        RequisitionProviderHandler requisitionProviderHandler = new RequisitionProviderHandler();
        ContextHandler contextHandlerRequisitions = new ContextHandler(requisitionProviderHandler, "/requisitions");

        // provide the documentation
        // Jetty 12 rejects a relative base resource as an "alias", so resolve
        // "./documentation/" (relative to the working directory) to an absolute,
        // normalized path before handing it to the ResourceHandler.
        final Path documentationRoot = Paths.get("documentation").toAbsolutePath().normalize();
        ResourceHandler docuResourceHandler = new ResourceHandler();
        docuResourceHandler.setDirAllowed(true);
        docuResourceHandler.setWelcomeFiles("index.html");
        docuResourceHandler.setBaseResource(
                ResourceFactory.of(docuResourceHandler).newResource(documentationRoot));

        // serving the docu at http://ip:port/
        ContextHandler rootContext = new ContextHandler(docuResourceHandler, "/");

        ContextHandlerCollection contextHandlerCollection =
                new ContextHandlerCollection(contextHandlerRequisitions, rootContext);

        // The configuration REST API is strictly opt-in: without the flag the
        // server exposes exactly the same contexts as before
        if (this.config.getBoolean("config.api.enabled", false)) {
            final String token = this.config.getString("config.api.token", "");
            if (token.isBlank()) {
                throw new IllegalStateException(
                        "config.api.enabled is set but config.api.token is missing - "
                        + "refusing to start the configuration API without authentication");
            }

            final Path base = this.config.getBasePath();
            final ConfigApiHandler configApiHandler = new ConfigApiHandler(token,
                    base.resolve("global.properties"),
                    new RequisitionConfigRepository(base.resolve("requisitions")));

            contextHandlerCollection.addHandler(new ContextHandler(configApiHandler, "/api/v1/config"));
        }

        server.setHandler(contextHandlerCollection);

        server.start();
        return server;
    }
}
