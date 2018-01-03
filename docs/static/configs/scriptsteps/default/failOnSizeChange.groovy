/**
* This script persists the amount of nodes delivered for the last request into a previousSize.txt file next to the requisition.properties.
* To configure the script provide a script.sizeChangeAbs parameter in the requisition.properties.
**/

import org.slf4j.Logger
import org.opennms.pris.model.Requisition
import javax.xml.bind.JAXBContext
import java.io.StringWriter

logger.info("starting failOnSizeChange.groovy")
String fileName = "previousSize.txt"
String sizeChangeAbsParam = "sizeChangeAbs"
logger.info("Is '{}' set in config? '{}'", sizeChangeAbsParam ,config.containsKey(sizeChangeAbsParam))

if (config.containsKey(sizeChangeAbsParam)) {
	logger.info("fail on size change bigger than '{}'", config.getInt(sizeChangeAbsParam))
	int sizeChangeThreshold = config.getInt(sizeChangeAbsParam)
	File previousSizeFile = new File("requisitions" + File.separator + requisition.getForeignSource() + File.separator + fileName)
	if (previousSizeFile.exists() && previousSizeFile.canRead()) {
		int previousSize = previousSizeFile.getText('UTF-8').toInteger()
		logger.info("previousSize from file is '{}'. New size is '{}'", previousSize, requisition.getNodes().size())
		int sizeChange = (previousSize - requisition.getNodes().size()).abs()
		if (sizeChange  > sizeChangeThreshold) {
			throw new Exception("The requisition '" + requisition.getForeignSource()  + "' changed size by '" + sizeChange + "' nodes. The configured limit for size change is '" + sizeChangeThreshold + "' Failed on purpose.")
		}
	} else {
		logger.info("No " + fileName + " found. Deliver requisition.")	
		}
}

logger.info("done with failOnSizeChange.groovy")

return requisition 
