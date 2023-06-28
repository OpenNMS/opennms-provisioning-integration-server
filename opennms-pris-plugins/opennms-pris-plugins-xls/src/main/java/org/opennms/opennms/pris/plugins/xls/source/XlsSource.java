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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.kohsuke.MetaInfServices;
import org.opennms.opennms.pris.plugins.xls.source.exceptions.InvalidInterfaceException;
import org.opennms.opennms.pris.plugins.xls.source.exceptions.MissingRequiredColumnHeaderException;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.Source;
import org.opennms.pris.model.AssetField;
import org.opennms.pris.model.MetaData;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(XlsSource.class);

	private final InstanceConfiguration config;

	private final String csvHeaders;

	private final String WITHIN_SPLITTER = ",";
	
	// note that the csv file cells are delimited by ',' but multiple values (services) within a cell must be delimited by ';' within cells
	private final String CSV_FILE_DELIMITER = ",";
	private final String CSV_FILE_WITHIN_SPLITTER = ";";
	private final String SPREADSHEET_HEADER_FIELDS="org.opennms.pris.spreadsheet.fields";

	private final String PREFIX_FOR_ASSETS = "Asset_";
	private final String PREFIX_FOR_METADATA = "MetaData_";

	private final String INTERFACE_TYPE_PRIMARY = "P";
	private final String INTERFACE_TYPE_SECONDARY = "S";

	private Map<String, Integer> requiredColumns;
	private Map<String, List<Integer>> optionalMultiColumns;
	private Map<String, Integer> optionalUniquHeaders;
	private Map<String, Integer> assetColumns;
	private Map<String, List<Integer>> metaDataColumns;

	private File xls;
	private final String encoding;

	public static String getStringValueFromCell(Cell cell) {
		String value = null;
		switch (cell.getCellTypeEnum()) {
		case NUMERIC:
			double d = cell.getNumericCellValue();
			if (d % 1 == 0) {
				value = Integer.toString((int) d);
			} else {
				// prints double with 7 decimal places - suitable for lat/long
				value = String.format("%.7f", d);
			}
			break;
		case STRING:
			value = cell.getStringCellValue();
			break;
		case BOOLEAN:
			value = ((Boolean) cell.getBooleanCellValue()).toString();
			break;
		default:
			break;
		}

		return value;
	}

	public static Integer getIntValueFromCell(Cell cell) {
		Integer value = null;
		switch (cell.getCellTypeEnum()) {
		case NUMERIC:
			value = (int) cell.getNumericCellValue();
			break;
		case STRING:
			value = Integer.getInteger(cell.getStringCellValue());
			break;
		default:
			break;
		}

		return value;
	}

	public XlsSource(final InstanceConfiguration config) {
		this.config = config;
		
		this.encoding = config.getString("encoding", "ISO-8859-1");

		this.csvHeaders = config.getString(SPREADSHEET_HEADER_FIELDS,"");
	}

	public Workbook getWorkbook(File file) {

		// if file is csv file then create spreadsheet in workbook from lines in csv file.
		String fileName = file.getName();
		String extension = null;
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i + 1);
			fileName = fileName.substring(0, i);
		}

		if ("csv".equals(extension)) {
			FileInputStream fis=null;
			try {
				HSSFWorkbook workbook = new HSSFWorkbook();
				HSSFSheet sheet = workbook.createSheet(fileName);

				// if headers provided as a configuration property then create first row of spreadsheet with
				// headers
				int rowNum = 0;
				if (csvHeaders != null && ! csvHeaders.isEmpty() ) {
					LOGGER.info("csv headers property set "+SPREADSHEET_HEADER_FIELDS+ " = "+csvHeaders);
					String[] headers = csvHeaders.split(CSV_FILE_DELIMITER);

					HSSFRow firstRow = sheet.createRow(rowNum);
					for (int headercount = 0; headercount < headers.length; headercount++) {
						HSSFCell cell = firstRow.createCell(headercount);
						cell.setCellValue(headers[headercount].trim());
					}
					rowNum++;
				}
				
				// create rest of spreadsheet with csv data in file see
				// https://javacodepoint.com/java-code-convert-csv-to-excel-file-using-apache-poi/
				fis = new FileInputStream(file);
			    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
			    BufferedReader br = new BufferedReader(isr);

				String nextLine;
				while ((nextLine = br.readLine()) != null) {
					Row currentRow = sheet.createRow(rowNum);
					
					String rowData[] = nextLine.split(CSV_FILE_DELIMITER);
					for (int column = 0; column < rowData.length; column++) {
						if (NumberUtils.isDigits(rowData[column])) {
							currentRow.createCell(column).setCellValue(Integer.parseInt(rowData[column].trim()));
							// isCreatable same as deprecated isNumber
						} else if (NumberUtils.isCreatable(rowData[column])) {
							currentRow.createCell(column).setCellValue(Double.parseDouble(rowData[column].trim()));
						} else {
							// String values we always substitute SPLITTERS
							String stringCellData = rowData[column].trim().replace(CSV_FILE_WITHIN_SPLITTER, WITHIN_SPLITTER);
							Cell cell = currentRow.createCell(column);
							cell.setCellValue(stringCellData);
						}
					}
					
					rowNum++;
				}
				
				br.close();
				
				return workbook;

			} catch (Exception ex) {
				LOGGER.error("can not create workbook from csv file {}", file.getAbsolutePath(), ex);
				return null;
			} finally {
				if(fis!=null)
					try {
						fis.close();
					} catch (IOException e) {}
			}
		}

		// for normal excel spreadsheet read file
		try {
			return new XSSFWorkbook(new FileInputStream(file));
		} catch (Exception e) {
			try {
				return new HSSFWorkbook(new FileInputStream(file));
			} catch (Exception e1) {
				LOGGER.error("can not create workbook from file {}", file.getAbsolutePath(), e);
				LOGGER.error("can not create workbook from file {}", file.getAbsolutePath(), e1);
			}
		}
		return null;

	}

	@Override
	public Object dump() throws MissingRequiredColumnHeaderException, Exception {
		final String instance = this.config.getInstanceIdentifier();

		Requisition requisition = new Requisition().withForeignSource(instance);
		xls = new File(getXlsFile());
		Workbook workbook = getWorkbook(xls);
		List<String> sheetNames = new ArrayList<String>();
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			sheetNames.add(workbook.getSheetName(i));
		}
		if (!sheetNames.contains(instance)) {
			LOGGER.error("can not find sheet {} in workbook from file {}", instance, xls.getAbsolutePath());
			workbook.close();
			throw new RuntimeException(
					"can not find sheet " + instance + " in workbook from file " + xls.getAbsolutePath());
		}

		Sheet sheet = workbook.getSheet(instance);
		if (sheet == null) {
			LOGGER.error("can not read sheet {} in workbook from file {} check the configured encoding {}", instance,
					xls.getAbsolutePath(), encoding);
			workbook.close();
			throw new RuntimeException("can not read sheet " + instance + " from file " + xls.getAbsolutePath()
					+ " check the encoding " + encoding + ".");
		}

		requiredColumns = initializeRequiredColumns(sheet);
		optionalMultiColumns = initializeOptionalMultiColumns(sheet);
		optionalUniquHeaders = initializeOptionalUniquHeaders(sheet);
		assetColumns = initializeAssetColumns(sheet);
		metaDataColumns = initializeMetaDataColumns(sheet);

		RequisitionInterface reqInterface;
		Iterator<Row> rowiterator = sheet.rowIterator();
		if (rowiterator.hasNext()) {
			rowiterator.next();
		}
		Map<String, RequisitionNode> nodeLabelRequisitionNodeMap = new HashMap<>();
		while (rowiterator.hasNext()) {
			Row row = rowiterator.next();
			Cell cell = getRelevantColumnID(row, REQUIRED_UNIQUE_PREFIXES.PREFIX_NODE);
			if (cell == null) {
				continue;
			}
			String nodeLabel = XlsSource.getStringValueFromCell(cell);
			RequisitionNode node = new RequisitionNode();
			if (nodeLabelRequisitionNodeMap.containsKey(nodeLabel)) {
				node = nodeLabelRequisitionNodeMap.get(nodeLabel);
			} else {
				node.setNodeLabel(nodeLabel);
				node.setForeignId(nodeLabel);
				nodeLabelRequisitionNodeMap.put(nodeLabel, node);
				requisition.getNodes().add(node);
			}

			cell = getRelevantColumnID(row, OPTIONAL_UNIQUE_HEADERS.PREFIX_FOREIGN_ID);
			if (cell != null) {
				node.setForeignId(XlsSource.getStringValueFromCell(cell));
			}

			cell = getRelevantColumnID(row, OPTIONAL_UNIQUE_HEADERS.PREFIX_LOCATION);
			if (cell != null) {
				node.setLocation(XlsSource.getStringValueFromCell(cell));
			}

			// adding parent data
			cell = getRelevantColumnID(row, OPTIONAL_UNIQUE_HEADERS.PREFIX_PARENT_FOREIGN_SOURCE);
			if (cell != null) {
				node.setParentForeignSource(XlsSource.getStringValueFromCell(cell));
			}

			cell = getRelevantColumnID(row, OPTIONAL_UNIQUE_HEADERS.PREFIX_PARENT_FOREIGN_ID);
			if (cell != null) {
				node.setParentForeignId(XlsSource.getStringValueFromCell(cell));
			}

			cell = getRelevantColumnID(row, OPTIONAL_UNIQUE_HEADERS.PREFIX_PARENT_NODE_LABEL);
			if (cell != null) {
				node.setParentNodeLabel(XlsSource.getStringValueFromCell(cell));
			}

			node.getCategories().addAll(getCategoriesByRow(row));

			// adding assets
			node.getAssets().addAll(getAssetsByRow(row));

			// adding meta-data
			node.getMetaDatas().addAll(getMetaDataByRow(row));

			// Add interface
			reqInterface = getInterfaceByRow(row);

			// Add services to the interface
			reqInterface.getMonitoredServices().addAll(getServicesByRow(row));
			node.getInterfaces().add(reqInterface);
		}
		workbook.close();
		LOGGER.info("xls source delivered for requisition '{}' '{}' nodes", instance, requisition.getNodes().size());
		return requisition;
	}

	private Cell getRelevantColumnID(Row row, REQUIRED_UNIQUE_PREFIXES prefix) {
		return row.getCell(requiredColumns.get(prefix.PREFIX));
	}

	private Cell getRelevantColumnID(Row row, OPTIONAL_UNIQUE_HEADERS header) {
		Integer columnId = optionalUniquHeaders.get(header.HEADER);
		if (columnId == null) {
			return null;
		}
		return row.getCell(columnId);
	}

	private List<Integer> getRelevantColumnIDs(OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES prefix) {
		return optionalMultiColumns.get(prefix.PREFIX);
	}

	private Map<String, Integer> initializeRequiredColumns(Sheet sheet) throws MissingRequiredColumnHeaderException {
		Map<String, Integer> result = new HashMap<>();
		for (REQUIRED_UNIQUE_PREFIXES prefix : REQUIRED_UNIQUE_PREFIXES.values()) {
			Row row = sheet.getRow(0);
			Iterator<Cell> celliterator = row.cellIterator();
			while (celliterator.hasNext()) {
				Cell cell = celliterator.next();
				if (cell.getStringCellValue().toLowerCase().startsWith(prefix.PREFIX.toLowerCase())) {
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
				if (cell.getStringCellValue().toLowerCase().startsWith(header.HEADER.toLowerCase())) {
					result.put(header.HEADER, cell.getColumnIndex());
				}
			}
		}
		return result;
	}

	private Map<String, List<Integer>> initializeOptionalMultiColumns(Sheet sheet) {
		Map<String, List<Integer>> result = new HashMap<>();
		for (OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES prefix : OPTIONAL_MULTIPLE_SPLITCONTENT_PREFIXES.values()) {
			Row row = sheet.getRow(0);
			Iterator<Cell> celliterator = row.cellIterator();
			while (celliterator.hasNext()) {
				Cell cell = celliterator.next();
				if (cell.getStringCellValue().toLowerCase().startsWith(prefix.PREFIX.toLowerCase())) {
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

	private Map<String, List<Integer>> initializeMetaDataColumns(final Sheet sheet) {
		final Map<String, List<Integer>> result = new HashMap<>();
		final Row row = sheet.getRow(0);
		final Iterator<Cell> celliterator = row.cellIterator();
		while (celliterator.hasNext()) {
			Cell cell = celliterator.next();
			final String cellName = cell.getStringCellValue();
			if (cellName.toLowerCase().startsWith(PREFIX_FOR_METADATA.toLowerCase())) {
				final String metaDataKey = cellName.substring(PREFIX_FOR_METADATA.length());
				if (!result.containsKey(metaDataKey)) {
					result.put(metaDataKey, new ArrayList<>());
				}
				result.get(metaDataKey).add(cell.getColumnIndex());
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
				if (cell.getStringCellValue().toLowerCase().equalsIgnoreCase(PREFIX_FOR_ASSETS + prefix.name)) {
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
				Cell cell = row.getCell(column);
				if (cell == null) {
					continue;
				}
				String rawCategories = XlsSource.getStringValueFromCell(cell);
				if (rawCategories == null) {
					continue;
				}
				for (String category : rawCategories.trim().split(WITHIN_SPLITTER)) {
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
				Cell cell = row.getCell(column);
				if (cell == null) {
					continue;
				}
				String value = XlsSource.getStringValueFromCell(cell);
				if (value == null) {
					continue;
				}
				String rawServices = value.trim();
				for (String service : rawServices.split(WITHIN_SPLITTER)) {
					service = service.trim();
					if (!service.isEmpty()) {
						services.add(new RequisitionMonitoredService().withServiceName(service));
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
			value = XlsSource.getStringValueFromCell(cell);
			if (value != null && !value.isEmpty()) {
				assets.add(new RequisitionAsset(entry.getKey(), value));
			}
		}
		return assets;
	}

	private Set<MetaData> getMetaDataByRow(Row row) {
		Set<MetaData> metaData = new HashSet<>();
		for (final Map.Entry<String, List<Integer>> entry : metaDataColumns.entrySet()) {
			for (final int columnIndex : entry.getValue()) {
				final Cell cell = row.getCell(columnIndex);
				if (cell == null) {
					continue;
				}
				final String value = XlsSource.getStringValueFromCell(cell);
				if (value != null && !value.isBlank()) {
					String context = "requisition";
					String key = entry.getKey();
					final int index = entry.getKey().indexOf(":");
					if (index != -1) {
						context = entry.getKey().substring(0, index);
						key = entry.getKey().substring(index + 1);
					}
					metaData.add(new MetaData(context, key, value));
				}
			}
		}
		return metaData;
	}

	private RequisitionInterface getInterfaceByRow(Row row) throws InvalidInterfaceException {
		RequisitionInterface reqInterface = new RequisitionInterface();
		String ip = XlsSource
				.getStringValueFromCell(getRelevantColumnID(row, REQUIRED_UNIQUE_PREFIXES.PREFIX_IP_ADDRESS));
		if (ip == null) {
			throw new InvalidInterfaceException("Null IP-Address for node '"
					+ getRelevantColumnID(row, REQUIRED_UNIQUE_PREFIXES.PREFIX_NODE).getStringCellValue().trim()
					+ "' at row '" + row.getRowNum(), null);
		}
		try {
			reqInterface.setIpAddr(ip.trim());
		} catch (IllegalArgumentException ex) {
			throw new InvalidInterfaceException("Invalid IP-Address for node '"
					+ getRelevantColumnID(row, REQUIRED_UNIQUE_PREFIXES.PREFIX_NODE).getStringCellValue().trim()
					+ "' at row '" + row.getRowNum() + "' and IP '" + ip.trim() + "'", ex);
		}

		String interfaceType = XlsSource.getStringValueFromCell(
				getRelevantColumnID(row, REQUIRED_UNIQUE_PREFIXES.PREFIX_INTERFACE_MANGEMENT_TYPE)).trim();
		if (interfaceType.equalsIgnoreCase(INTERFACE_TYPE_PRIMARY)) {
			reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
		} else if (interfaceType.equalsIgnoreCase(INTERFACE_TYPE_SECONDARY)) {
			reqInterface.setSnmpPrimary(PrimaryType.SECONDARY);
		} else {
			reqInterface.setSnmpPrimary(PrimaryType.NOT_ELIGIBLE);
		}

		Cell cell = getRelevantColumnID(row, OPTIONAL_UNIQUE_HEADERS.PREFIX_INTERFACE_STATUS);
		if (cell != null) {
			Integer value = XlsSource.getIntValueFromCell(cell);
			if (value != null) {
				reqInterface.setStatus(value);
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

		PREFIX_NODE("Node_"), PREFIX_IP_ADDRESS("IP_"), PREFIX_INTERFACE_MANGEMENT_TYPE("MgmtType_");

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
		PREFIX_INTERFACE_STATUS("InterfaceStatus"), PREFIX_FOREIGN_ID("ID_"), PREFIX_LOCATION("Location"),
		PREFIX_PARENT_FOREIGN_SOURCE("Parent_Foreign_Source"), PREFIX_PARENT_FOREIGN_ID("Parent_Foreign_Id"),
		PREFIX_PARENT_NODE_LABEL("Parent_Node_Label");

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
