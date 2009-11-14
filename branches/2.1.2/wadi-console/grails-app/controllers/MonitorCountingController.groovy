import org.codehaus.wadi.servicespace.admin.commands.CountingServiceSpaceEnvelopeCommand
import org.codehaus.wadi.servicespace.ServiceSpaceName

class MonitorCountingController extends AbstractTargetServiceSpaceController {

    def monitor = {
        def serviceSpaceName = params['serviceSpaceName']
        if (processServiceSpaceName(serviceSpaceName)) {
            redirect(action:'monitor', id:serviceSpaceName)
            return
        }
        
        serviceSpaceName = params['id']
        if (serviceSpaceName) {
            def serviceSpaceNameObj = new ServiceSpaceName(new URI(serviceSpaceName))
            def countingInfos = executeCommand(new CountingServiceSpaceEnvelopeCommand(serviceSpaceNameObj))
            render(template:'monitor', model:['countingInfos' : countingInfos, 'serviceSpaceName': serviceSpaceName])
            return
        }

        flash.warningMessage = 'No ServiceSpaceName selected!'   
	    redirect(action:listServiceSpaceNames)
    }

    def monitorJSON = {
        def serviceSpaceName = params['id']
        if (processServiceSpaceName(serviceSpaceName)) {
            def serviceSpaceNameObj = new ServiceSpaceName(new URI(serviceSpaceName))
            def tmpCountingInfos = executeCommand(new CountingServiceSpaceEnvelopeCommand(serviceSpaceNameObj))
            render(contentType:"text/json") {
                countingInfos {
                    for(tmpCountingInfo in tmpCountingInfos) {
                        countingInfo(hostingPeerName: tmpCountingInfo.hostingPeer.name,
                            inboundEnvelopeCpt: tmpCountingInfo.inboundEnvelopeCpt,
                            outboundEnvelopeCpt: tmpCountingInfo.outboundEnvelopeCpt)
                    }
                }    
            }
        } else {
            render '<div class="error">No ServiceSpaceName selected</div>'   
        }
    }

}
