import org.codehaus.wadi.servicespace.admin.commands.GetSessionInfos
import org.codehaus.wadi.servicespace.ServiceSpaceName

class ListSessionController extends AbstractAdminServiceSpaceController {

	def listSession = {
	    retrieveSessions(params, session, 'listSession')
	}
	
	def listTable = {
	    retrieveSessions(params, session, 'listTable')
	}
	
	def retrieveSessions(params, session, templateName) {
	    def serviceSpaceName = params['serviceSpaceName']
  	    if (processServiceSpaceName(serviceSpaceName)) {
  	    	def serviceSpaceNameObj = new ServiceSpaceName(new URI(serviceSpaceName))
  	    	def sessionInfos = executeCommand(new GetSessionInfos(serviceSpaceNameObj))
  	    	def model = ['sessions' : sessionInfos, 'serviceSpaceName' : serviceSpaceName]
	        render(template:templateName, model:model)
  	    } else {
	     	render '<div class="error">No ServiceSpaceName selected</div>'   
  	    }
	}
	
}

