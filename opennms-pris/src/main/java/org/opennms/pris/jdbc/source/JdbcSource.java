/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/
 * *****************************************************************************
 */
package org.opennms.pris.jdbc.source;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.*;
import org.opennms.pris.ASSET_FIELD;
import org.opennms.pris.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * A JDBC data source allows to connect to an SQL database and extract data in a given format. The result set is mapped to
 * an OpenNMS requisition.
 */
// move to org.opennms.pris.source.jdbc
public class JdbcSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSource.class);

    private final String instance;
    private final Configuration config;

    /**
     * Columns which have to be mapped from the SQL result set to an OpenNMS requisition
     */
    private static final String COLUMN_NODE_LABEL = "Node_Label";
    private static final String COLUMN_CATEGORY = "Cat";
    private static final String COLUMN_SERVICE = "Svc";
    private static final String COLUMN_IP_ADDRESS = "Ip_Address";
    private static final String COLUMN_INTERFACE_TYPE = "If_Type";
    private static final String COLUMN_FOREIGN_ID = "Foreign_Id";
    private static final String INTERFACE_TYPE_PRIMARY = "P";
    private static final String INTERFACE_TYPE_SECONDARY = "S";

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance, final Configuration config) {
            return new JdbcSource(instance, config);
        }
    }

    public JdbcSource(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Object dump() {

        Requisition requisition = new Requisition(instance);

        Statement statement;
        ResultSet resultSet;

        LOGGER.debug("-------- JDBC Connection Testing ------------");
        try {

            Class.forName(this.getJdbcDriver());

        } catch (ClassNotFoundException e) {

            LOGGER.error("JDBC Driver not found. Include it in class path.", e);
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

                    RequisitionNode node = requisition.getNode(foreignId);

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

                    String ipAddress = getString(resultSet, COLUMN_IP_ADDRESS);

                    if (ipAddress != null) {
                        RequisitionInterface reqInterface = node.getInterface(ipAddress);

                        if (reqInterface == null) {
                            reqInterface = new RequisitionInterface();
                            reqInterface.setIpAddr(ipAddress);
                            node.getInterfaces().add(reqInterface);
                        }

                        String ifType = getString(resultSet, COLUMN_INTERFACE_TYPE);

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
                            if (reqInterface.getMonitoredService(service) == null) {
                                reqInterface.getMonitoredServices().add(new RequisitionMonitoredService(service));
                            }
                        }
                    } else {
                        LOGGER.warn(COLUMN_IP_ADDRESS + " is null, ignoring " + COLUMN_INTERFACE_TYPE + " and " + COLUMN_SERVICE);
                    }

                    String category = getString(resultSet, COLUMN_CATEGORY);

                    if (category != null) {
                        if (node.getCategory(category) == null) {
                            node.getCategories().add(new RequisitionCategory(category));
                        }
                    }

                    for (ASSET_FIELD assetField : ASSET_FIELD.values()) {
                        String assetValue = getString(resultSet, "Asset_" + assetField.getFieldName());
                        if (assetValue != null) {
                            LOGGER.info("Adding to node:{} the asset:{} with value:{}", node.getNodeLabel(), assetField.getFieldName(), assetValue);
                            if (node.getAsset(assetField.getFieldName()) == null) {
                                node.getAssets().add(new RequisitionAsset(assetField.getFieldName(), assetValue));
                            } else {
                                node.getAsset(assetField.getFieldName()).setValue(assetValue);
                            }
                        }
                    }
                }
            } else {
                LOGGER.error("ResultSet is null");
            }

        } catch (SQLException ex) {
            LOGGER.error("SQL problem", ex);
        }

        return requisition;
    }

    private String getString(ResultSet resultSet, String columnName) {
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
        return this.config.getString("jdbc.driver", null);
    }

    public String getJdbcUrl() {
        return this.config.getString("jdbc.url", null);
    }

    public String getJdbcUser() {
        return this.config.getString("jdbc.user", null);
    }

    public String getJdbcPassword() {
        return this.config.getString("jdbc.password", null);
    }

    public String getJdbcSelectStatement() {
        String arr[] = this.config.getStringArray("jdbc.selectStatement");
        return StringUtils.join(arr, ',');
    }
}
