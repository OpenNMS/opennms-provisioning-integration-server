/**
* This sample script step demonstrates all objects provided by the pris runtime.
* The objects are casted to there exact type and logged.
**/

import java.nio.file.Path
import org.slf4j.Logger
import org.opennms.pris.model.Requisition
import org.opennms.pris.util.InterfaceUtils
import org.opennms.pris.config.InstanceApacheConfiguration

logger.info("starting Sample.groovy")

logger.debug("script '{}'", ((Path)script))

logger.debug("data '{}'", ((Object)data))

logger.debug("requisition '{}'", ((Requisition)requisition))

logger.debug("logger '{}'", ((Logger)logger))

logger.debug("config '{}'", ((InstanceApacheConfiguration)config))

logger.debug("config '{}'", ((InterfaceUtils)interfaceUtils))

logger.info("done with Sample.groovy")

return requisition
