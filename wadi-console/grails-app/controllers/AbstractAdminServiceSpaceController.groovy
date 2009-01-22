import org.codehaus.wadi.servicespace.admin.commands.GetServiceSpaceInfos
import org.codehaus.wadi.servicespace.admin.AdminServiceSpace
import org.codehaus.wadi.servicespace.admin.AdminServiceSpaceHelper

abstract class AbstractAdminServiceSpaceController {
    
	def adminServiceSpaceHelper = new AdminServiceSpaceHelper()
	def dispatcherRegistry

	def executeCommand(command) {
	    def clusterName = session[SelectClusterController.SESSION_KEY_SELECTED_CLUSTER_NAME]
        def dispatcher = dispatcherRegistry.getDispatcherByClusterName(clusterName)
        def adminServiceSpace = adminServiceSpaceHelper.getAdminServiceSpace(dispatcher);
        def commandEndPoint = adminServiceSpace.getCommandEndPoint()
        commandEndPoint.execute(command)
	}

}

