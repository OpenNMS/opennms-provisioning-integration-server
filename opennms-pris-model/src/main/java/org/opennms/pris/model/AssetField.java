package org.opennms.pris.model;

public enum AssetField {

    additionalhardware("additionalhardware", "Additional hardware", "String", 64),
    address1("address1", "Address 1", "String", 256),
    address2("address2", "Address 2", "String", 256),
    admin("admin", "Admin", "String", 32),
    assetNumber("assetNumber", "Asset Number", "String", 64),
    autoenable("autoenable", "AutoEnable", "String", 1),
    building("building", "Building", "String", 64),
    category("category", "Category", "String", 64),
    circuitId("circuitId", "Circuit ID", "String", 64),
    city("city", "City", "String", 64),
    comment("comment", "Comment", "String", 0),
    connection("connection", "Connection", "String", 32),
    country("country", "Country", "String", 32),
    cpu("cpu", "CPU", "String", 64),
    dateInstalled("dateInstalled", "Date Installed", "Date", 64),
    department("department", "Department", "String", 64),
    description("description", "Description", "String", 128),
    displayCategory("displayCategory", "Display Category", "String", 64),
    division("division", "Division", "String", 64),
    enable("enable", "Enable Password", "String", 32),
    floor("floor", "Floor", "String", 64),
    hdd1("hdd1", "HDD 1", "String", 64),
    hdd2("hdd2", "HDD 2", "String", 64),
    hdd3("hdd3", "HDD 3", "String", 64),
    hdd4("hdd4", "HDD 4", "String", 64),
    hdd5("hdd5", "HDD 5", "String", 64),
    hdd6("hdd6", "HDD 6", "String", 64),
    inputpower("inputpower", "Inputpower", "String", 11),
    latitude("latitude", "Latitude", "String", 0),
    lease("lease", "Lease", "String", 64),
    leaseExpires("leaseExpires", "Lease Expires", "Date", 64),
    longitude("longitude", "Longitude", "String", 0),
    maintContractExpiration("maintContractExpiration", "Contract Expires", "Date", 64),
    maintContractNumber("maintContractNumber", "Maint Contract Number", "String", 64),
    manufacturer("manufacturer", "Manufacturer", "String", 64),
    modelNumber("modelNumber", "Model Number", "String", 64),
    notifyCategory("notifyCategory", "Notification Category", "String", 64),
    numpowersupplies("numpowersupplies", "Number of power supplies", "Integer", 1),
    operatingSystem("operatingSystem", "Operating System", "String", 64),
    password("password", "Password", "String", 32),
    pollerCategory("pollerCategory", "Poller Category", "String", 64),
    port("port", "Port", "String", 64),
    rack("rack", "Rack", "String", 64),
    rackunitheight("rackunitheight", "Rack unit height", "Integer", 2),
    ram("ram", "RAM", "String", 10),
    region("region", "Region", "String", 64),
    room("room", "Room", "String", 64),
    serialNumber("serialNumber", "Serial Number", "String", 64),
    slot("slot", "Slot", "String", 64),
    snmpcommunity("snmpcommunity", "SNMP community", "String", 32),
    state("state", "State", "String", 64),
    storagectrl("storagectrl", "Storage Controller", "String", 64),
    supportPhone("supportPhone", "Maint Phone", "String", 64),
    thresholdCategory("thresholdCategory", "Threshold Category", "String", 64),
    username("username", "Username", "String", 32),
    vendor("vendor", "Vendor", "String", 64),
    vendorAssetNumber("vendorAssetNumber", "Vendor Asset", "String", 64),
    vendorFax("vendorFax", "Vendor Fax", "String", 64),
    vendorPhone("vendorPhone", "Phone", "String", 64),
    vmwareManagedEntityType("vmwareManagedEntityType", "VMware managed entity type", "String", 70),
    vmwareManagedObjectId("vmwareManagedObjectId", "VMware managed object ID", "String", 70),
    vmwareManagementServer("vmwareManagementServer", "VMware management server", "String", 70),
    vmwareState("vmwareState", "VMware state", "String", 255),
    vmwareTopologyInfo("vmwareTopologyInfo", "VMware Topology Info", "String", 0),
    zip("zip", "", "String", 64);
    
    /**
     * Name of the AssetFiled in the system.
     * 
     * Used in the database and in requisitions.
     */
    public final String name;
    
    /**
     * Nice name for the AssetFiled to display in the webUI for example.
     */
    public final String displayName;
    
    //TODO Class<?> vs String?
    /**
     * Type of the AssetFiled.
     * 
     * Like {@literal String} or {@literal Integer}.
     */
    public final String type;
    
    /**
     * The maximal length that the filed can hold.
     * 
     * Use {@literal 0} for unknown, {@literal -1} for unlimited length.
     */
    public final int maxLength;

    private AssetField(final String name,
                       final String displayName,
                       final String type,
                       final int maxLength) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.maxLength = maxLength;
    }
}