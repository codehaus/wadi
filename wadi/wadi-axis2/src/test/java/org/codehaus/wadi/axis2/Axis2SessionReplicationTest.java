package org.codehaus.wadi.axis2;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.Constants;

public class Axis2SessionReplicationTest extends TestCase{
	
	private String repo;
	private int port1 = 10010;
	private int port2 = 10020;
	private long replication_interval = 10*1000; // we have to tweak this a bit
	private String axis2File; 
	private Axis2TestBuilder testBuilder;
	
	public Axis2SessionReplicationTest(){
		super();
		testBuilder = new Axis2TestBuilder();
	}	
	
	/** 
	 * create a single Axis2 instance
	 * 
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		repo = System.getProperty("axis2.repo");
			
		axis2File = repo + "/server/axis2.xml";
		
		// setup two axis2 instances
		testBuilder.startUpAxis2Instance(repo,axis2File,port1);
		testBuilder.startUpAxis2Instance(repo,axis2File,port2);
		System.setProperty("axis2.xml",repo + "/client/axis2.xml");
	}
	
	protected void tearDown() throws Exception {
		testBuilder.stopAxis2Instances();
	}
	
	public void testSessionReplication() throws Exception {
		Map eprParams = new HashMap();

		SOAPEnvelope request = testBuilder.getLoginRequest("12345", "911");
		SOAPEnvelope reply = testBuilder.send(testBuilder.authServiceUrl, eprParams, request);
		
		String response = testBuilder.retriveAuthStatus(reply);
		assertTrue("Session creation was unsuccessful",response.startsWith("Success"));
		
		String sessionId = testBuilder.retriveSessionId(eprParams);
		assertNotNull("Session id is null",sessionId);
		
		// sleep for a minute
		Thread.sleep(replication_interval);
		
		//removing the serviceGrpId
		// a proper axis2 version should be able to ignore this
		QName serviceGroupIdHeader = new QName(Constants.AXIS2_NAMESPACE_URI,"ServiceGroupId");
		eprParams.remove(serviceGroupIdHeader);
		
		// now do a balance querry with second axis2 instance
		// it should return 10,000 as the users balance
		SOAPEnvelope balRequest = testBuilder.getBalanceRequest();
		SOAPEnvelope balReply = testBuilder.send(testBuilder.accountServiceUrl, eprParams, balRequest);
		String	balance = testBuilder.retriveBalance(balReply);
		
		assertEquals("Session wasn't replicated properly","10,000",balance);
	}	
	
	public static void main(String[] args){
		Axis2SessionReplicationTest t = new Axis2SessionReplicationTest();
		try {
			t.setUp();
			t.testSessionReplication();
			t.tearDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
