package org.codehaus.wadi;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.gridstate.Dispatcher;

import junit.framework.TestCase;

public abstract class AbstractTestEvacuation extends TestCase {
	
	protected Log _log = LogFactory.getLog(getClass());
	protected String _url = "jdbc:axiondb:WADI";
	protected DataSource _ds = new AxionDataSource(_url);
	
	public AbstractTestEvacuation(String arg0) {
		super(arg0);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;
	
	public void testEvacuation() throws Exception {
		assertTrue(true);
		String clusterName="TEST";
		
		MyStack stack1=new MyStack(_url, _ds, createDispatcher(clusterName, "red", 5000L));
		_log.info("RED STARTING...");
		stack1.start();
		_log.info("...RED STARTED");
		MyStack stack2=new MyStack(_url, _ds, createDispatcher(clusterName, "green", 5000L));
		_log.info("GREEN STARTING...");
		stack2.start();
		_log.info("...GREEN STARTED");
		
		String id=stack1.getManager().create().getId();
		
		_log.info("RED STOPPING...");
		stack1.stop();
		_log.info("...RED STOPPED");
		
		//    stack2.getManager().destroy(id);
		
		_log.info("GREEN STOPPING...");
		stack2.stop();
		_log.info("...GREEN STOPPED");
	}
	
}
