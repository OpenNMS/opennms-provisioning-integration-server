/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/ *****************************************************************************
 */
package org.opennms.opennms.pris.plugins.jdbc.source;

import org.kohsuke.MetaInfServices;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.opennms.pris.model.*;
import org.opennms.pris.util.RequisitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * A JDBC data source allows to connect to an SQL database and extract data in a given format. The result set is mapped to an OpenNMS requisition.
 */
public class JdbcSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSource.class);

    private final InstanceConfiguration config;

    /**
     * Columns which have to be mapped from the SQL result set to an OpenNMS requisition
     */
    private static final String COLUMN_NODE_LABEL = "Node_Label";
    private static final String COLUMN_LOCATION = "Location";
    private static final String COLUMN_CATEGORY = "Cat";
    private static final String COLUMN_SERVICE = "Svc";
    private static final String COLUMN_IP_ADDRESS = "Ip_Address";
    private static final String COLUMN_INTERFACE_MANGEMENT_TYPE = "MgmtType";
    private static final String COLUMN_INTERFACE_STATUS = "InterfaceStatus";
    private static final String COLUMN_FOREIGN_ID = "Foreign_Id";
    private static final String INTERFACE_TYPE_PRIMARY = "P";
    private static final String INTERFACE_TYPE_SECONDARY = "S";
    private static final String COLUMN_PARENT_NODE_LABEL = "Parent_Node_Label";
    private static final String COLUMN_PARENT_FOREIGN_ID = "Parent_Foreign_Id";
    private static final String COLUMN_PARENT_FOREIGN_SOURCE = "Parent_Foreign_Source";

    public JdbcSource(final InstanceConfiguration config) {
        this.config = config;
    }

    @Override
    public Object dump() {
        Requisition requisition = new Requisition();
        requisition.setForeignSource(this.config.getInstanceIdentifier());

        Statement statement;
        ResultSet resultSet;

        LOGGER.debug("-------- JDBC Connection Testing ------------");
        try {

            Class.forName(this.getJdbcDriver());

        } catch (ClassNotFoundException e) {

            LOGGER.error("JDBC Driver not found in class path.", e);
            return null;
        }

        LOGGER.debug("JDBC Driver Registered!");

        try (Connection connection = DriverManager.getConnection(this.getJdbcUrl(), this.getJdbcUser(), this.getJdbcPassword())) {
            statement = connection.createStatement();

            LOGGER.info(this.getJdbcSelectStatement());

            resultSet = statement.executeQuery(this.getJdbcSelectStatement());

            if (resultSet != null) {
                while (resultSet.next()) {
                    String foreignId = getString(resultSet, COLUMN_FOREIGN_ID);

                    if (foreignId == null) {
                        LOGGER.warn("Foreign_Id is null - skipping row...");
                        continue;
                    }

                    RequisitionNode node = RequisitionUtils.findNode(requisition, foreignId);

                    if (node == null) {
                        node = new RequisitionNode();
                        node.setForeignId(foreignId);
                        requisition.getNodes().add(node);
                    } else {
                        LOGGER.info("Existing node - updating {} ", foreignId);
                    }

                    String nodeLabel = getString(resultSet, COLUMN_NODE_LABEL);

                    if (nodeLabel != null) {
                        node.setNodeLabel(nodeLabel);
                    }

                    String location = getString(resultSet, COLUMN_LOCATION);

                    if (location != null) {
                        node.setLocation(location);
                    }

                    String parentNodeLabel = getString(resultSet, COLUMN_PARENT_NODE_LABEL);
                    if (parentNodeLabel != null) {
                        node.setParentNodeLabel(parentNodeLabel);
                    }
                    
                    String parentForeignId = getString(resultSet, COLUMN_PARENT_FOREIGN_ID);
                    if (parentForeignId != null) {
                        node.setParentForeignId(parentForeignId);
                    }
                    
                    String parentForeignSource = getString(resultSet, COLUMN_PARENT_FOREIGN_SOURCE);
                    if (parentForeignSource != null) {
                        node.setParentForeignSource(parentForeignSource);
                    }
                    
                    String ipAddress = getString(resultSet, COLUMN_IP_ADDRESS);

                    if (ipAddress != null) {
                        RequisitionInterface reqInterface = RequisitionUtils.findInterface(node, ipAddress);

                        if (reqInterface == null) {
                            reqInterface = new RequisitionInterface();
                            reqInterface.setIpAddr(ipAddress);
                            node.getInterfaces().add(reqInterface);
                        }

                        String ifStatus = getString(resultSet, COLUMN_INTERFACE_STATUS);
                        if (ifStatus != null) {
                            try {
                                reqInterface.setStatus(Integer.parseInt(ifStatus));
                            } catch (NumberFormatException ex) {
                                LOGGER.warn("'{}' for IP '{}' is not a valid integer, ignoring value '{}'", COLUMN_INTERFACE_STATUS, ipAddress, ifStatus, ex);
                            }
                        }

                        String ifType = getString(resultSet, COLUMN_INTERFACE_MANGEMENT_TYPE);
                        if (INTERFACE_TYPE_PRIMARY.equalsIgnoreCase(ifType)) {
                            reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
                        } else {
                            if (INTERFACE_TYPE_SECONDARY.equalsIgnoreCase(ifType)) {
                                reqInterface.setSnmpPrimary(PrimaryType.SECONDARY);
                            } else {
                                reqInterface.setSnmpPrimary(PrimaryType.NOT_ELIGIBLE);
                            }
                        }
                        String service = getString(resultSet, COLUMN_SERVICE);

                        if (service != null) {
                            if (RequisitionUtils.findMonitoredService(reqInterface, service) == null) {
                                RequisitionMonitoredService monitoredService = new RequisitionMonitoredService();
                                monitoredService.setServiceName(service);
                                
                                reqInterface.getMonitoredServices().add(monitoredService);
                            }
                        }
                    } else {
                        LOGGER.warn(COLUMN_IP_ADDRESS + " is null, ignoring " + COLUMN_INTERFACE_MANGEMENT_TYPE + " and " + COLUMN_SERVICE);
                    }

                    String category = getString(resultSet, COLUMN_CATEGORY);

                    if (category != null) {
                        if (RequisitionUtils.findCategory(node, category) == null) {
                            node.getCategories().add(new RequisitionCategory(category));
                        }
                    }

                    for (AssetField assetField : AssetField.values()) {
                        String assetValue = getString(resultSet, "Asset_" + assetField.name);
                        if (assetValue != null) {
                            LOGGER.info("Adding to node:{} the asset:{} with value:{}", node.getNodeLabel(), assetField.name, assetValue);
                            RequisitionAsset asset = RequisitionUtils.findAsset(node, assetField.name);
                            if (asset == null) {
                                node.getAssets().add(asset = new RequisitionAsset().withName(assetField.name));
                            }
                            
                            asset.setValue(assetValue);
                        }
                    }
                }
            } else {
                LOGGER.error("ResultSet is null");
            }

        } catch (SQLException ex) {
            LOGGER.error("SQL problem", ex);
        }
        LOGGER.info("JDBC-Source delivered for requisition '{}'", requisition.getNodes().size());
        return requisition;
    }

    private String getString(ResultSet resultSet,
                             String columnName) {
        try {
            String result = resultSet.getString(columnName);

            if (result != null) {
                result = result.trim();
            }

            if ("".equals(result)) {
                return null;
            }

            return result;
        } catch (SQLException sqlException) {
            return null;
        }
    }

    public String getJdbcDriver() {
        return this.config.getString("driver", null);
    }

    public String getJdbcUrl() {
        return this.config.getString("url", null);
    }

    public String getJdbcUser() {
        return this.config.getString("user", null);
    }

    public String getJdbcPassword() {
        return this.config.getString("password", null);
    }

    public String getJdbcSelectStatement() {
        return this.config.getString("selectStatement");
    }

    @MetaInfServices
    public static class Factory implements Source.Factory {

        @Override
        public String getIdentifier() {
            return "jdbc";
        }

        @Override
        public Source create(final InstanceConfiguration config) {
            return new JdbcSource(config);
        }
    }
}
