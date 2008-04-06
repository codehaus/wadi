import org.codehaus.wadi.servicespace.admin.commands.GetServiceSpaceInfos
import org.codehaus.wadi.servicespace.admin.AdminServiceSpace

class ListServiceSpaceController extends AbstractAdminServiceSpaceController {

	def index = {
	    redirect(action:list)
	}
	
	def list = {
        def serviceSpaceInfos = executeCommand(new GetServiceSpaceInfos())
        
        def nameToServiceSpaceInfos = [:]
        serviceSpaceInfos.each() {
            def serviceSpaceName = it.getServiceSpaceName()
            if (serviceSpaceName == AdminServiceSpace.NAME) {
            	return
            }
            def nameServiceSpaceInfos = nameToServiceSpaceInfos.get(serviceSpaceName)
            if (null == nameServiceSpaceInfos) {
                nameServiceSpaceInfos = []
                nameToServiceSpaceInfos.put(serviceSpaceName, nameServiceSpaceInfos)
            }
            nameServiceSpaceInfos.add(it)
        }
                                      
        ["nameToServiceSpaceInfos" : nameToServiceSpaceInfos]
	}
	
}

