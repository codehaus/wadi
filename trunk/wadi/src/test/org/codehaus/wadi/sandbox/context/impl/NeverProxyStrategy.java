/**
*
* Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.ProxyStrategy;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class NeverProxyStrategy implements ProxyStrategy {

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.ProxyStrategy#proxy(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, EDU.oswego.cs.dl.util.concurrent.Sync, org.codehaus.wadi.sandbox.context.Location)
	 */
	public boolean proxy(HttpServletRequest hreq, HttpServletResponse hres, String id, Sync promotionLock, Location location) throws IOException {
		return false;
	}
}
