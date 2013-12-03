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
import org.opennms.provisioner.IpInterfaceHelper;
import org.opennms.provisioner.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultOcsSnmpDevicesMapper implements Mapper {

  public static class Factory implements Mapper.Factory {

    @Override
    public Mapper create(final String instance,
                         final Configuration config) {
      return new DefaultOcsSnmpDevicesMapper(instance, config);
    }
  }
  
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOcsSnmpDevicesMapper.class);
  private final String instance;
  private final Configuration config;
  private final IpInterfaceHelper ipInterfaceHelper = new IpInterfaceHelper();

  public DefaultOcsSnmpDevicesMapper(final String instance,
                                     final Configuration config) {
    this.instance = instance;
    this.config = config;
  }

  @Override
  public Requisition map(Object data, Requisition requisition) throws Exception {

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

    String ipAddress = this.ipInterfaceHelper.selectIpAddress(snmpDevice);
    if(ipAddress != null) {
        final RequisitionInterface requisitionInterface = new RequisitionInterface();
        requisitionInterface.setIpAddr(ipAddress);
        requisitionInterface.setDescr("OCS");
        requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
        requisitionInterface.setStatus(1);
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("SNMP"));
        requisitionInterface.insertMonitoredService(new RequisitionMonitoredService("ICMP"));
        requisitionNode.getInterfaces().add(requisitionInterface);
    } else {
        LOGGER.warn("snmpDevice '{}' named '{}' whith ipAddress '{}', is not valid follworing black- and whitelists.", snmpDevice.getSNMP().getId(), snmpDevice.getSNMP().getName(), snmpDevice.getSNMP().getIPAddr());
    }
    final String ocsSnmpDeviceLink = "<a href=" + this.config.getString("ocs.url") + "/index.php?function=snmp_detail&head=1&id=" + requisitionNode.getForeignId() + ">OCS-Inventory</a>";
    requisitionNode.getAssets().add(new RequisitionAsset("comment", ocsSnmpDeviceLink));


    return requisitionNode;
  }
}
