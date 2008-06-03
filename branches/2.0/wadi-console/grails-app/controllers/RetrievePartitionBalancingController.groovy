import org.codehaus.wadi.servicespace.admin.commands.GetPartitionBalancingInfos
import org.codehaus.wadi.servicespace.ServiceSpaceName

class RetrievePartitionBalancingController extends AbstractTargetServiceSpaceController {

	def getPartitionBalancing = {
	    def serviceSpaceName = params['serviceSpaceName']
	    if (processServiceSpaceName(serviceSpaceName)) {
	        def serviceSpaceNameObj = new ServiceSpaceName(new URI(serviceSpaceName))
	        def partitionBalancingInfo = executeCommand(new GetPartitionBalancingInfos(serviceSpaceNameObj))
		    render(template:'getPartitionBalancing', model:['partitionBalancingInfo' : partitionBalancingInfo])
	    } else {
	     	render '<div class="error">No ServiceSpaceName selected</div>'   
	    }
	}
	
}

