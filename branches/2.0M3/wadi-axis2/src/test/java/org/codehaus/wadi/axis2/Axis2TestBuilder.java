package org.codehaus.wadi.axis2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.wsdl.WSDLConstants;


public class Axis2TestBuilder{

	protected OMNamespace namespace;

	protected SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();

	protected BufferedReader reader = new BufferedReader(new InputStreamReader(
			System.in));

	protected Options options;

	protected String authServiceUrl = "http://localhost:10010/axis2/services/AuthService";
	
	protected String accountServiceUrl = "http://localhost:10020/axis2/services/AccountService";
	
	protected Vector axis2InstanceHolder = new Vector();

	public Axis2TestBuilder() {
		namespace = fac
				.createOMNamespace("http://axis2/test/namespace1", "ns1");
		options = new Options();
		options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		options.setUseSeparateListener(false);
	}
	
	protected void startUpAxis2Instance(String repo, String axis2File, int port) throws Exception{
		SimpleHTTPServer receiver = new SimpleHTTPServer(
		ConfigurationContextFactory.createConfigurationContextFromFileSystem(
		        repo, axis2File), port, null);
		receiver.start();
		axis2InstanceHolder.add(receiver);
	}
	
	protected void stopAxis2Instances() throws Exception {
		for(Iterator it = axis2InstanceHolder.iterator(); it.hasNext(); ){
			SimpleHTTPServer server = (SimpleHTTPServer)it.next();
			server.stop();
		}
		axis2InstanceHolder.removeAllElements();
	}	

	protected SOAPEnvelope getLoginRequest(String acctnum, String passwd) {
		SOAPEnvelope envelope = fac.getDefaultEnvelope();

		OMElement params = fac.createOMElement("login", namespace);
		OMElement param1OM = fac.createOMElement("acctNum", namespace);
		OMElement param2OM = fac.createOMElement("passwd", namespace);
		param1OM.setText(acctnum);
		param2OM.setText(passwd);
		params.addChild(param1OM);
		params.addChild(param2OM);
		envelope.getBody().setFirstChild(params);

		return envelope;
	}

	protected String retrieveAuthStatus(SOAPEnvelope reply) {
		if (reply != null) {
			OMElement resultOM = reply.getBody().getFirstChildWithName(
					new QName("STATUS_MESSAGE"));
			return resultOM.getText();
		} else
			return null;
	}
	
	protected String retrieveSessionId(Map params) {
		QName sessionIdHeader = new QName(Constants.AXIS2_NAMESPACE_URI,"sessionId");
		if (params != null) {
			OMElement resultOM = (OMElement)params.get(sessionIdHeader);
			return resultOM.getText();
		} else
			return null;
	}

    protected SOAPEnvelope getBalanceRequest(){
    	SOAPEnvelope envelope = fac.getDefaultEnvelope();
        OMElement params = fac.createOMElement("getBalance", namespace);
        envelope.getBody().setFirstChild(params);
        
        return envelope;
    }
    
    protected String retrieveBalance(SOAPEnvelope reply) {
    	if (reply != null) {
            try {
				OMElement resultOM = reply.getBody().getFirstChildWithName(new QName("BALANCE"));
				if (resultOM == null){
					return null;
				}else{
					return resultOM.getText();
				}
			} catch (OMException e) {
				return null;
			}
        } else
            return null;
    }
    
	protected SOAPEnvelope send(String url, Map eprParams, SOAPEnvelope env)
			throws AxisFault {
		EndpointReference targetEPR = new EndpointReference(url);
		targetEPR.setName("TO");
		targetEPR.setReferenceParameters(eprParams);

		ServiceClient serviceClient = new ServiceClient();
		options.setTo(targetEPR);
		options.setAction("ANY-URI");
		serviceClient.setOptions(options);

		serviceClient.engageModule(new QName("addressing"));
		
		MessageContext requetMessageContext = new MessageContext();
		requetMessageContext.setEnvelope(env);
				
		OperationClient opClient = serviceClient
				.createClient(ServiceClient.ANON_OUT_IN_OP);
		requetMessageContext.setTo(targetEPR);
		opClient.addMessageContext(requetMessageContext);
		opClient.setOptions(options);

		opClient.execute(true);

		MessageContext reply = opClient
				.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

		eprParams.putAll(reply.getReplyTo().getAllReferenceParameters());

		return reply.getEnvelope();
	}

}
