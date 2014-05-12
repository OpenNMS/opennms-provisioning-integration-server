/**
 * *****************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C)
 * 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 *
 * For more information contact: OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/ http://www.opennms.com/
 * *****************************************************************************
 */
package org.opennms.pris.xls.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionCategory;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.pris.AssetField;
import org.opennms.pris.Starter;
import org.opennms.pris.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(XlsSource.class);
    private final String instance;
    private final Configuration config;
    private final String WITHIN_SPLITTER = ",";

    private final String PREFIX_FOR_ASSETS = "Asset_";
    private final String INTERFACE_TYPE_PRIMARY = "P";
    private final String INTERFACE_TYPE_SECONDARY = "S";

    private Map<String, Integer> requiredColumns;
    private Map<String, List<Integer>> optionalMultiColumns;
    private Map<String, Integer> assetColumns;

    private final String ENCODING;
    private File xls = null;

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance, final Configuration config) {
            return new XlsSource(instance, config);
        }
    }

    public XlsSource(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
        this.ENCODING = config.getString("xls.encoding", "ISO-8859-1");
    }

    @Override
    public Object dump() throws MissingRequiredColumnHeaderException, Exception {
        Requisition requisition = new Requisition(instance);
        if (getXlsFile() != null) {
            xls = new File(getXlsFile());
            if (xls.canRead()) {
                try {
                    WorkbookSettings workbookSettings = new WorkbookSettings();
                    workbookSettings.setEncoding(ENCODING);
                    Workbook workbook = Workbook.getWorkbook(xls, workbookSettings);
                    List<String> sheetNames = Arrays.asList(workbook.getSheetNames());
                    if (!sheetNames.contains(instance)) {
                        LOGGER.error("can not find sheet {} in workbook from file {}", instance, xls.getAbsolutePath());
                        throw new RuntimeException("can not find sheet " + instance + " in workbook from file " + xls.getAbsolutePath());    
                    }
                    Sheet sheet = workbook.getSheet(instance);
                    if (sheet == null) {
                        LOGGER.error("can not read sheet {} in workbook from file {} check the configured encoding {}", instance, xls.getAbsolutePath(), ENCODING);
                        throw new RuntimeException("can not read sheet " + instance + " from file " + xls.getAbsolutePath() + " check the encoding " + ENCODING + ".");
                    }

                    requiredColumns = initializeRequiredColumns(sheet);
                    optionalMultiColumns = initializeOptionalMultiColumns(sheet);
                    assetColumns = initializeAssetColumns(sheet);

                    RequisitionNode node = new RequisitionNode();
                    RequisitionInterface reqInterface;
                    Integer row = 1;
                    while (row < sheet.getRows()) {
                        //TODO clean this if
                        if (!sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.PREFIX), row).getContents().trim().isEmpty()) {
                            if (row.equals(1) || !sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.PREFIX), row).getContents().trim().equalsIgnoreCase(sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.PREFIX), row - 1).getContents().trim())) {
                                String nodeLabel = sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.PREFIX), row).getContents().trim();
                                node = new RequisitionNode();
                                node.setNodeLabel(nodeLabel);
                                node.setForeignId(nodeLabel);
                                requisition.getNodes().add(node);
                            }
                            //adding categories
                            node.getCategories().addAll(getCategoriesByRow(sheet, row));

                            //adding assets
                            node.getAssets().addAll(getAssetsByRow(sheet, row));

                            //Add interface
                            reqInterface = getInterfaceByRow(sheet, row);

                            //Add services to the interface
                            reqInterface.getMonitoredServices().addAll(getServicesByRow(sheet, row));
                            node.getInterfaces().add(reqInterface);
                        }
                        row++;
                    }
                } catch (IOException ex) {
                    LOGGER.error("Problem working with file {}", xls.getAbsolutePath(), ex);
                } catch (BiffException ex) {
                    LOGGER.error("Problem working with xls file {}", xls.getAbsolutePath(), ex);
                }
            } else {
                throw new RuntimeException("can't read file" + xls.getAbsolutePath());
            }
        } else {
            throw new RuntimeException("no xls.file defined");
        }
        return requisition;
    }

    private Integer getRelevantColumnID(String prefix) {
        return requiredColumns.get(prefix);
    }

    private List<Integer> getRelevantColumnIDs(String prefix) {
        return optionalMultiColumns.get(prefix);
    }

    private Map<String, Integer> initializeRequiredColumns(Sheet sheet) throws MissingRequiredColumnHeaderException {
        Map<String, Integer> result = new HashMap<>();
        for (REQUIRED_PREFIXES prefix : REQUIRED_PREFIXES.values()) {
            Cell[] row = sheet.getRow(0);
            for (Cell cell : row) {
                if (cell.getContents().trim().toLowerCase().startsWith(prefix.PREFIX.toLowerCase())) {
                    result.put(prefix.PREFIX, cell.getColumn());
                }
            }
            if (!result.containsKey(prefix.PREFIX)) {
                throw new MissingRequiredColumnHeaderException(prefix.PREFIX);
            }
        }
        return result;
    }

    private Map<String, List<Integer>> initializeOptionalMultiColumns(Sheet sheet) {
        Map<String, List<Integer>> result = new HashMap<>();
        for (OPTIONAL_PREFIXES prefix : OPTIONAL_PREFIXES.values()) {
            Cell[] row = sheet.getRow(0);
            for (Cell cell : row) {
                if (cell.getContents().trim().toLowerCase().startsWith(prefix.PREFIX.toLowerCase())) {
                    if (result.containsKey(prefix.PREFIX)) {
                        result.get(prefix.PREFIX).add(cell.getColumn());
                    } else {
                        List<Integer> columnIds = new ArrayList<>();
                        columnIds.add(cell.getColumn());
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
            Cell[] row = sheet.getRow(0);
            for (Cell cell : row) {
                if (cell.getContents().trim().toLowerCase().equalsIgnoreCase(PREFIX_FOR_ASSETS + prefix.FIELD_NAME)) {
                    if (result.containsKey(prefix.FIELD_NAME)) {
                        result.put(prefix.FIELD_NAME, cell.getColumn());
                    } else {
                        result.put(prefix.FIELD_NAME, cell.getColumn());
                    }
                }
            }
        }
        return result;
    }

    private Set<RequisitionCategory> getCategoriesByRow(Sheet sheet, Integer rowID) {
        Set<RequisitionCategory> categories = new TreeSet<>();
        List<Integer> relevantColumnIDs = getRelevantColumnIDs(OPTIONAL_PREFIXES.PREFIX_CATEGORY.PREFIX);
        if (relevantColumnIDs != null) {
            for (Integer column : relevantColumnIDs) {
                String rawCategories = sheet.getCell(column, rowID).getContents().trim();
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

    private Set<RequisitionMonitoredService> getServicesByRow(Sheet sheet, Integer rowID) {
        Set<RequisitionMonitoredService> services = new TreeSet<>();
        List<Integer> relevantColumnIDs = getRelevantColumnIDs(OPTIONAL_PREFIXES.PREFIX_SERVICE.PREFIX);
        if (relevantColumnIDs != null) {
            for (Integer column : relevantColumnIDs) {
                String rawServices = sheet.getCell(column, rowID).getContents().trim();
                for (String service : rawServices.split(WITHIN_SPLITTER)) {
                    service = service.trim();
                    if (!service.isEmpty()) {
                        services.add(new RequisitionMonitoredService(service));
                    }
                }
            }
        }
        return services;
    }

    private Set<RequisitionAsset> getAssetsByRow(Sheet sheet, Integer rowID) {
        Set<RequisitionAsset> assets = new TreeSet<>();
        for (Map.Entry<String, Integer> entry : assetColumns.entrySet()) {
            String value = sheet.getCell(entry.getValue(), rowID).getContents().trim();
            if (!value.isEmpty()) {
                assets.add(new RequisitionAsset(entry.getKey(), value));
            }
        }
        return assets;
    }

    private RequisitionInterface getInterfaceByRow(Sheet sheet, Integer rowID) throws InvalidInterfaceException {
        RequisitionInterface reqInterface = new RequisitionInterface();
        try {
            reqInterface.setIpAddr(sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_IP_ADDRESS.PREFIX), rowID).getContents().trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidInterfaceException("Invalid IP-Address for node '" + sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.PREFIX), rowID).getContents().trim() + "' at row '" + rowID + "' and IP '" + sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_IP_ADDRESS.PREFIX), rowID).getContents().trim() + "'" , ex);
        }
        String interfaceType = sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_INTERFACE_MANGEMENT_TYPE.PREFIX), rowID).getContents().trim();
        if (interfaceType.equalsIgnoreCase(INTERFACE_TYPE_PRIMARY)) {
            reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        } else if (interfaceType.equalsIgnoreCase(INTERFACE_TYPE_SECONDARY)) {
            reqInterface.setSnmpPrimary(PrimaryType.SECONDARY);
        } else {
            reqInterface.setSnmpPrimary(PrimaryType.NOT_ELIGIBLE);
        }
        return reqInterface;
    }

    public String getXlsFile() {
        if (xls == null) {
            Path xlsFilePath = Starter.getConfigManager().getInstancePath(this.instance).resolve(this.config.getString("xls.file", null));
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

    private enum REQUIRED_PREFIXES {

        PREFIX_NODE("Node_"),
        PREFIX_IP_ADDRESS("IP_"),
        PREFIX_INTERFACE_MANGEMENT_TYPE("MgmtType_");

        private final String PREFIX;

        private REQUIRED_PREFIXES(String prefix) {
            this.PREFIX = prefix;
        }
    }

    private enum OPTIONAL_PREFIXES {

        PREFIX_CATEGORY("cat_"),
        PREFIX_SERVICE("svc_");

        private final String PREFIX;

        private OPTIONAL_PREFIXES(String prefix) {
            this.PREFIX = prefix;
        }
    }
}
