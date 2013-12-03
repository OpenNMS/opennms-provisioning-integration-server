package org.opennms.provisioner.mapper;

import org.apache.commons.configuration.Configuration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;

public class NullMapper implements Mapper {

    public static class Factory implements Mapper.Factory {

        @Override
        public Mapper create(final String instance,
                final Configuration config) {
            return new NullMapper(instance, config);
        }
    }

    private final String instance;
    private final Configuration config;

    public NullMapper(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Requisition map(Object data, Requisition requisition) throws Exception {
        return requisition;
    }

}
