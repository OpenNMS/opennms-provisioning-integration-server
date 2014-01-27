package org.opennms.provisioner.jdbc.source;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.provisioner.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            resultSet = statement.executeQuery(this.getJdbcSelectStatement());

            if (resultSet != null) {
                while (resultSet.next()) {
                    String nodeLabel = resultSet.getString("MyNode");
                    LOGGER.info("NodeLabel :: {}", nodeLabel);
                    RequisitionNode node = new RequisitionNode();
                    node.setNodeLabel(nodeLabel);
                    node.setForeignId(nodeLabel);

                    requisition.getNodes().add(node);
                }
            } else {
                LOGGER.error("ResultSet was NULL");
            }

        } catch (SQLException ex) {
            LOGGER.error("SQL problem", ex);
        }

        return requisition;
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
        return this.config.getString("jdbc.selectStatement", null);
    }
}
