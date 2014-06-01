package org.opennms.opennms.pris.plugins.xls.source;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.nio.file.Paths;
import java.util.Date;
import java.util.GregorianCalendar;
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
import org.opennms.pris.api.AssetField;


public class XlsSourceTest {

    private XlsSource xlsSource;
    
    private final Date TEST_DATE = new Date();
    private XMLGregorianCalendarImpl xMLGregorianCalendarImpl = new XMLGregorianCalendarImpl((GregorianCalendar) GregorianCalendar.getInstance());

    @Before
    public void setUp() {
        MockInstanceConfiguration config = new MockInstanceConfiguration("test");
        config.set("encoding", "ISO-8859-1");
        config.set("file", Paths.get("src/test/resources/test.xls"));
        
        xlsSource = new XlsSource(config);
    }

    @Test
    public void basicTest() throws Exception {
        Requisition goalRequisition = new Requisition("test");
        RequisitionNode node = new RequisitionNode();
        node.setNodeLabel("TestNode");
        node.setForeignId("TestNode");
        node.getAssets().add(new RequisitionAsset(AssetField.address1.name, "Wilhelmstraße 30"));
        node.getAssets().add(new RequisitionAsset(AssetField.city.name, "Braunschweig"));
        node.getAssets().add(new RequisitionAsset(AssetField.comment.name, "Died in Göttingen"));
        node.getAssets().add(new RequisitionAsset(AssetField.description.name, "POB: Johann Carl Friedrich Gauß"));
        node.getAssets().add(new RequisitionAsset(AssetField.vendor.name, "Vater"));
        node.getAssets().add(new RequisitionAsset(AssetField.vendorPhone.name, "123"));
        RequisitionInterface reqInterface = new RequisitionInterface();
        reqInterface.setIpAddr("1.2.3.4");
        reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        reqInterface.setStatus(1);
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