package org.opennms.provisioner.ocs.mapper;

import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.ocs.inventory.client.response.Computer;
import org.opennms.ocs.inventory.client.response.Computers;
import org.opennms.ocs.inventory.client.response.Network;
import org.opennms.provisioner.IpInterfaceHelper;
import org.opennms.provisioner.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultOcsComputerMapper implements Mapper {

  public static class Factory implements Mapper.Factory {

    @Override
    public Mapper create(final String instance,
                         final Configuration config) {
      return new DefaultOcsComputerMapper(instance, config);
    }
  }
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOcsComputerMapper.class);
  private final String instance;
  private final Configuration config;
  
  private final IpInterfaceHelper ipInterfaceHelper = new IpInterfaceHelper();

  public DefaultOcsComputerMapper(final String instance,
                                  final Configuration config) {
    this.instance = instance;
    this.config = config;
  }

  @Override
  public Requisition map(Object data, Requisition requisition) throws Exception {

    for (final Computer computer : ((Computers) data).getComputers()) {

      final RequisitionNode requisitionNode = mapComputerToRequisitionNode(computer);
      if (requisitionNode != null) {
        requisition.getNodes().add(requisitionNode);
      }
    }

    return requisition;
  }

  private RequisitionNode mapComputerToRequisitionNode(Computer computer) {
    final RequisitionNode requisitionNode = new RequisitionNode();
    requisitionNode.setForeignId(Integer.toString(computer.getHardware().getId()));
    requisitionNode.setNodeLabel(computer.getHardware().getName());

    final RequisitionInterface requisitionInterface = new RequisitionInterface();
    
    final Network managementNetwork = this.ipInterfaceHelper.selectManagementNetwork(computer);
    if (managementNetwork != null) {
      requisitionInterface.setIpAddr(managementNetwork.getIPAddress());
      requisitionInterface.setDescr(managementNetwork.getDescription());
      requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
      requisitionInterface.setStatus(1);
      requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
      requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
      requisitionNode.getInterfaces().add(requisitionInterface);
    } else {
        LOGGER.warn("computer '{}' named '{}' has no electable ip-address following the black- and whitelists.", computer.getHardware().getId(), computer.getHardware().getName());
    }
    requisitionNode.getAssets().add(new RequisitionAsset("operatingSystem", computer.getHardware().getOsname()));

    final String ocsComputerLink = "<a href=" + this.config.getString("ocs.url") + "/index.php?function=computer&head=1&systemid=" + requisitionNode.getForeignId() + ">OCS-Inventory</a>";
    requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsComputerLink));

    requisitionNode.getAssets().add(new RequisitionAsset("cpu", computer.getHardware().getProcessort()));

    return requisitionNode;
  }
}
