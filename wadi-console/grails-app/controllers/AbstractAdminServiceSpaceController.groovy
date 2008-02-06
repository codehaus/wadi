import org.codehaus.wadi.servicespace.admin.commands.GetServiceSpaceInfos
import org.codehaus.wadi.servicespace.admin.AdminServiceSpace
import org.codehaus.wadi.servicespace.admin.AdminServiceSpaceHelper

abstract class AbstractAdminServiceSpaceController {
    def PLEASE_SELECT_VALUE = '-- Please Select a Service Space --'
    
	def adminServiceSpaceHelper = new AdminServiceSpaceHelper()
	def dispatcherRegistry

    def beforeInterceptor = {
        if (null == session[SelectClusterController.SESSION_KEY_SELECTED_CLUSTER_NAME]) {
	        redirect(controller:'selectCluster', action:'list')
	        return false
        }
        return true
 	}
    
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

	def executeCommand(command) {
	    def clusterName = session[SelectClusterController.SESSION_KEY_SELECTED_CLUSTER_NAME]
        def dispatcher = dispatcherRegistry.getDispatcherByClusterName(clusterName)
        def adminServiceSpace = adminServiceSpaceHelper.getAdminServiceSpace(dispatcher);
        def commandEndPoint = adminServiceSpace.getCommandEndPoint()
        commandEndPoint.execute(command)
	}

    def processServiceSpaceName(value) {
        null != value && PLEASE_SELECT_VALUE != value
    }
    
}

