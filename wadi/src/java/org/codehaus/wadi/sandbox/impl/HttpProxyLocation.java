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

import java.net.InetSocketAddress;

import javax.jms.Destination;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.HttpProxy;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.ProxyingException;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class HttpProxyLocation extends SimpleEvictable implements Location {
	protected final static Log _log = LogFactory.getLog(HttpProxyLocation.class);

	protected InetSocketAddress _location;
	protected HttpProxy _proxy;

	public HttpProxyLocation(Destination destination, InetSocketAddress location, HttpProxy proxy) {
		super();
		_destination=destination;
		_location=location;
		_proxy=proxy;
	}

	public void proxy(HttpServletRequest hreq, HttpServletResponse hres) throws ProxyingException {
		_proxy.proxy(_location, hreq, hres);
	}

	public String toString() {return "<HttpProxyLocation:"+_location+">";} // we could include proxy strategy here...

	protected final Destination _destination;
	public Destination getDestination(){return _destination;}
}
