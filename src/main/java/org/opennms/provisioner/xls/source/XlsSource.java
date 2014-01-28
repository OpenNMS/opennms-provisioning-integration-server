package org.opennms.provisioner.xls.source;

import java.io.File;
import java.io.IOException;
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
import org.opennms.provisioner.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(XlsSource.class);
    private final String instance;
    private final Configuration config;
    private final String WITHIN_SPLITTER = ",";

    private final String PREFIX_NODE = "Node_";
    private final String PREFIX_CATEGORY = "cat_";
    private final String PREFIX_SERVICE = "svc_";
    private final String PREFIX_IP_ADDRESS = "IP_";
    private final String PREFIX_INTERFACE_TYPE = "IfType_";
    private final String PREFIX_ASSET_DESCRIPTION = "Asset_Description";

    private final String INTERFACE_TYPE_PRIMARY = "P";
    private final String INTERFACE_TYPE_SECONDARY = "S";
    private final String ASSET_DESCRIPTION = "description";

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
            File xls = new File(getXlsFile());
            if (xls.canRead()) {
                try {
                    Workbook workbook = Workbook.getWorkbook(xls);
                    Sheet sheet = workbook.getSheet(instance);
                    if (sheet == null) {
                        LOGGER.error("can not find sheet {} in workbook from file {}", instance, xls.getAbsolutePath());
                        throw new RuntimeException("reading sheet " + instance + " from " + xls.getAbsolutePath() + "failed");
                    }
                    RequisitionNode node = new RequisitionNode();
                    RequisitionInterface reqInterface;
                    Integer row = 1;
                    while (row < sheet.getRows()) {
                        //TODO clean this if
                        if (row.equals(1) || !sheet.getCell(0, row).getContents().trim().equalsIgnoreCase(sheet.getCell(0, row - 1).getContents().trim())) {
                            String nodeLabel = sheet.getCell(getRelevantColumnID(sheet, PREFIX_NODE), row).getContents().trim();
                            node = new RequisitionNode();
                            node.setNodeLabel(nodeLabel);
                            node.setForeignId(nodeLabel);
                            String assetDescription = sheet.getCell(getRelevantColumnID(sheet, PREFIX_ASSET_DESCRIPTION), row).getContents().trim();
                            if (!assetDescription.isEmpty()) {
                                node.getAssets().add(new RequisitionAsset(ASSET_DESCRIPTION, assetDescription));
                            }
                            //adding categories
                            node.getCategories().addAll(getCategoriesByRow(sheet, row));
                            requisition.getNodes().add(node);
                        }
                        //Add interface
                        reqInterface = getInterfaceByRow(sheet, row);

                        //Add services to the interface
                        reqInterface.getMonitoredServices().addAll(getServicesByRow(sheet, row));
                        node.getInterfaces().add(reqInterface);
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

    //TODO null protection
    private Integer getRelevantColumnID(Sheet sheet, String prefix) {
        Cell[] row = sheet.getRow(0);
        for (Cell cell : row) {
            if (cell.getContents().trim().toLowerCase().startsWith(prefix.toLowerCase())) {
                return (cell.getColumn());
            }
        }
        LOGGER.debug("NO Column in {} starting with {}", sheet.getName(), prefix);
        return null;
    }

    private Set<Integer> getRelevantColumnIDs(Sheet sheet, String prefix) {
        Set<Integer> relevantColumnIDs = new TreeSet<>();
        Cell[] row = sheet.getRow(0);
        for (Cell cell : row) {
            if (cell.getContents().trim().toLowerCase().startsWith(prefix.toLowerCase())) {
                relevantColumnIDs.add(cell.getColumn());
            }
        }
        LOGGER.debug("Found {} Columns for {} starting with {}", relevantColumnIDs.size(), sheet.getName(), prefix);
        return relevantColumnIDs;
    }

    private Set<RequisitionCategory> getCategoriesByRow(Sheet sheet, Integer rowID) {
        Set<RequisitionCategory> categories = new TreeSet<>();
        for (Integer column : getRelevantColumnIDs(sheet, PREFIX_CATEGORY)) {
            String rawCategories = sheet.getCell(column, rowID).getContents().trim();
            for (String category : rawCategories.split(WITHIN_SPLITTER)) {
                category = category.trim();
                if (!category.isEmpty()) {
                    categories.add(new RequisitionCategory(category));
                }
            }
        }
        return categories;
    }

    private Set<RequisitionMonitoredService> getServicesByRow(Sheet sheet, Integer rowID) {
        Set<RequisitionMonitoredService> services = new TreeSet<>();
        for (Integer column : getRelevantColumnIDs(sheet, PREFIX_SERVICE)) {
            String rawServices = sheet.getCell(column, rowID).getContents().trim();
            for (String service : rawServices.split(WITHIN_SPLITTER)) {
                service = service.trim();
                if (!service.isEmpty()) {
                    services.add(new RequisitionMonitoredService(service));
                }
            }
        }
        return services;
    }

    private RequisitionInterface getInterfaceByRow(Sheet sheet, Integer rowID) {
        RequisitionInterface reqInterface = new RequisitionInterface();

        reqInterface.setIpAddr(sheet.getCell(getRelevantColumnID(sheet, PREFIX_IP_ADDRESS), rowID).getContents().trim());
        String interfaceType = sheet.getCell(getRelevantColumnID(sheet, PREFIX_INTERFACE_TYPE), rowID).getContents().trim();
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
        return this.config.getString("xls.file", null);
    }
}