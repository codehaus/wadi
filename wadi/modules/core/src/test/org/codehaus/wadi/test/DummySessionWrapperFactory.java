package org.codehaus.wadi.test;

import javax.servlet.http.HttpSession;

import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.impl.SessionWrapper;

public class DummySessionWrapperFactory implements SessionWrapperFactory {

	public HttpSession create(Session session) {
		return new SessionWrapper(session);
	}

}
