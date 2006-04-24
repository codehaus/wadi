package org.codehaus.wadi.axis2;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.axiom.soap.SOAPEnvelope;

public class Axis2SessionCreationTest extends TestCase{
	
	private String repo = "./resources";
	private String axis2File = "./resources/axis2.xml";
	private int port = 10010;
	private Axis2TestBuilder testBuilder;
	
	public Axis2SessionCreationTest(){
		super();
		testBuilder = new Axis2TestBuilder();
	}	
	
	/** 
	 * create a single Axis2 instance
	 * 
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		// setup a single axis2 instance
		repo = System.getProperty("axis2.repo");
		
		axis2File = repo + "/server/axis2.xml";
		
		testBuilder.startUpAxis2Instance(repo,axis2File,port);
		
		System.setProperty("axis2.xml",repo + "/client/axis2.xml");
	}
	
	protected void tearDown() throws Exception {
		testBuilder.stopAxis2Instances();
	}
	
	public void testSessionCreation() throws Exception {
		Map eprParams = new HashMap();
		
		SOAPEnvelope request = testBuilder.getLoginRequest("12345", "911");
		SOAPEnvelope reply = testBuilder.send(testBuilder.authServiceUrl, eprParams, request);

		String response = testBuilder.retrieveAuthStatus(reply);
		assertTrue("Session creation was unsuccessful",response.startsWith("Success"));
		
		String sessionId = testBuilder.retrieveSessionId(eprParams);
		assertNotNull("Session id is null",sessionId);
		
		// you can write a test case to verfiy the format of session id as well
	}
	
	public static void main(String[] args){
		Axis2SessionCreationTest t = new Axis2SessionCreationTest();
		try {
			t.setUp();
			t.testSessionCreation();
			t.tearDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
