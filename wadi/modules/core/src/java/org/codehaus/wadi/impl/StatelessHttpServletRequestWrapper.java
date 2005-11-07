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


public class StatelessHttpServletRequestWrapper extends HttpServletRequestWrapper {
    
	public StatelessHttpServletRequestWrapper(HttpServletRequest request) {super(request);}
    
	// These methods should never be called while contextualising a stateless request...
	public HttpSession getSession(){return getSession(true);}
	public HttpSession getSession(boolean create){throw new UnsupportedOperationException();}

	// TODO - consider session cookie related methods as well..
}