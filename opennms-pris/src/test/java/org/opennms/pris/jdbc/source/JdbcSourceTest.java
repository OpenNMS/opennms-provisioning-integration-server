package org.opennms.pris.jdbc.source;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JdbcSourceTest {

    private final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private final String CONNECTION_URL_CREATE = "jdbc:derby:memory:testDB;create=true";
    private final String CONNECTION_URL_SHUTDOWN = "jdbc:derby:memory:testDB;shutdown=true;";
    private final String CONNECTION_URL_DROP = "jdbc:derby:memory:testDB;drop=true";
    private Connection connection = null;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("derby.stream.error.field", "DerbyUtil.DEV_NULL");
    }

    @Before
    public void setUp() throws ClassNotFoundException, SQLException {
        Class.forName(DRIVER);
        connection = DriverManager.getConnection(CONNECTION_URL_CREATE);
        String DDL = "CREATE TABLE test_table ("
                + "id INT NOT NULL, "
                + "name VARCHAR(20) NOT NULL, "
                + "PRIMARY KEY (id))";
        Statement stmnt = connection.createStatement();
        stmnt.executeUpdate(DDL);
        String DML = "INSERT INTO test_table VALUES (1, 'test data')";
        stmnt = connection.createStatement();
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
        ResultSet selectAllResult = selectAll.executeQuery("SELECT * FROM test_table");
        while (selectAllResult.next()) {
            System.out.println("Reulst: " + selectAllResult.getString("id") + " " + selectAllResult.getString("name"));
        }
    }

    @Test
    public void testDump() {
        System.out.println("dump");
    }
}
