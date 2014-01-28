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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;

public class JdbcSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSource.class);
    private final String instance;
    private final Configuration config;
    private final String SPLITTER = ",";

    private final String PREFIX_NODE = "Node_";
    private final String PREFIX_CATEGORY = "cat_";
    private final String PREFIX_SERVICE = "svc_";
    private final String PREFIX_IP_ADDRESS = "IP_";
    private final String PREFIX_INTERFACE_TYPE = "IfType_";
    private final String PREFIX_ASSET_DESCRIPTION = "Asset_Description";

    private final String INTERFACE_TYPE_PRIMARY = "P";
    private final String INTERFACE_TYPE_SECONDARY = "S";
    private final String ASSET_DESCRIPTION = "description";

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

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        LOGGER.debug("-------- JDBC Connection Testing ------------");
        try {

            Class.forName(this.getJdbcDriver());

        } catch (ClassNotFoundException e) {

            LOGGER.error("JDBC Driver not found. Include it in class path.", e);
            return null;
        }

        LOGGER.debug("JDBC Driver Registered!");

        try {
            connection = DriverManager.getConnection(this.getJdbcUrl(), this.getJdbcUser(), this.getJdbcPassword());
        } catch (SQLException e) {
            LOGGER.error("Connection Failed! Check output console", e);
            return null;
        }

        if (connection != null) {
            LOGGER.debug("JDBC Connection is working!");
        } else {
            LOGGER.error("Failed to make connection!");
            return null;
        }

        try {
            statement = connection.createStatement();

            LOGGER.info(this.getJdbcSelectStatement());

            Set<String> columns = new TreeSet<String>();

            resultSet = statement.executeQuery(this.getJdbcSelectStatement());

            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            for (int i = 0; i < resultSetMetaData.getColumnCount(); i++) {
                columns.add(resultSetMetaData.getColumnName(i + 1));
            }

            LOGGER.info("Columns: {}",columns);

            if (resultSet != null) {
                while (resultSet.next()) {
                    String nodeLabel = getString(resultSet, "Node_Label", columns);

                    if (nodeLabel == null) {
                        LOGGER.warn("Node_Label is null");
                        continue;
                    }

                    RequisitionNode node = requisition.getNode(nodeLabel);

                    if (node == null) {
                        node = new RequisitionNode();
                        node.setNodeLabel(nodeLabel);
                        node.setForeignId(nodeLabel);
                        requisition.getNodes().add(node);
                    } else {
                        LOGGER.info("Existing node - updating {} ", nodeLabel);
                    }


                    String description = getString(resultSet, "Asset_Description", columns);

                    if (description != null) {
                        node.getAssets().add(new RequisitionAsset("description", description));
                    }

                    String ipAddress = getString(resultSet, "Ip_Address", columns);

                    if (ipAddress != null) {
                        RequisitionInterface reqInterface = node.getInterface(ipAddress);

                        if (reqInterface == null) {
                            reqInterface = new RequisitionInterface();
                            reqInterface.setIpAddr(ipAddress);
                            node.getInterfaces().add(reqInterface);
                        }

                        String ifType = getString(resultSet, "If_Type", columns);

                        if ("P".equalsIgnoreCase(ifType)) {
                            reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
                        } else {
                            if ("S".equalsIgnoreCase(ifType)) {
                                reqInterface.setSnmpPrimary(PrimaryType.SECONDARY);
                            } else {
                                reqInterface.setSnmpPrimary(PrimaryType.NOT_ELIGIBLE);
                            }
                        }
                        String service = getString(resultSet, "Svc", columns);

                        if (service != null) {
                            if (reqInterface.getMonitoredService(service) == null) {
                                reqInterface.getMonitoredServices().add(new RequisitionMonitoredService(service));
                            }
                        }
                    } else {
                        LOGGER.warn("Ip_Address is null, ignoring If_Type and Svc");
                    }

                    String category = getString(resultSet, "Cat", columns);

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

    private String getString(ResultSet resultSet, String columnName, Set<String> columns) {
        if (!columns.contains(columnName.toLowerCase())) {
            return null;
        }

        try {
            String result = resultSet.getString(columnName);

            if (result != null) {
                return result.trim();
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
