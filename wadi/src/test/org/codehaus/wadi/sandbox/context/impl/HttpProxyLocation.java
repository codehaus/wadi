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

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.HttpProxy;
import org.codehaus.wadi.sandbox.context.Location;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class HttpProxyLocation implements Location {
	protected final static Log _log = LogFactory.getLog(HttpProxyLocation.class);
	
	protected HttpProxy _proxy=new StandardHttpProxy();
	protected InetSocketAddress _location;
	
	public HttpProxyLocation(InetSocketAddress location) {
		super();
		_location=location;
	}
	
	// TODO - serial proxying at the moment - until I decide how to make it concurrent...
	public Context proxy(ServletRequest req, ServletResponse res, String id, Sync promotionLock) {
		HttpServletRequest hreq=(HttpServletRequest)req;
		String uri=hreq.getRequestURI();
		String qs=hreq.getQueryString();
		if (qs!=null) {
			uri=new StringBuffer(uri).append("?").append(qs).toString();
		}

		try {
			URL url=new URL(req.getScheme(), _location.getHostName(), _location.getPort(), uri);
			_log.info("proxying to: "+url);
			_proxy.proxy(req, res, url); // TODO - why a URL - expensive...
		} catch (MalformedURLException e) {
			_log.error(e);
		} finally {
			promotionLock.release();
		}
		
		return null; // no migration yet
	}

	public long getExpiryTime(){ return 0;}// TODO - NYI
}
