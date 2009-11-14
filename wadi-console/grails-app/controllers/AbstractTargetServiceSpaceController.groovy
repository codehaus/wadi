import org.codehaus.wadi.servicespace.admin.commands.GetServiceSpaceInfos
import org.codehaus.wadi.servicespace.admin.AdminServiceSpace
import org.codehaus.wadi.servicespace.admin.AdminServiceSpaceHelper

abstract class AbstractTargetServiceSpaceController extends AbstractAdminServiceSpaceController {
    def PLEASE_SELECT_VALUE = '-- Please Select a Service Space --'
    
	def index = {
	    redirect(action:listServiceSpaceNames)
	}
    
	def listServiceSpaceNames = {
        def serviceSpaceInfos = executeCommand(new GetServiceSpaceInfos())

        def serviceSpaceNames = new LinkedHashSet()
        serviceSpaceNames.add(PLEASE_SELECT_VALUE)
        serviceSpaceInfos.each() {
            def serviceSpaceName = it.getServiceSpaceName()
            if (serviceSpaceName != AdminServiceSpace.NAME) {
	            serviceSpaceNames.add(it.getServiceSpaceName())
            }
        }
        
        ["serviceSpaceNames" : serviceSpaceNames]
	}

    def processServiceSpaceName(value) {
        null != value && PLEASE_SELECT_VALUE != value
    }
    
}

