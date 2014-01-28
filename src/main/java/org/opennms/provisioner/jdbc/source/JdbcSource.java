package org.opennms.provisioner.jdbc.source;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionCategory;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.provisioner.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSource.class);

    private final String instance;
    private final Configuration config;

    private static final String COLUMN_NODE_LABEL = "Node_Label";
    private static final String COLUMN_CATEGORY = "Cat";
    private static final String COLUMN_SERVICE = "Svc";
    private static final String COLUMN_IP_ADDRESS = "Ip_Address";
    private static final String COLUMN_INTERFACE_TYPE = "If_Type";
    private static final String COLUMN_ASSET_DESCRIPTION = "Asset_Description";
    private static final String COLUMN_FOREIGN_ID = "Foreign_Id";
    private static final String INTERFACE_TYPE_PRIMARY = "P";
    private static final String INTERFACE_TYPE_SECONDARY = "S";
    private static final String ASSET_DESCRIPTION = "description";

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

                    String description = getString(resultSet, COLUMN_ASSET_DESCRIPTION);

                    if (description != null) {
                        if (node.getAsset(ASSET_DESCRIPTION) == null) {
                            node.getAssets().add(new RequisitionAsset(ASSET_DESCRIPTION, description));
                        } else {
                            node.getAsset(ASSET_DESCRIPTION).setValue(description);
                        }
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
