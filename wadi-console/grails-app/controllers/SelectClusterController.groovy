class SelectClusterController {
    public static def SESSION_KEY_SELECTED_CLUSTER_NAME = 'SELECTED_CLUSTER_NAME'
	def dispatcherRegistry

	def list = {
	    ['dispatchers': dispatcherRegistry.getDispatchers()]
	}
	
	def select = {
	    def clusterName = params['clusterName']
	    session[SESSION_KEY_SELECTED_CLUSTER_NAME] = clusterName
	}
    
	def unselect = {
	    session[SESSION_KEY_SELECTED_CLUSTER_NAME] = null
	}
}

