import org.codehaus.wadi.servicespace.admin.commands.CountingServiceSpaceEnvelopeCommand
import org.codehaus.wadi.servicespace.ServiceSpaceName

class MonitorCountingController extends AbstractTargetServiceSpaceController {

	def monitor = {
	    def serviceSpaceName = params['serviceSpaceName']
	    if (processServiceSpaceName(serviceSpaceName)) {
	        def serviceSpaceNameObj = new ServiceSpaceName(new URI(serviceSpaceName))
	        def countingInfos = executeCommand(new CountingServiceSpaceEnvelopeCommand(serviceSpaceNameObj))
		    render(template:'monitor', model:['countingInfos' : countingInfos])
	    } else {
	     	render '<div class="error">No ServiceSpaceName selected</div>'   
	    }
	}
	
}

