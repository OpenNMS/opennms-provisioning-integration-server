/**
* This script provides backwards compatibility with OpenNMS 1.12 in regards to Assets.
**/

import org.opennms.pris.model.Requisition
import org.opennms.pris.model.RequisitionNode
import org.opennms.pris.model.RequisitionAsset
import org.opennms.pris.util.RequisitionUtils
import org.opennms.pris.model.AssetField_1_12
import org.opennms.pris.util.AssetUtils

logger.info("starting OpenNMS_Assets_1_12.groovy")

List<RequisitionAsset> assetsToRemove = new ArrayList<>();

for (RequisitionNode node : requisition.getNodes()) {
   for (RequisitionAsset asset : node.getAssets()) {
       if (asset.getName().equalsIgnoreCase("managedObjectInstance") || asset.getName().equalsIgnoreCase("managedObjectType") ) {
           assetsToRemove.add(asset);
           logger.info("Remove from node '{}' the asset '{}' with the value '{}'", node.getNodeLabel(), asset.getName(), asset.getValue());
       } else {
           for (AssetField_1_12 assetField : AssetField_1_12.values()) {
               if (asset.getName().equalsIgnoreCase(assetField.name())) {
                   String assetValue_1_12 = AssetUtils.assetStringCleaner(asset.getValue(), assetField.maxLength);
                   if (!assetValue_1_12.equals(asset.getValue())) {
                       logger.info("For node '{}' asset '{}' was changed from '{}' to '{}'" , node.getNodeLabel(), asset.getName(), asset.getValue(), assetValue_1_12)
                       asset.setValue(assetValue_1_12);
                   }
                   break;
               }
           }
      }
  }
  node.getAssets().removeAll(assetsToRemove);
  assetsToRemove = new ArrayList<>();
}
logger.info("done with OpenNMS_Assets_1_12.groovy")
return requisition
