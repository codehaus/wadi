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
package org.codehaus.wadi.sandbox.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;


public class StatefulHttpServletRequestWrapper extends HttpServletRequestWrapper {
    
    protected static final HttpServletRequest _dummy=new DummyHttpServletRequest();
	protected Session _session;
	    
	public StatefulHttpServletRequestWrapper(HttpServletRequest request, Session state) {
	    super(_dummy);
	}

	void initialise(HttpServletRequest request, Session session) {
	    setRequest(request);
	    _session=session;
	}
	
	void recycle() {
	    setRequest(_dummy);
	    _session=null;
	}
	
	// Session related method interceptions...
	
	public HttpSession getSession(){return getSession(true);}

	public HttpSession getSession(boolean create) {
	    // TODO - I'm assuming single threaded access to request objects...

	    if (null==_session) {
	        if (create) {
	            // create a new session...
	            return null; // FIXME
	        } else {
	            return null;
	        }
	    } else {
	        return _session.getWrapper();
	    }
	}
}