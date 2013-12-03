package org.opennms.provisioner.vmware.mapper;

import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.opennms.provisioner.IpInterfaceHelper;
import org.opennms.provisioner.mapper.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVmwareMapper implements Mapper {

    public static class Factory implements Mapper.Factory {

        @Override
        public Mapper create(final String instance,
                final Configuration config) {
            return new DefaultVmwareMapper(instance, config);
        }
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVmwareMapper.class);
    private final String instance;
    private final Configuration config;

    private final IpInterfaceHelper ipInterfaceHelper = new IpInterfaceHelper();

    public DefaultVmwareMapper(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Requisition map(Object data) throws Exception {
        final Requisition requisition = new Requisition(instance);
        //DO THE MAGIC
        return requisition;
    }
}
