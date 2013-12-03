package org.opennms.provisioner.vmware.source;

import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import java.net.URL;
import org.apache.commons.configuration.Configuration;
import org.opennms.provisioner.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VmwareSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmwareSource.class);

    private final String instance;
    private final Configuration config;

    private VmwareSource(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance,
                final Configuration config) {
            return new VmwareSource(instance, config);
        }
    }

    @Override
    public Object dump() throws Exception {

        ServiceInstance serviceInstance = new ServiceInstance(new URL(getUrl()), getUsername(), getPassword(), true);
        InventoryNavigator inventoryNavigator = new InventoryNavigator(serviceInstance.getRootFolder());
        ManagedEntity[] managedEntities = inventoryNavigator.searchManagedEntities(getType());
//        ManagedEntity[] searchManagedEntities = inventoryNavigator.searchManagedEntities("VirtualMachine");

        return managedEntities;
    }

    public final String getUrl() {
        return this.config.getString("vmware.url");
    }

    public final String getUsername() {
        return this.config.getString("vmware.username");
    }

    public final String getPassword() {
        return this.config.getString("vmware.password");
    }

    public final String getType() {
        return this.config.getString("vmware.type");
    }
}
