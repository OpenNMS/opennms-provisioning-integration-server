/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/
package org.opennms.pris.source;

import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Source to merge two requisitions (A and B) provided via HTTP. The merge behavior is configurable and allows
 * the following configuration:
 *
 * Default: natural join between A and B
 * keepAllA: is similar like an outer left join between A and B
 * keepAllB: is similar like an outer right join between A and B
 */
public class HttpRequisitionMergeSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequisitionMergeSource.class);

    // instanceName?
    private final String instance;
    private final Configuration config;

    private HttpRequisitionMergeSource(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    private Requisition getRequisition(String url, String userName, String password) {
        Requisition requisition = null;

        if (url != null) {
            try {
                HttpClientBuilder builder = HttpClientBuilder.create();

                // If username and password was found, inject the credentials
                if (userName != null && password != null) {

                    CredentialsProvider provider = new BasicCredentialsProvider();

                    // Create the authentication scope
                    AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);

                    // Create credential pair
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);

                    // Inject the credentials
                    provider.setCredentials(scope, credentials);

                    // Set the default credentials provider
                    builder.setDefaultCredentialsProvider(provider);
                }

                HttpClient client = builder.build();
                HttpGet request = new HttpGet(url);
                HttpResponse response = client.execute(request);
                try {

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);

                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    requisition = (Requisition) jaxbUnmarshaller.unmarshal(bufferedReader);

                } catch (JAXBException e) {
                    LOGGER.error("The responce did not contain a valid requisition as xml.", e);
                }
                LOGGER.debug("Got Requisition {}", requisition);
            } catch (IOException ex) {
                LOGGER.error("Requestiong requisition from {} faild", url, ex);
                return null;
            }
        } else {
            LOGGER.error("Parameter requisition.url is missing in requisition.properties");
            return null;
        }
        return requisition;
    }

    @Override
    public Object dump() throws Exception {
        LOGGER.info("HttpRequisitionMergeSource at work....");
        Requisition a = getRequisition(getAUrl(), getAUserName(), getAPassword());
        Requisition b = getRequisition(getBUrl(), getBUserName(), getBPassword());
        Map<String, RequisitionNode> nodesByForeignIdA = new HashMap<>();
        Map<String, RequisitionNode> nodesByForeignIdB = new HashMap<>();

        Map<String, RequisitionNode> mergedNodes = new HashMap<>();

        if (a != null && b != null) {
            for (RequisitionNode node : a.getNodes()) {
                nodesByForeignIdA.put(node.getForeignId(), node);
            }

            for (RequisitionNode node : b.getNodes()) {
                nodesByForeignIdB.put(node.getForeignId(), node);
            }

            //merge matching nodes
            for (Map.Entry<String, RequisitionNode> entry : nodesByForeignIdA.entrySet()) {
                if (nodesByForeignIdB.containsKey(entry.getKey())) {
                    RequisitionNode node = mergeNodes(entry.getValue(), nodesByForeignIdB.get(entry.getKey()));
                    mergedNodes.put(node.getForeignId(), node);
                }
            }
            if (isKeepAllA()) {
                // can you move that to a separate method, you use it a couple of lines below again?!
                for (RequisitionNode node : a.getNodes()) {
                    if (!mergedNodes.containsKey(node.getForeignId())) {
                        mergedNodes.put(node.getForeignId(), node);
                    }
                }
            }
            if (isKeepAllB()) {
                for (RequisitionNode node : b.getNodes()) {
                    if (!mergedNodes.containsKey(node.getForeignId())) {
                        mergedNodes.put(node.getForeignId(), node);
                    }
                }
            }
        } else {
            // can you please not throw RuntimeException?
            throw new RuntimeException("one or more requisitions have not been loaded correctly");
        }

        Requisition result = new Requisition(instance);
        for (RequisitionNode node : mergedNodes.values()) {
            result.insertNode(node);
        }

        return result;
    }

    // Keeps everything that nodeA has and adds everything that nodeB has but nodeA has not.
    private RequisitionNode mergeNodes(RequisitionNode nodeA, RequisitionNode nodeB) {
        //Add all nodeB categories to nodeA, do not duplicate
        for (RequisitionCategory category : nodeB.getCategories()) {
            if (nodeA.getCategory(category.getName()) == null) {
                nodeA.getCategories().add(category);
            }
        }

        //Add all nodeB assets to nodeA, do not duplicate
        for (RequisitionAsset asset : nodeB.getAssets()) {
            if (nodeA.getAsset(asset.getName()) == null) {
                nodeA.getAssets().add(asset);
            }
        }

        //Add all Interfaces and Services from nodeB to nodeA
        for (RequisitionInterface interfaceB : nodeB.getInterfaces()) {
            if (nodeA.getInterface(interfaceB.getIpAddr()) == null) {
                //Interface dose not exist on nodeA, add entire interfaceB to nodeA
                nodeA.getInterfaces().add(interfaceB);
            } else {
                //get interface information from interfaceB if interfaceA has non
                RequisitionInterface interfaceA = nodeA.getInterface(interfaceB.getIpAddr());
                if (interfaceA.getDescr() != null && interfaceB.getDescr() != null) {
                    if (interfaceA.getDescr().isEmpty() && !interfaceB.getDescr().isEmpty()) {
                        interfaceA.setDescr(interfaceB.getDescr());
                    }
                }

                if (interfaceA.getSnmpPrimary().equals(PrimaryType.NOT_ELIGIBLE) && !interfaceB.getSnmpPrimary().equals(PrimaryType.NOT_ELIGIBLE)) {
                    interfaceA.setSnmpPrimary(interfaceB.getSnmpPrimary());
                }

                //Interface exists on both requisitions, add all interfaceB services to interfaceA
                Map<String, RequisitionMonitoredService> servicesA = new HashMap<>();
                for (RequisitionMonitoredService serviceA : nodeA.getInterface(interfaceB.getIpAddr()).getMonitoredServices()) {
                    servicesA.put(serviceA.getServiceName(), serviceA);
                }
                for (RequisitionMonitoredService serviceB : interfaceB.getMonitoredServices()) {
                    if (!servicesA.containsKey(serviceB.getServiceName())) {
                        nodeA.getInterface(interfaceB.getIpAddr()).getMonitoredServices().add(serviceB);
                    }
                }

            }
        }

        //Set parent information from nodeB if nodeA has non
        if (nodeA.getParentForeignId() == null && nodeA.getParentForeignSource() == null && nodeA.getParentNodeLabel() == null) {
            if (nodeB.getParentForeignId() == null && nodeB.getParentForeignSource() == null && nodeB.getParentNodeLabel() == null) {
                nodeA.setParentForeignId(nodeB.getParentForeignId());
                nodeA.setParentForeignSource(nodeB.getParentForeignSource());
                nodeA.setParentNodeLabel(nodeB.getParentNodeLabel());
            }
        }

        //Set building from nodeB if nodeA has non
        if (nodeA.getBuilding() != null && nodeB.getBuilding() != null) {
            if (nodeA.getBuilding().isEmpty() && !nodeB.getBuilding().isEmpty()) {
                nodeA.setBuilding(nodeB.getBuilding());
            }
        }

        //Set city from nodeB if nodeA has non
        if (nodeA.getCity() != null && nodeB.getCity() != null) {
            if (nodeA.getCity().isEmpty() && !nodeB.getCity().isEmpty()) {
                nodeA.setCity(nodeB.getCity());
            }
        }

        return nodeA;
    }

    public final String getAUserName() {
        return this.config.getString("requisition.A.username");
    }

    public final String getAPassword() {
        return this.config.getString("requisition.A.password");
    }

    public final String getAUrl() {
        return this.config.getString("requisition.A.url");
    }

    public final String getBUserName() {
        return this.config.getString("requisition.B.username");
    }

    public final String getBPassword() {
        return this.config.getString("requisition.B.password");
    }

    public final String getBUrl() {
        return this.config.getString("requisition.B.url");
    }

    private boolean isKeepAllA() {
        return this.config.containsKey("requisition.merge.keepAllA");
    }

    private boolean isKeepAllB() {
        return this.config.containsKey("requisition.merge.keepAllB");
    }

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance, final Configuration config) {
            return new HttpRequisitionMergeSource(instance, config);
        }
    }
}
