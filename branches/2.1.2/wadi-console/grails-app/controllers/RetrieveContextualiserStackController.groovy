import org.codehaus.wadi.servicespace.admin.commands.GetContextualiserInfoStack
import org.codehaus.wadi.servicespace.ServiceSpaceName

class RetrieveContextualiserStackController extends AbstractTargetServiceSpaceController {

	def getContextualiserStack = {
	    def serviceSpaceName = params['serviceSpaceName']
	    if (processServiceSpaceName(serviceSpaceName)) {
	        def serviceSpaceNameObj = new ServiceSpaceName(new URI(serviceSpaceName))
	        def contextualiserStack = executeCommand(new GetContextualiserInfoStack(serviceSpaceNameObj))
		    render(template:'contextualiserStack', model:['contextualiserStack' : contextualiserStack])
	    } else {
	     	render '<div class="error">No ServiceSpaceName selected</div>'   
	    }
	}

}

