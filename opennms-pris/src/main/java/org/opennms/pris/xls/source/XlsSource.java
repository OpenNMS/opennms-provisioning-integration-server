package org.opennms.pris.xls.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionCategory;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.pris.ASSET_FIELD;
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
    }

    @Override
    public Object dump() {
        Requisition requisition = new Requisition(instance);
        if (getXlsFile() != null) {
            xls = new File(getXlsFile());
            if (xls.canRead()) {
                try {
                    Workbook workbook = Workbook.getWorkbook(xls);
                    Sheet sheet = workbook.getSheet(instance);
                    if (sheet == null) {
                        LOGGER.error("can not find sheet {} in workbook from file {}", instance, xls.getAbsolutePath());
                        throw new RuntimeException("reading sheet " + instance + " from " + xls.getAbsolutePath() + " failed. Dose the file contain a sheet called " + instance + "?");
                    }

                    requiredColumns = initializeRequiredColumns(sheet);
                    optionalMultiColumns = initializeOptionalMultiColumns(sheet);
                    assetColumns = initializeAssetColumns(sheet);

                    RequisitionNode node = new RequisitionNode();
                    RequisitionInterface reqInterface;
                    Integer row = 1;
                    while (row < sheet.getRows()) {
                        //TODO clean this if
                        if (!sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.getPrefix()), row).getContents().trim().isEmpty()) {
                            if (row.equals(1) || !sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.getPrefix()), row).getContents().trim().equalsIgnoreCase(sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.getPrefix()), row - 1).getContents().trim())) {
                                String nodeLabel = sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_NODE.getPrefix()), row).getContents().trim();
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

    private Map<String, Integer> initializeRequiredColumns(Sheet sheet) {
        Map<String, Integer> result = new HashMap<>();
        for (REQUIRED_PREFIXES prefix : REQUIRED_PREFIXES.values()) {
            Cell[] row = sheet.getRow(0);
            for (Cell cell : row) {
                if (cell.getContents().trim().toLowerCase().startsWith(prefix.getPrefix().toLowerCase())) {
                    result.put(prefix.getPrefix(), cell.getColumn());
                }
            }
        }
        return result;
    }

    private Map<String, List<Integer>> initializeOptionalMultiColumns(Sheet sheet) {
        Map<String, List<Integer>> result = new HashMap<>();
        for (OPTIONAL_PREFIXES prefix : OPTIONAL_PREFIXES.values()) {
            Cell[] row = sheet.getRow(0);
            for (Cell cell : row) {
                if (cell.getContents().trim().toLowerCase().startsWith(prefix.getPrefix().toLowerCase())) {
                    if (result.containsKey(prefix.getPrefix())) {
                        result.get(prefix.getPrefix()).add(cell.getColumn());
                    } else {
                        List<Integer> columnIds = new ArrayList<>();
                        columnIds.add(cell.getColumn());
                        result.put(prefix.getPrefix(), columnIds);
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Integer> initializeAssetColumns(Sheet sheet) {
        Map<String, Integer> result = new HashMap<>();
        for (ASSET_FIELD prefix : ASSET_FIELD.values()) {
            Cell[] row = sheet.getRow(0);
            for (Cell cell : row) {
                if (cell.getContents().trim().toLowerCase().startsWith(PREFIX_FOR_ASSETS.toLowerCase() + prefix.getFieldName().toLowerCase())) {
                    if (result.containsKey(prefix.getFieldName())) {
                        result.put(prefix.getFieldName(), cell.getColumn());
                    } else {
                        result.put(prefix.getFieldName(), cell.getColumn());
                    }
                }
            }
        }
        return result;
    }

    private Set<RequisitionCategory> getCategoriesByRow(Sheet sheet, Integer rowID) {
        Set<RequisitionCategory> categories = new TreeSet<>();
        List<Integer> relevantColumnIDs = getRelevantColumnIDs(OPTIONAL_PREFIXES.PREFIX_CATEGORY.getPrefix());
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
        List<Integer> relevantColumnIDs = getRelevantColumnIDs(OPTIONAL_PREFIXES.PREFIX_SERVICE.getPrefix());
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

    private RequisitionInterface getInterfaceByRow(Sheet sheet, Integer rowID) {
        RequisitionInterface reqInterface = new RequisitionInterface();

        reqInterface.setIpAddr(sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_IP_ADDRESS.getPrefix()), rowID).getContents().trim());
        String interfaceType = sheet.getCell(getRelevantColumnID(REQUIRED_PREFIXES.PREFIX_INTERFACE_TYPE.getPrefix()), rowID).getContents().trim();
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
        PREFIX_INTERFACE_TYPE("IfType_");

        private String prefix;

        private REQUIRED_PREFIXES(String prefix) {
            this.prefix = prefix;
        }

        private String getPrefix() {
            return prefix;
        }
    }

    private enum OPTIONAL_PREFIXES {

        PREFIX_CATEGORY("cat_"),
        PREFIX_SERVICE("svc_");

        private String prefix;

        private OPTIONAL_PREFIXES(String prefix) {
            this.prefix = prefix;
        }

        private String getPrefix() {
            return prefix;
        }
    }

}
