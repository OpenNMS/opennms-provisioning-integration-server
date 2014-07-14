package org.opennms.opennms.pris.plugins.xls.source;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.opennms.pris.api.MockInstanceConfiguration;
import org.opennms.pris.model.AssetField;
import org.opennms.pris.model.PrimaryType;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionCategory;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.RequisitionNode;
import org.opennms.pris.util.RequisitionUtils;

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
        Requisition resultRequisition = (Requisition) xlsSource.dump();
        
        assertEquals(resultRequisition.getForeignSource(), "test");
        assertEquals(1, resultRequisition.getNodes().size());
        
        RequisitionNode resultNode = resultRequisition.getNodes().get(0);
        assertEquals("TestNode", resultNode.getNodeLabel());
        assertEquals("TestNode", resultNode.getForeignId());
        
        assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.vendor.name).getValue(), "Vater");
        assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.city.name).getValue(), "Braunschweig");
        assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.vendorPhone.name).getValue(), "123");
        assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.address1.name).getValue(), "Wilhelmstraße 30");
        assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.description.name).getValue(), "POB: Johann Carl Friedrich Gauß");
        assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.comment.name).getValue(), "Died in Göttingen");
        
        RequisitionInterface resultInterface = RequisitionUtils.findInterface(resultNode, "1.2.3.4");
        assertEquals(PrimaryType.PRIMARY, resultInterface.getSnmpPrimary());
        assertEquals(1, resultInterface.getStatus());
        
        RequisitionMonitoredService resultService = RequisitionUtils.findMonitoredService(resultInterface, "Test");
        assertEquals("Test", resultService.getServiceName());
        
        RequisitionCategory findCategory = RequisitionUtils.findCategory(resultNode, "Test");
        assertEquals("Test", findCategory.getName());
    }

    @Test
    public void testGetXlsFile() {
        System.out.println("getXlsFile");
    }
}
