package org.opennms.provisioner.ocs.mapper;

import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevice;
import org.opennms.ocs.inventory.client.response.snmp.SnmpDevices;

public class DefaultOcsSnmpDevicesMapper implements Mapper {

  public static class Factory implements Mapper.Factory {

    @Override
    public Mapper create(final String instance,
                         final Configuration config) {
      return new DefaultOcsSnmpDevicesMapper(instance, config);
    }
  }
  
  private final String instance;
  private final Configuration config;

  public DefaultOcsSnmpDevicesMapper(final String instance,
                                     final Configuration config) {
    this.instance = instance;
    this.config = config;
  }

  @Override
  public Requisition map(Object data) throws Exception {
    final Requisition requisition = new Requisition();

    for (final SnmpDevice snmpDevice : ((SnmpDevices) data).getSNMPDevices()) {
      final RequisitionNode requisitionNode = mapSnmpDeviceToRequisitionNode(snmpDevice);
      if (requisitionNode != null) {
        requisition.getNodes().add(requisitionNode);
      }
    }

    return requisition;
  }

  private RequisitionNode mapSnmpDeviceToRequisitionNode(SnmpDevice snmpDevice) {
    final RequisitionNode requisitionNode = new RequisitionNode();
    requisitionNode.setForeignId(snmpDevice.getSNMP().getId() + "");
    requisitionNode.setNodeLabel(snmpDevice.getSNMP().getName());

    final RequisitionInterface requisitionInterface = new RequisitionInterface();
    requisitionInterface.setIpAddr(snmpDevice.getSNMP().getIPAddr());
    requisitionInterface.setDescr("OCS");
    requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
    requisitionInterface.setManaged(Boolean.TRUE);
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
    requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));

    final String ocsSnmpDeviceLink = "<a href=" + this.config.getString("ocs.url") + "/index.php?function=snmp_detail&head=1&id=" + requisitionNode.getForeignId() + ">OCS-Inventory</a>";
    requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsSnmpDeviceLink));

    requisitionNode.getInterfaces().add(requisitionInterface);

    return requisitionNode;
  }
}
