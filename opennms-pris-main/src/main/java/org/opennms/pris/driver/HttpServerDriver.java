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

import java.net.InetSocketAddress;
import org.eclipse.jetty.rewrite.handler.RedirectPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.opennms.pris.api.Configuration;
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

    private final String DOCU_PATH = "/documentation";
    
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
                this.config.getInt("port", 8000)));
        
        
        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        
        // custom handling for requisitions
        RequisitionProviderHandler requisitionProviderHandler = new RequisitionProviderHandler();
        ContextHandler contextHandlerRequisitions = new ContextHandler("/requisitions");
        contextHandlerRequisitions.setHandler(requisitionProviderHandler);
        contextHandlerCollection.addHandler(contextHandlerRequisitions);

        // custom handling for provision
        PushProvisionHandler pushProvisionHandler = new PushProvisionHandler();
        ContextHandler contextHandlerPushRequisitions = new ContextHandler("/provision");
        contextHandlerPushRequisitions.setHandler(pushProvisionHandler);
        contextHandlerCollection.addHandler(contextHandlerPushRequisitions);
        
        // provide the documentation
        ResourceHandler docuResourceHandler = new ResourceHandler();
        docuResourceHandler.setDirectoriesListed(true);
        docuResourceHandler.setWelcomeFiles(new String[]{"index.html"});
        docuResourceHandler.setResourceBase("./documentation/");
        ContextHandler contextHandlerDocu = new ContextHandler(DOCU_PATH);
        contextHandlerDocu.setHandler(docuResourceHandler);
        contextHandlerCollection.addHandler(contextHandlerDocu);

        // redirecting http://ip:port/ to the docu
        RewriteHandler redirector = new RewriteHandler();
        redirector.setRewriteRequestURI(true);
        redirector.setRewritePathInfo(false);

        RedirectPatternRule rootToDocuRedirectRule = new RedirectPatternRule();
        rootToDocuRedirectRule.setPattern("/*");
        rootToDocuRedirectRule.setLocation(DOCU_PATH);
        redirector.addRule(rootToDocuRedirectRule);

        ContextHandler rootContext = new ContextHandler("/");
        rootContext.setHandler(redirector);
        contextHandlerCollection.addHandler(rootContext);
        
        server.setHandler(contextHandlerCollection);

        server.start();
        server.join();
    }
}