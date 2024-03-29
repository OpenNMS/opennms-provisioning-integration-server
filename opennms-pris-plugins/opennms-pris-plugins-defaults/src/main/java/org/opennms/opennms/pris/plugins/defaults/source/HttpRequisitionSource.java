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

package org.opennms.opennms.pris.plugins.defaults.source;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.MetaInfServices;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.opennms.pris.model.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * A external OpenNMS requisition provided through HTTP
 */
public class HttpRequisitionSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequisitionSource.class);

    private final InstanceConfiguration config;

    private HttpRequisitionSource(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        Requisition requisition = null;

        if (getUrl() != null) {
            HttpClientBuilder builder = HttpClientBuilder.create();

            // If username and password was found, inject the credentials
            if (getUserName() != null && getPassword() != null) {

                CredentialsProvider provider = new BasicCredentialsProvider();

                // Create the authentication scope
                AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);

                // Create credential pair
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(getUserName(), getPassword());

                // Inject the credentials
                provider.setCredentials(scope, credentials);

                // Set the default credentials provider
                builder.setDefaultCredentialsProvider(provider);
            }

            HttpClient client = builder.build();
            HttpGet request = new HttpGet(getUrl());
            HttpResponse response = client.execute(request);
            try {

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);

                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                requisition = (Requisition) jaxbUnmarshaller.unmarshal(bufferedReader);

            } catch (JAXBException e) {
                LOGGER.error("The response did not contain a valid requisition as xml.", e);
            }
            LOGGER.debug("Got Requisition {}", requisition);
        } else {
            LOGGER.error("Parameter requisition.url is missing in requisition.properties");
        }
        if (requisition == null) {
            LOGGER.error("Requisition is null for unknown reasons");
            return null;
        }
        LOGGER.info("HttpRequisitionSource delivered for requisition '{}'", requisition.getNodes().size());
        return requisition;
    }

    public final String getUserName() {
        return this.config.getString("username", null);
    }

    public final String getPassword() {
        return this.config.getString("password", null);
    }

    public final String getUrl() {
        return this.config.getString("url");
    }

    @MetaInfServices
    public static class Factory implements Source.Factory {

        @Override
        public String getIdentifier() {
            return "http";
        }

        @Override
        public Source create(final InstanceConfiguration config) {
            return new HttpRequisitionSource(config);
        }
    }
}
