/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.opennms.pris.plugins.xls.source;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.Test;
import org.opennms.pris.api.MockInstanceConfiguration;
import org.opennms.pris.model.AssetField;
import org.opennms.pris.model.MetaData;
import org.opennms.pris.model.PrimaryType;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionCategory;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.RequisitionNode;
import org.opennms.pris.util.RequisitionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class XlsSourceTest {

	private XlsSource xlsSource;

	@Test
	public void testxlsSource() throws Exception {
		MockInstanceConfiguration config = new MockInstanceConfiguration("test");
		config.set("encoding", "ISO-8859-1");
		config.set("file", Paths.get("src/test/resources/test.xls"));

		xlsSource = new XlsSource(config);

		publishTestRequisitionAndSheet("test");

		basicTest("test");

	}

	@Test
	public void testCsvSource() throws Exception {

		MockInstanceConfiguration config = new MockInstanceConfiguration("testcsv");
		config.set("encoding", "ISO-8859-1");
		config.set("file", Paths.get("src/test/resources/testcsv.csv"));

		xlsSource = new XlsSource(config);

		publishTestRequisitionAndSheet("testcsv");

		basicTest("testcsv");

	}

	@Test
	public void testCsvSourceNoHeader() throws Exception {
		MockInstanceConfiguration config = new MockInstanceConfiguration("testcsv-noheaders");
		config.set("encoding", "ISO-8859-1");
		config.set("file", Paths.get("src/test/resources/testcsv-noheaders.csv"));

		config.set("org.opennms.pris.spreadsheet.fields",
				"Parent_Foreign_Source,Parent_Foreign_ID,Parent_Node_Label,ID_,Node_Label,Location,Asset_Description,IP_Address,MgmtType_For_SNMP,InterfaceStatus,Cat_Test,Svc_Test,Asset_City,Asset_Address1,Asset_Address2,Asset_Comment,Asset_Vendor,Asset_VendorPhone,MetaData_KeyWithoutContext,MetaData_Context:KeyWithContext,Asset_latitude,Asset_longitude");

		xlsSource = new XlsSource(config);

		publishTestRequisitionAndSheet("testcsv-noheaders");

		basicTest("testcsv-noheaders");

	}

	// test method used by xls and csv tests
	public void basicTest(String foreignSource) throws Exception {
		Requisition resultRequisition = (Requisition) xlsSource.dump();

		assertEquals(resultRequisition.getForeignSource(), foreignSource);
		assertEquals(2, resultRequisition.getNodes().size());

		RequisitionNode resultNode = resultRequisition.getNodes().get(0);
		assertEquals("TestNode", resultNode.getNodeLabel());
		assertEquals("TestNode", resultNode.getForeignId());
		assertEquals("Test-Parent-Foreign-Source", resultNode.getParentForeignSource());
		assertEquals("Test-Parent-Foreign-Id", resultNode.getParentForeignId());
		assertEquals("Test-Parent-Node-Label", resultNode.getParentNodeLabel());
		assertEquals("Test-Location", resultNode.getLocation());

		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.vendor.name).getValue(), "Vater");
		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.city.name).getValue(), "Braunschweig");
		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.vendorPhone.name).getValue(), "123");
		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.address1.name).getValue(), "Wilhelmstraße 30");
		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.description.name).getValue(),
				"POB: Johann Carl Friedrich Gauß");
		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.comment.name).getValue(), "Died in Göttingen");

		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.latitude.name).getValue(), "54.9633229");
		assertEquals(RequisitionUtils.findAsset(resultNode, AssetField.longitude.name).getValue(), "1");

		RequisitionInterface resultInterface = RequisitionUtils.findInterface(resultNode, "1.2.3.4");
		assertEquals(PrimaryType.PRIMARY, resultInterface.getSnmpPrimary());
		assertEquals(1, resultInterface.getStatus());

		RequisitionMonitoredService resultService = RequisitionUtils.findMonitoredService(resultInterface, "Test");
		assertEquals("Test", resultService.getServiceName());

		RequisitionCategory findCategory = RequisitionUtils.findCategory(resultNode, "Test");
		assertEquals("Test", findCategory.getName());

		assertThat(resultNode.getMetaDatas(),
				containsInAnyOrder(new MetaData("requisition", "KeyWithoutContext", "Foo"),
						new MetaData("Context", "KeyWithContext", "Bar")));
	}

	// test method used by xls and csv tests
	public void getNodeWithMultipleIpInterfaces(String foreignSource) throws Exception {
		Requisition resultRequisition = (Requisition) xlsSource.dump();
		assertEquals(resultRequisition.getForeignSource(), foreignSource);
		RequisitionNode resultNode = resultRequisition.getNodes().get(1);
		assertEquals(resultNode.getInterfaces().size(), 2);
		assertEquals(resultNode.getNodeLabel(), "Node2Ips");
		assertEquals(resultNode.getInterfaces().get(0).getIpAddr(), "23.23.23.23");
		assertEquals(resultNode.getInterfaces().get(0).getSnmpPrimary(), "P");
		assertEquals(resultNode.getInterfaces().get(1).getIpAddr(), "42.42.42.42");
		assertEquals(resultNode.getInterfaces().get(1).getSnmpPrimary(), "S");
	}

	public void publishTestRequisitionAndSheet(String name) {

		// print out parsed spreadsheet
		File xls = new File(xlsSource.getXlsFile());
		Workbook workbook = xlsSource.getWorkbook(xls);
		File xlsFile = new File("target/" + name + ".xls");
		xlsFile.delete();
		try (FileOutputStream outputStream = new FileOutputStream(xlsFile)) {
			workbook.write(outputStream);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// print out xml requisition

		File csvFile = new File("target/" + name + ".xml");
		csvFile.delete();
		try (FileOutputStream outputStream = new FileOutputStream(csvFile)) {

			Requisition resultRequisition = (Requisition) xlsSource.dump();

			JAXBContext jc = JAXBContext.newInstance("org.opennms.pris.model");
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.marshal(resultRequisition, outputStream);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
