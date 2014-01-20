package org.opennms.provisioner.xls.source;

import java.io.File;
import java.io.IOException;
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
    private final String ASSET_DESCRIPTION = "description";
    private final String instance;
    private final Configuration config;


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
                        if (!sheet.getCell(0, row).getContents().isEmpty()) {
                            node = new RequisitionNode();
                            node.setNodeLabel(sheet.getCell(0, row).getContents().trim());
                            node.setForeignId(sheet.getCell(0, row).getContents().trim());
                            if (!sheet.getCell(3, row).getContents().isEmpty()) {
                                node.getAssets().add(new RequisitionAsset(ASSET_DESCRIPTION, sheet.getCell(3, row).getContents().trim()));
                            }

                            Integer column = 5;
                            while (column < sheet.getRow(row).length) {
                                if (!sheet.getCell(column, row).getContents().isEmpty()) {
                                    node.getCategories().add(new RequisitionCategory(sheet.getCell(column, row).getContents().trim()));
                                }
                                column++;
                            }
                            requisition.getNodes().add(node);
                        }
                        reqInterface = new RequisitionInterface();
                        reqInterface.setIpAddr(sheet.getCell(1, row).getContents());
                        if (sheet.getCell(2, row).getContents().equalsIgnoreCase("P")) {
                            reqInterface.setSnmpPrimary(PrimaryType.PRIMARY);
                        } else if (sheet.getCell(2, row).getContents().equalsIgnoreCase("S")) {
                            reqInterface.setSnmpPrimary(PrimaryType.SECONDARY);
                        } else {
                            reqInterface.setSnmpPrimary(PrimaryType.NOT_ELIGIBLE);
                        }
                        //Add services to the interface
                        if (!sheet.getCell(4, row).getContents().isEmpty()) {
                            String[] forcedServices = sheet.getCell(4, row).getContents().trim().split(",");
                            for (String service : forcedServices) {
                                reqInterface.getMonitoredServices().add(new RequisitionMonitoredService(service.trim()));
                            }
                        }
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

    public String getXlsFile() {
        return this.config.getString("xls.file", null);
    }
}
