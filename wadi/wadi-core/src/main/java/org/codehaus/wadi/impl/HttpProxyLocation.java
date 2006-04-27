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

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.ProxiedLocation;
import org.codehaus.wadi.ProxyingException;

/**
 * A Location that includes a hostname/ip-address and HTTP port
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class HttpProxyLocation extends SimpleEvictable implements Location {
	
	protected final static Log _log = LogFactory.getLog(HttpProxyLocation.class);
	
	protected ProxiedLocation _location;
	protected InvocationProxy _proxy;
	
	public HttpProxyLocation(Destination destination, ProxiedLocation location, InvocationProxy proxy) {
		super();
		_destination=destination;
		_location=location;
		_proxy=proxy;
	}
	
	public void proxy(Invocation invocation) throws ProxyingException {
		_proxy.proxy(_location, invocation);
	}
	
	public String toString() {
		return "<HttpProxyLocation:"+_location+">"; // we could include proxy strategy here...
	}
	
	protected final Destination _destination;
	
	public Destination getDestination() {
		return _destination;
	}
	
}
