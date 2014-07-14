import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionAsset
import org.opennms.pris.model.RequisitionNode
import org.opennms.pris.model.RequisitionCategory

final String ASSET_LATITUDE = "latitude"
final String ASSET_LONGITUDE = "longitude"

Requisition requisition = data
requisition.setForeignSource(instance)
for (RequisitionNode node : requisition.getNodes()) {
    node.setBuilding("")
}

for (RequisitionNode node : requisition.getNodes()) {
    for (RequisitionCategory category : node.getCategories()) {
        
        if (category.getName().equals("USA")) {
            node.getAssets().add(new RequisitionAsset(ASSET_LATITUDE, "35.71736"))
            node.getAssets().add(new RequisitionAsset(ASSET_LONGITUDE, "-79.16181"))
        }
        if (category.getName().equals("GERMANY")) {
            node.getAssets().add(new RequisitionAsset(ASSET_LATITUDE, "50.555451"))
            node.getAssets().add(new RequisitionAsset(ASSET_LONGITUDE, "9.678587"))
        }
        if (category.getName().equals("ITALY")) {
            node.getAssets().add(new RequisitionAsset(ASSET_LATITUDE, "46.894884"))
            node.getAssets().add(new RequisitionAsset(ASSET_LONGITUDE, "11.435673"))
        }
    }
}

logger.info("Requisition: {}", requisition)

return requisition

