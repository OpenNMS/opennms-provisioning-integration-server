/**
* This script persists the amount of nodes delivered for the last request into a previousSize.txt file next to the requisition.properties.
**/

import org.slf4j.Logger
import org.opennms.pris.model.Requisition
import javax.xml.bind.JAXBContext
import java.io.StringWriter

logger.info("starting persistRequisitionSize.groovy")

JAXBContext jc = JAXBContext.newInstance(Requisition)
StringWriter sw = new StringWriter()
jc.createMarshaller().marshal(requisition, sw)

String xml = sw.toString()

File file = new File("requisitions" + File.separator  + requisition.getForeignSource() + File.separator + "previousSize.txt")
file.write requisition.getNodes().size() + ""
logger.info("persisted a requisition size of {}", requisition.getNodes().size())
logger.info("done with persistRequisitionSize.groovy")

return requisition 
