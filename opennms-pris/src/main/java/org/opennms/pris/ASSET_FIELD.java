package org.opennms.pris;

// Java naming convention? AssetField?
public enum ASSET_FIELD {

    additionalhardware("additionalhardware", "Additional hardware", "String", 0),
    address1("address1", "Address 1", "String", 0),
    address2("address2", "Address 2", "String", 0),
    admin("admin", "Admin", "String", 0),
    assetNumber("assetNumber", "Asset Number", "String", 0),
    autoenable("autoenable", "AutoEnable", "String", 0),
    building("building", "Building", "String", 0),
    category("category", "Category", "String", 0),
    circuitId("circuitId", "Circuit ID", "String", 0),
    city("city", "City", "String", 0),
    comment("comment", "Comment", "String", 0),
    connection("connection", "Connection", "String", 0),
    country("country", "Country", "String", 0),
    cpu("cpu", "CPU", "String", 0),
    dateInstalled("dateInstalled", "Date Installed", "Date", 0),
    department("department", "Department", "String", 0),
    description("description", "Description", "String", 0),
    displayCategory("displayCategory", "Display Category", "String", 0),
    division("division", "Division", "String", 0),
    enable("enable", "Enable Password", "String", 0),
    floor("floor", "Floor", "String", 0),
    hdd1("hdd1", "HDD 1", "String", 0),
    hdd2("hdd2", "HDD 2", "String", 0),
    hdd3("hdd3", "HDD 3", "String", 0),
    hdd4("hdd4", "HDD 4", "String", 0),
    hdd5("hdd5", "HDD 5", "String", 0),
    hdd6("hdd6", "HDD 6", "String", 0),
    inputpower("inputpower", "Inputpower", "String", 0),
    latitude("latitude", "Latitude", "String", 0),
    lease("lease", "Lease", "String", 0),
    leaseExpires("leaseExpires", "Lease Expires", "Date", 0),
    longitude("longitude", "Longitude", "String", 0),
    maintContractExpiration("maintContractExpiration", "Contract Expires", "Date", 0),
    maintContractNumber("maintContractNumber", "Maint Contract Number", "String", 0),
    manufacturer("manufacturer", "Manufacturer", "String", 0),
    modelNumber("modelNumber", "Model Number", "String", 0),
    notifyCategory("notifyCategory", "Notification Category", "String", 0),
    numpowersupplies("numpowersupplies", "Number of power supplies", "Integer", 1),
    operatingSystem("operatingSystem", "Operating System", "String", 0),
    password("password", "Password", "String", 0),
    pollerCategory("pollerCategory", "Poller Category", "String", 0),
    port("port", "Port", "String", 0),
    rack("rack", "Rack", "String", 0),
    rackunitheight("rackunitheight", "Rack unit height", "Integer", 0),
    ram("ram", "RAM", "String", 0),
    region("region", "Region", "String", 0),
    room("room", "Room", "String", 0),
    serialNumber("serialNumber", "Serial Number", "String", 0),
    slot("slot", "Slot", "String", 0),
    snmpcommunity("snmpcommunity", "SNMP community", "String", 0),
    state("state", "State", "String", 0),
    storagectrl("storagectrl", "Storage Controller", "String", 0),
    supportPhone("supportPhone", "Maint Phone", "String", 0),
    thresholdCategory("thresholdCategory", "Threshold Category", "String", 0),
    username("username", "Username", "String", 0),
    vendor("vendor", "Vendor", "String", 0),
    vendorAssetNumber("vendorAssetNumber", "Vendor Asset", "String", 0),
    vendorFax("vendorFax", "Vendor Fax", "String", 0),
    vendorPhone("vendorPhone", "Phone", "String", 0),
    vmwareManagedEntityType("vmwareManagedEntityType", "VMware managed entity type", "String", 0),
    vmwareManagedObjectId("vmwareManagedObjectId", "VMware managed object ID", "String", 0),
    vmwareManagementServer("vmwareManagementServer", "VMware management server", "String", 0),
    vmwareState("vmwareState", "VMware state", "String", 0),
    vmwareTopologyInfo("vmwareTopologyInfo", "VMware Topology Info", "String", 0),
    zip("zip", "", "String", 0);

    private final String fieldName;
    private final String fieldDisplayName;

    // can it be Class<?> or does it need to be String?
    private final String fieldType;

    // comment? max length is always 0, what does that mean, unlimited? If it is always 0, do I need it anyways?
    private final Integer maxLength;

    private ASSET_FIELD(String fieldName, String fieldDisplayName, String fieldType, Integer maxLength) {
        this.fieldName = fieldName;
        this.fieldDisplayName = fieldDisplayName;
        this.fieldType = fieldType;
        this.maxLength = maxLength;
    }

    // you do not need this, the same can be achieved with this.name()
    public String getFieldName() {
        return this.fieldName;
    }

    public String getFieldDisplayName() {
        return this.fieldDisplayName;
    }

    public String getFieldType() {
        return this.fieldType;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

}
