package org.opennms.pris.xls.source;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.io.File;
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
import org.opennms.pris.AssetField;

public class XlsSourceTest {

    private XlsSource xlsSource;
    private Date testDate = new Date();
    private XMLGregorianCalendarImpl xMLGregorianCalendarImpl = new XMLGregorianCalendarImpl((GregorianCalendar) GregorianCalendar.getInstance());

    @Before
    public void setUp() {
        xlsSource = new XlsSource("test", null);
        xlsSource.setXlsFile(new File("src/test/resources/test.xls"));
    }

    @Test
    public void basicTest() throws Exception {
        Requisition goalRequisition = new Requisition("test");
        RequisitionNode node = new RequisitionNode();
        node.setNodeLabel("TestNode");
        node.setForeignId("TestNode");
        node.getAssets().add(new RequisitionAsset(AssetField.city.FIELD_NAME, "CityA"));
        node.getAssets().add(new RequisitionAsset(AssetField.description.FIELD_NAME, "Description"));
        RequisitionInterface reqInterface = new RequisitionInterface();
        reqInterface.setIpAddr("1.2.3.4");
        reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        RequisitionMonitoredService service = new RequisitionMonitoredService("Test");
        reqInterface.getMonitoredServices().add(service);
        node.getInterfaces().add(reqInterface);
        goalRequisition.getNodes().add(node);
        node.getCategories().add(new RequisitionCategory("Test"));

        goalRequisition.setDate(testDate);
        goalRequisition.setDateStamp(xMLGregorianCalendarImpl);
        goalRequisition.setLastImport(xMLGregorianCalendarImpl);

        Requisition resultRequisition = (Requisition) xlsSource.dump();
        resultRequisition.setDate(testDate);
        resultRequisition.setDateStamp(xMLGregorianCalendarImpl);
        resultRequisition.setLastImport(xMLGregorianCalendarImpl);

        assertEquals(goalRequisition, resultRequisition);
    }

    @Test
    public void testGetXlsFile() {
        System.out.println("getXlsFile");
    }
}
