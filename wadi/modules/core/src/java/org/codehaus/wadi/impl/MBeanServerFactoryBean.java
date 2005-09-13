package org.codehaus.wadi.impl;

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

// only create a new MBeanServer if one does not already exist...

public class MBeanServerFactoryBean extends org.springframework.jmx.support.MBeanServerFactoryBean {
	
	protected MBeanServer _server;
	
	public void afterPropertiesSet() {
		String agentId=null; // TODO - parameterise
		ArrayList servers=MBeanServerFactory.findMBeanServer(agentId);
		if (servers!=null && servers.size()>0)
			_server=(MBeanServer)servers.get(0);
		else
			super.afterPropertiesSet();
	}
	
	public Object getObject() {
		if (_server!=null)
			return _server;
		else
			return super.getObject();
	}
	
	public Class getObjectType() {
		if (_server!=null)
			return _server.getClass();
		else
			return super.getObjectType();
	}
	
	public void destroy() {
		if (_server==null)
			super.destroy();
	}
}
