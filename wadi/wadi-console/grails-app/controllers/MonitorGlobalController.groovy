import org.codehaus.wadi.servicespace.admin.commands.CountingGlobalEnvelopeCommand

class MonitorGlobalController extends AbstractAdminServiceSpaceController {

	def index = {
	    redirect(action:monitor)
	}

	def monitor = {
        def countingInfos = executeCommand(new CountingGlobalEnvelopeCommand())
	    ['countingInfos' : countingInfos]
	}
	
}

