/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.codehaus.wadi.Context;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.PoolableHttpServletRequestWrapper;
import org.codehaus.wadi.WADIHttpSession;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class StatefulHttpServletRequestWrapper extends HttpServletRequestWrapper implements PoolableHttpServletRequestWrapper {

	protected static final HttpServletRequest _dummy=new DummyHttpServletRequest();
	protected HttpSession _session; // I want to maintain a Session - but it's hard to get hold of it upon creation... - do we really need it ?


	public StatefulHttpServletRequestWrapper() {
		super(_dummy);
	}

	public void init(InvocationContext invocationContext, Context context) {
		HttpServletRequest request = ((WebInvocationContext) invocationContext).getHreq();
		setRequest(request);
		_session=context==null?null:((WADIHttpSession)context).getWrapper();
	}

	public void destroy() {
		setRequest(_dummy);
		_session=null;
	}

	// Session related method interceptions...

	public HttpSession getSession(){return getSession(true);}

	public HttpSession getSession(boolean create) {
		// TODO - I'm assuming single threaded access to request objects...
		// so no synchronization ?

		if (null==_session)
			return (_session=((HttpServletRequest)getRequest()).getSession(create));
		else
			return _session;
	}

}
