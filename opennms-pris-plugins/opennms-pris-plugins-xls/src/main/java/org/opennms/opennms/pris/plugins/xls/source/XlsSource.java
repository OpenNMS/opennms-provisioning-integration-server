/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/ *****************************************************************************
 */
package org.opennms.opennms.pris.plugins.xls.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.kohsuke.MetaInfServices;
import org.opennms.opennms.pris.plugins.xls.source.exceptions.InvalidInterfaceException;
import org.opennms.opennms.pris.plugins.xls.source.exceptions.MissingRequiredColumnHeaderException;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.opennms.pris.model.AssetField;
import org.opennms.pris.model.PrimaryType;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionAsset;
import org.opennms.pris.model.RequisitionCategory;
import org.opennms.pris.model.RequisitionInterface;
import org.opennms.pris.model.RequisitionMonitoredService;
import org.opennms.pris.model.RequisitionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsSource implements Source {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XlsSource.class);

	private final InstanceConfiguration config;

	private final String WITHIN_SPLITTER = ",";

	private final String PREFIX_FOR_ASSETS = "Asset_";
	private final String INTERFACE_TYPE_PRIMARY = "P";
	private final String INTERFACE_TYPE_SECONDARY = "S";

	private Map<String, Integer> requiredColumns;
	private Map<String, List<Integer>> optionalMultiColumns;
	private Map<String, Integer> optionalUniquHeaders;
	private Map<String, Integer> assetColumns;

	private final String encoding;

	private File xls = null;

	public XlsSource(final InstanceConfiguration config) {
		this.config = config;

		this.encoding = config.getString("encoding", "ISO-8859-1");
	}

	private Workbook getWorkbook() throws FileNotFoundException {
		if (getXlsFile() != null) {
			xls = new File(getXlsFile());
		}
		if (!xls.canRead()) {
			return null;
		}

		// FIXME this seems shit!
		try {
			return new XSSFWorkbook(new FileInputStream(xls));
		} catch (Exception e) {
			try {
				return new HSSFWorkbook(new FileInputStream(xls));
			} catch (Exception e1) {
			} 
		}

		return null;

	}

	@Override
	public Object dump() throws MissingRequiredColumnHeaderException, Exception {
		final String instance = this.config.getInstanceIdentifier();

		Requisition requisition = new Requisition().withForeignSource(instance);
		Workbook workbook = getWorkbook();
		List<String> sheetNames = new ArrayList<String>();
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			sheetNames.add(workbook.getSheetName(i));
		}
		if (!sheetNames.contains(instance)) {
			LOGGER.error("can not find sheet {} in workbook from file {}",
					instance, xls.getAbsolutePath());
			workbook.close();
			throw new RuntimeException("can not find sheet " + instance
					+ " in workbook from file " + xls.getAbsolutePath());
		}
		Sheet sheet = workbook.getSheet(instance);
		if (sheet == null) {
			LOGGER.error(
					"can not read sheet {} in workbook from file {} check the configured encoding {}",
					instance, xls.getAbsolutePath(), encoding);
			workbook.close();
			throw new RuntimeException("can not read sheet " + instance
					+ " from file " + xls.getAbsolutePath()
					+ " check the encoding " + encoding + ".");
		}

		requiredColumns = initializeRequiredColumns(sheet);
		optionalMultiColumns = initializeOptionalMultiColumns(sheet);
		optionalUniquHeaders = initializeOptionalUniquHeaders(sheet);
		assetColumns = initializeAssetColumns(sheet);

		RequisitionInterface reqInterface;
		Iterator<Row> rowiterator = sheet.rowIterator();
		if (rowiterator.hasNext()) {
			rowiterator.next();
		}
		while (rowiterator.hasNext()) {
			Row row = rowiterator.next();
			Cell cell = row
					.getCell(getRelevantColumnID(REQUIRED_UNIQUE_PREFIXES.PREFIX_NODE));

			if (cell == null) {
				continue;
			}

			String nodeLabel = cell.getStringCellValue();
			RequisitionNode node = new RequisitionNode();
			node.setNodeLabel(nodeLabel);
			node.setForeignId(nodeLabel);

			cell = row
					.getCell(getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_FOREIGN_ID));
			if (cell != null) {
				node.setForeignId(cell.getStringCellValue());
			}

			cell = row
					.getCell(getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_LOCATION));
			if (cell != null) {
				node.setLocation(cell.getStringCellValue());
			}

			// adding parent data
			cell = row
					.getCell(getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_PARENT_FOREIGN_SOURCE));
			if (cell != null) {
				node.setParentForeignSource(cell.getStringCellValue());
			}

			cell = row
					.getCell(getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_PARENT_FOREIGN_ID));
			if (cell != null) {
				node.setParentForeignId(cell.getStringCellValue());
			}

			cell = row
					.getCell(getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_PARENT_NODE_LABEL));
			if (cell != null) {
				node.setParentNodeLabel(cell.getStringCellValue());
			}

			node.getCategories().addAll(getCategoriesByRow(row));

			// adding assets
			node.getAssets().addAll(getAssetsByRow(row));

			// Add interface
			reqInterface = getInterfaceByRow(row);

			// Add services to the interface
			reqInterface.getMonitoredServices().addAll(getServicesByRow(row));
			node.getInterfaces().add(reqInterface);
			requisition.getNodes().add(node);
		}
		workbook.close();
		LOGGER.info("xls source delivered for requisition '{}' '{}' nodes",
				instance, requisition.getNodes().size());
		return requisition;
	}

	private Integer getRelevantColumnID(REQUIRED_UNIQUE_PREFIXES prefix) {
		return requiredColumns.get(prefix.PREFIX);
	}

	private Integer getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS header) {
		return optionalUniquHeaders.get(header.HEADER);
	}

	private List<Integer> getRelevantColumnIDs(
			OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES prefix) {
		return optionalMultiColumns.get(prefix.PREFIX);
	}

	private Map<String, Integer> initializeRequiredColumns(Sheet sheet)
			throws MissingRequiredColumnHeaderException {
		Map<String, Integer> result = new HashMap<>();
		for (REQUIRED_UNIQUE_PREFIXES prefix : REQUIRED_UNIQUE_PREFIXES
				.values()) {
			Row row = sheet.getRow(0);
			Iterator<Cell> celliterator = row.cellIterator();
			while (celliterator.hasNext()) {
				Cell cell = celliterator.next();
				if (cell.getStringCellValue().toLowerCase()
						.startsWith(prefix.PREFIX.toLowerCase())) {
					result.put(prefix.PREFIX, cell.getColumnIndex());
				}
			}
			if (!result.containsKey(prefix.PREFIX)) {
				throw new MissingRequiredColumnHeaderException(prefix.PREFIX);
			}
		}
		return result;
	}

	private Map<String, Integer> initializeOptionalUniquHeaders(Sheet sheet) {
		Map<String, Integer> result = new HashMap<>();
		for (OPTIONAL_UNIQUE_HEADERS header : OPTIONAL_UNIQUE_HEADERS.values()) {
			Row row = sheet.getRow(0);
			Iterator<Cell> celliterator = row.cellIterator();
			while (celliterator.hasNext()) {
				Cell cell = celliterator.next();
				if (cell.getStringCellValue().toLowerCase()
						.startsWith(header.HEADER.toLowerCase())) {
					result.put(header.HEADER, cell.getColumnIndex());
				}
			}
		}
		return result;
	}

	private Map<String, List<Integer>> initializeOptionalMultiColumns(
			Sheet sheet) {
		Map<String, List<Integer>> result = new HashMap<>();
		for (OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES prefix : OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES
				.values()) {
			Row row = sheet.getRow(0);
			Iterator<Cell> celliterator = row.cellIterator();
			while (celliterator.hasNext()) {
				Cell cell = celliterator.next();
				if (cell.getStringCellValue().toLowerCase()
						.startsWith(prefix.PREFIX.toLowerCase())) {
					if (result.containsKey(prefix.PREFIX)) {
						result.get(prefix.PREFIX).add(cell.getColumnIndex());
					} else {
						List<Integer> columnIds = new ArrayList<>();
						columnIds.add(cell.getColumnIndex());
						result.put(prefix.PREFIX, columnIds);
					}
				}
			}
		}
		return result;
	}

	private Map<String, Integer> initializeAssetColumns(Sheet sheet) {
		Map<String, Integer> result = new HashMap<>();
		for (AssetField prefix : AssetField.values()) {
			Row row = sheet.getRow(0);
			Iterator<Cell> celliterator = row.cellIterator();
			while (celliterator.hasNext()) {
				Cell cell = celliterator.next();
				if (cell.getStringCellValue().toLowerCase()
						.equalsIgnoreCase(PREFIX_FOR_ASSETS + prefix.name)) {
					if (result.containsKey(prefix.name)) {
						result.put(prefix.name, cell.getColumnIndex());
					} else {
						result.put(prefix.name, cell.getColumnIndex());
					}
				}
			}
		}
		return result;
	}

	private Set<RequisitionCategory> getCategoriesByRow(Row row) {
		Set<RequisitionCategory> categories = new HashSet<>();
		List<Integer> relevantColumnIDs = getRelevantColumnIDs(OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES.PREFIX_CATEGORY);
		if (relevantColumnIDs != null) {
			for (Integer column : relevantColumnIDs) {
				String rawCategories = row.getCell(column).getStringCellValue()
						.trim();
				for (String category : rawCategories.split(WITHIN_SPLITTER)) {
					category = category.trim();
					if (!category.isEmpty()) {
						categories.add(new RequisitionCategory(category));
					}
				}
			}
		}
		return categories;
	}

	private Set<RequisitionMonitoredService> getServicesByRow(Row row) {
		Set<RequisitionMonitoredService> services = new HashSet<>();
		List<Integer> relevantColumnIDs = getRelevantColumnIDs(OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES.PREFIX_SERVICE);
		if (relevantColumnIDs != null) {
			for (Integer column : relevantColumnIDs) {
				String rawServices = row.getCell(column).getStringCellValue()
						.trim();
				for (String service : rawServices.split(WITHIN_SPLITTER)) {
					service = service.trim();
					if (!service.isEmpty()) {
						services.add(new RequisitionMonitoredService()
								.withServiceName(service));
					}
				}
			}
		}
		return services;
	}

	private Set<RequisitionAsset> getAssetsByRow(Row row) {
		Set<RequisitionAsset> assets = new HashSet<>();
		for (Map.Entry<String, Integer> entry : assetColumns.entrySet()) {
			String value;
			Cell cell = row.getCell(entry.getValue());
			if (cell == null) {
				continue;
			}
			if (cell.getCellTypeEnum() == CellType.NUMERIC) {
				Double d = cell.getNumericCellValue();
				int di = (int) d.doubleValue();
				value = Integer.toString(di);
			} else {
				value = cell.getStringCellValue().trim();
			}
			if (!value.isEmpty()) {
				assets.add(new RequisitionAsset(entry.getKey(), value));
			}
		}
		return assets;
	}

	private RequisitionInterface getInterfaceByRow(Row row)
			throws InvalidInterfaceException {
		RequisitionInterface reqInterface = new RequisitionInterface();
		try {
			reqInterface
					.setIpAddr(row
							.getCell(
									getRelevantColumnID(REQUIRED_UNIQUE_PREFIXES.PREFIX_IP_ADDRESS))
							.getStringCellValue().trim());
		} catch (IllegalArgumentException ex) {
			throw new InvalidInterfaceException(
					"Invalid IP-Address for node '"
							+ row.getCell(
									getRelevantColumnID(REQUIRED_UNIQUE_PREFIXES.PREFIX_NODE))
									.getStringCellValue().trim()
							+ "' at row '"
							+ row.getRowNum()
							+ "' and IP '"
							+ row.getCell(
									getRelevantColumnID(REQUIRED_UNIQUE_PREFIXES.PREFIX_IP_ADDRESS))
									.getStringCellValue().trim() + "'", ex);
		}
		String interfaceType = row
				.getCell(
						getRelevantColumnID(REQUIRED_UNIQUE_PREFIXES.PREFIX_INTERFACE_MANGEMENT_TYPE))
				.getStringCellValue().trim();
		if (interfaceType.equalsIgnoreCase(INTERFACE_TYPE_PRIMARY)) {
			reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
		} else if (interfaceType.equalsIgnoreCase(INTERFACE_TYPE_SECONDARY)) {
			reqInterface.setSnmpPrimary(PrimaryType.SECONDARY);
		} else {
			reqInterface.setSnmpPrimary(PrimaryType.NOT_ELIGIBLE);
		}
		if (getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_INTERFACE_STATUS) != null) {
			Cell cell = row
					.getCell(getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_INTERFACE_STATUS));
			if (cell != null && cell.getCellTypeEnum() == CellType.NUMERIC) {
				Double d = row
						.getCell(
								getRelevantColumnID(OPTIONAL_UNIQUE_HEADERS.PREFIX_INTERFACE_STATUS))
						.getNumericCellValue();
				int di = (int) d.doubleValue();
				reqInterface.setStatus(di);
			}
		}
		return reqInterface;
	}

	public String getXlsFile() {
		if (xls == null) {
			Path xlsFilePath = this.config.getPath("file", null);
			if (xlsFilePath == null) {
				return null;
			}
			return xlsFilePath.toString();
		} else {
			return xls.getAbsolutePath();
		}
	}

	public void setXlsFile(File xls) {
		this.xls = xls;
	}

	/**
	 * This header-prefixes are required.
	 * 
	 * They can just be used for one column.
	 */
	private enum REQUIRED_UNIQUE_PREFIXES {

		PREFIX_NODE("Node_"), PREFIX_IP_ADDRESS("IP_"), PREFIX_INTERFACE_MANGEMENT_TYPE(
				"MgmtType_");

		private final String PREFIX;

		private REQUIRED_UNIQUE_PREFIXES(String prefix) {
			this.PREFIX = prefix;
		}
	}

	/*
	 * This header-prefixes are optional. Can be used at multiple columns. Can
	 * contain splitted values.
	 */
	private enum OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES {

		PREFIX_CATEGORY("cat_"), PREFIX_SERVICE("svc_");

		private final String PREFIX;

		private OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES(String prefix) {
			this.PREFIX = prefix;
		}
	}

	/**
	 * This headers are optional.
	 * 
	 * They can just be used for one column.
	 */
	private enum OPTIONAL_UNIQUE_HEADERS {
		PREFIX_INTERFACE_STATUS("InterfaceStatus"), PREFIX_FOREIGN_ID("ID_"), PREFIX_LOCATION(
				"Location"), PREFIX_PARENT_FOREIGN_SOURCE(
				"Parent_Foreign_Source"), PREFIX_PARENT_FOREIGN_ID(
				"Parent_Foreign_Id"), PREFIX_PARENT_NODE_LABEL(
				"Parent_Node_Label");

		private final String HEADER;

		private OPTIONAL_UNIQUE_HEADERS(String header) {
			this.HEADER = header;
		}
	}

	@MetaInfServices
	public static class Factory implements Source.Factory {

		@Override
		public String getIdentifier() {
			return "xls";
		}

		@Override
		public Source create(final InstanceConfiguration config) {
			return new XlsSource(config);
		}
	}
}
