import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GAA

class ClusterSelectedFilters {
    def filters = {
        clusterSelected(controller:'*', action:'*') {
            before = {
                def controllerName = request.getAttribute(GAA.CONTROLLER_NAME_ATTRIBUTE)
                def actionName= request.getAttribute(GAA.ACTION_NAME_ATTRIBUTE)
                if ('selectCluster' == controllerName) {
                    return true
                }
                if (null == session[SelectClusterController.SESSION_KEY_SELECTED_CLUSTER_NAME]) {
                    flash.warningMessage = 'A cluster must be selected!'
        	        redirect(controller:'selectCluster', action:'list')
        	        return false
                }
                return true
            }
        }
    }
}