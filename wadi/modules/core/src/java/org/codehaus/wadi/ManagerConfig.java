package org.codehaus.wadi;

import javax.servlet.ServletContext;

import org.codehaus.wadi.impl.StandardManager;

public interface ManagerConfig {

	ServletContext getServletContext();
	void callback(StandardManager manager);
	
}
