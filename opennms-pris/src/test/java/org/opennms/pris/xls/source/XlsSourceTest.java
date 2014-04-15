package org.opennms.pris.xls.source;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionCategory;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.pris.AssetField;
import org.opennms.pris.ConfigManager;

public class XlsSourceTest {

    private XlsSource xlsSource;
    private final Date TEST_DATE = new Date();
    private XMLGregorianCalendarImpl xMLGregorianCalendarImpl = new XMLGregorianCalendarImpl((GregorianCalendar) GregorianCalendar.getInstance());

    @Before
    public void setUp() throws ConfigurationException {

        Configuration config = new ConfigManager().getGlobalConfig();
        config.setProperty("xls.encoding", "ISO-8859-1");
        xlsSource = new XlsSource("test", config);
        // the file is set directly and not via the config object to about mocking the static starter and configmanager
        xlsSource.setXlsFile(new File("src/test/resources/test.xls"));
    }

    @Test
    public void basicTest() throws Exception {
        Requisition goalRequisition = new Requisition("test");
        RequisitionNode node = new RequisitionNode();
        node.setNodeLabel("TestNode");
        node.setForeignId("TestNode");
        node.getAssets().add(new RequisitionAsset(AssetField.address1.FIELD_NAME, "Wilhelmstraße 30"));
        node.getAssets().add(new RequisitionAsset(AssetField.city.FIELD_NAME, "Braunschweig"));
        node.getAssets().add(new RequisitionAsset(AssetField.comment.FIELD_NAME, "Died in Göttingen"));
        node.getAssets().add(new RequisitionAsset(AssetField.description.FIELD_NAME, "POB: Johann Carl Friedrich Gauß"));
        RequisitionInterface reqInterface = new RequisitionInterface();
        reqInterface.setIpAddr("1.2.3.4");
        reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        RequisitionMonitoredService service = new RequisitionMonitoredService("Test");
        reqInterface.getMonitoredServices().add(service);
        node.getInterfaces().add(reqInterface);
        goalRequisition.getNodes().add(node);
        node.getCategories().add(new RequisitionCategory("Test"));

        goalRequisition.setDate(TEST_DATE);
        goalRequisition.setDateStamp(xMLGregorianCalendarImpl);
        goalRequisition.setLastImport(xMLGregorianCalendarImpl);

        Requisition resultRequisition = (Requisition) xlsSource.dump();
        resultRequisition.setDate(TEST_DATE);
        resultRequisition.setDateStamp(xMLGregorianCalendarImpl);
        resultRequisition.setLastImport(xMLGregorianCalendarImpl);
        for (RequisitionNode resultNode : resultRequisition.getNodes()) {
            for (RequisitionAsset asset : resultNode.getAssets()) {
                System.out.println(resultNode.getNodeLabel() + "\t" + asset.getName() + "\t" + asset.getValue());
            }
        }
        assertEquals(goalRequisition, resultRequisition);
    }

    @Test
    public void testGetXlsFile() {
        System.out.println("getXlsFile");
    }
}
