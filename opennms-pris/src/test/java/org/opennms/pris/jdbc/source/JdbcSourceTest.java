package org.opennms.pris.jdbc.source;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcSourceTest {

    private Connection connection = null;
    private final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private final String CONNECTION_URL_CREATE = "jdbc:derby:memory:testDB;create=true";
    private final String CONNECTION_URL_SHUTDOWN = "jdbc:derby:memory:testDB;shutdown=true;";
    private final String CONNECTION_URL_DROP = "jdbc:derby:memory:testDB;drop=true";

    private final String SQL_CREATE_ALL = "CREATE TABLE node (" +
            "foreignId INT," +
            "nodeLabel VARCHAR(255)," +
            "ipAddress VARCHAR(255)," +
            "ifType CHAR(1)," +
            "description VARCHAR(255)," +
            "city VARCHAR(255)," +
            "state VARCHAR(255)," +
            "serviceName VARCHAR(255)," +
            "categoryName VARCHAR(255)," +
            "PRIMARY KEY (foreignId))";

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("derby.stream.error.field", "DerbyUtil.DEV_NULL");
    }


    @Before
    public void setUp() throws ClassNotFoundException, SQLException {
        Class.forName(DRIVER);
        connection = DriverManager.getConnection(CONNECTION_URL_CREATE);
        Statement stmnt = connection.createStatement();
        stmnt.executeUpdate(SQL_CREATE_ALL);

        insertRow("1", "node1", "192.168.0.1", "P", "description1", "city1", "state1", "service1", "category1");
        insertRow("2", "node2", "192.168.0.2", "P", "description2", "city2", "state2", "service2", "category2");
        insertRow("3", "node3", "192.168.0.3", "P", "description3", "city3", "state3", "service3", "category3");
    }

    private void insertRow(String foreignId, String nodeLabel, String ipAddress, String ifType, String description, String city, String state, String serviceName, String categoryName) throws SQLException {
        String DML = "INSERT INTO node (foreignId, nodeLabel, ipAddress, ifType, description, city, state, serviceName, categoryName) VALUES (" + foreignId + ", '" + nodeLabel + "', '" + ipAddress + "', '" + ifType + "', '" + description + "', '" + city + "', '" + state + "', '" + serviceName + "', '" + categoryName + "')";
        Statement stmnt = connection.createStatement();
        stmnt.executeUpdate(DML);
    }

    @After
    public void cleanUp() {
        try {
            connection.close();
            DriverManager.getConnection(CONNECTION_URL_DROP);
            DriverManager.getConnection(CONNECTION_URL_SHUTDOWN);
        } catch (SQLException ex) {
        }
    }

    @Test
    public void testDB() throws SQLException {
        System.out.println("testDB");
        Statement selectAll = connection.createStatement();
        ResultSet selectAllResult = selectAll.executeQuery("SELECT foreignId AS Foreign_Id, " +
                "nodelabel AS Node_Label," +
                "ipAddress AS IP_Address," +
                "ifType AS If_Type," +
                "description AS Asset_Description," +
                "city AS Asset_City," +
                "state AS Asset_State," +
                "serviceName AS Svc," +
                "categoryName AS Cat FROM node");
        while (selectAllResult.next()) {
            System.out.println("Results: " + selectAllResult.getString("Foreign_Id") + " " + selectAllResult.getString("Node_Label"));
        }
    }

    @Test
    public void testDump() {
        System.out.println("dump");
    }
}
