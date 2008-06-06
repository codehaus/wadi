import org.codehaus.wadi.servicespace.admin.commands.CountingGlobalEnvelopeCommand

class MonitorGlobalController extends AbstractAdminServiceSpaceController {

    def index = {
        redirect(action:monitor)
    }

    def monitor = {
        def countingInfos = executeCommand(new CountingGlobalEnvelopeCommand())
        ['countingInfos' : countingInfos]
    }
    
    def monitorJSON = {
        def tmpCountingInfos = executeCommand(new CountingGlobalEnvelopeCommand())
        render(contentType:"text/json") {
            countingInfos {
                for(tmpCountingInfo in tmpCountingInfos) {
                    countingInfo(hostingPeerName: tmpCountingInfo.hostingPeer.name,
                        inboundEnvelopeCpt: tmpCountingInfo.inboundEnvelopeCpt,
                        outboundEnvelopeCpt: tmpCountingInfo.outboundEnvelopeCpt)
                }
            }    
        }
    }

}

