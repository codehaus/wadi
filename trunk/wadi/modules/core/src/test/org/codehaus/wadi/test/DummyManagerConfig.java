package org.codehaus.wadi.test;

import javax.servlet.ServletContext;

import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.impl.StandardManager;

public class DummyManagerConfig implements ManagerConfig {

	public ServletContext getServletContext() {
		return null;
	}

	public void callback(StandardManager manager) {
	// do nothing - should install Listeners
	}

}
