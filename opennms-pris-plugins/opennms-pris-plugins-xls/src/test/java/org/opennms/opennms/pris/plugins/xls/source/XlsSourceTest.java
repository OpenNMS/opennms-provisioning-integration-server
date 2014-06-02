package org.opennms.opennms.pris.plugins.xls.source;

import org.opennms.pris.api.MockInstanceConfiguration;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.opennms.pris.model.PrimaryType;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionAsset;
import org.opennms.pris.model.RequisitionCategory;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.RequisitionNode;
import org.opennms.pris.model.AssetField;


public class XlsSourceTest {

    private XlsSource xlsSource;
    
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
        Requisition goalRequisition = new Requisition();
        goalRequisition.setForeignSource("test");
        
        RequisitionNode node = new RequisitionNode();
        node.setNodeLabel("TestNode");
        node.setForeignId("TestNode");
        node.getAssets().add(new RequisitionAsset(AssetField.vendor.name, "Vater"));
        node.getAssets().add(new RequisitionAsset(AssetField.city.name, "Braunschweig"));
        node.getAssets().add(new RequisitionAsset(AssetField.vendorPhone.name, "123"));
        node.getAssets().add(new RequisitionAsset(AssetField.address1.name, "Wilhelmstraße 30"));
        node.getAssets().add(new RequisitionAsset(AssetField.description.name, "POB: Johann Carl Friedrich Gauß"));
        node.getAssets().add(new RequisitionAsset(AssetField.comment.name, "Died in Göttingen"));
        RequisitionInterface reqInterface = new RequisitionInterface();
        reqInterface.setIpAddr("1.2.3.4");
        reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        reqInterface.setStatus(1);
        RequisitionMonitoredService service = new RequisitionMonitoredService();
        service.setServiceName("Test");
        reqInterface.getMonitoredServices().add(service);
        node.getInterfaces().add(reqInterface);
        goalRequisition.getNodes().add(node);
        node.getCategories().add(new RequisitionCategory("Test"));

        goalRequisition.setDateStamp(xMLGregorianCalendarImpl);
        goalRequisition.setLastImport(xMLGregorianCalendarImpl);

        Requisition resultRequisition = (Requisition) xlsSource.dump();
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