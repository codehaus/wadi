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
//========================================================================
//$Id$
//Copyright 2004-2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.codehaus.wadi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.EndPoint;
import org.codehaus.wadi.ProxyingException;
import org.codehaus.wadi.web.WebEndPoint;
import org.codehaus.wadi.web.WebInvocation;

//HTTP/1.1 Methods:

//OPTIONS - RO, IDEM, SL?
//GET - RO, IDEM, SF,
//HEAD - RO, IDEM, SF?
//POST - RW, SF
//PUT - RW, IDEM, SF?
//DELETE - RW, IDEM, SF?
//TRACE - RO, IDEM, SL
//CONNECT - RO, IDEM, SL

// extra WEBDAV Methods - PROPFIND etc...

//SL methods need not be proxied
//some SF methods may not use state and hence need not be proxied
//HTTPS may result in immediate migration - or can we find a way of proxying it ? Are we ever likely to encounter it in a cluster ? Perhaps we could use an java.net.HttpsURLCommection ?

//how about some regexps: stateful-methods, stateful-uris
//if request does not relate to a session and match both regexps in this order, it will be given a stateless context and executed locally.
//if it matches (and is not https) it will be proxied
//if it is https, state will immigrate underneath it immediately.
//if lb integration allows updated routing info - do that too...
//if request accesses a forbidden resource, it can be processed locally/considered stateless

//we should establish the connection to server, before delegating to proxy code. This way we know if server is valid. Once control has been passed to proxy code, all subsequent errors will be passed directly to client...
//or we could leave the code here and use an IOException to signify to our caller that something under its control has gone wrong...

//Greg reckons all forms of HTTP auth except certificate should work ok
//look at Jetty ProxyHandler
//Https cannot be proxied at this level - need a Handler/Valve - why ?
//if ssl decryption is done at frontend how do we know whether an HTTP request is secure or not ?

//is there some way we can run our client2server output stream on a background thread ? perhaps using commons-httpclient...?

/**
 * Useful support for implementations of the HttpProxy interface
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @author <a href="mailto:gregw@mortbay.com">Greg Wilkins</a>
 * @version $Revision$
 */
public abstract class AbstractHttpProxy implements InvocationProxy {
	
	public static final String _WADI_IsSecure="WADI-IsSecure";
	
	protected static final HashSet _DontProxyHeaders = new HashSet();
	static
	{
		_DontProxyHeaders.add("proxy-connection");
		_DontProxyHeaders.add("connection");
		_DontProxyHeaders.add("keep-alive");
		_DontProxyHeaders.add("transfer-encoding");
		_DontProxyHeaders.add("te");
		_DontProxyHeaders.add("trailer");
		_DontProxyHeaders.add("proxy-authorization");
		_DontProxyHeaders.add("proxy-authenticate");
		_DontProxyHeaders.add("upgrade");
	}
	
	protected final String _sessionPathParamKey;
	
	public AbstractHttpProxy(String sessionPathParamKey) {
		_sessionPathParamKey=sessionPathParamKey;
	}
	
	public int copy(InputStream is, OutputStream os, int length) throws IOException {
		int total=0;
		byte[] buffer=new byte[length];
		for (int n=0; (n=is.read(buffer, 0, length))>=0;) {
			os.write(buffer,0,n);
			total+=n;
		}
		return total;
	}
	
	public String getRequestURI(HttpServletRequest hreq) {
		String uri=hreq.getRequestURI();
		// Jetty will return path params in this uri. Tomcat won't.
		// There seems to be no API for retrieving them in any other way - DOH !
		// Using more then just a jsessionid path param seems to confuse Tomcat anyway...
		if (hreq.isRequestedSessionIdFromURL() && !(uri.lastIndexOf(';')>=0)) {
			uri=new StringBuffer(uri).append(";").append(_sessionPathParamKey).append("=").append(hreq.getRequestedSessionId()).toString();
		}
		return uri;
	}
	
	public final void proxy(EndPoint location, Invocation invocation) throws ProxyingException {
		if (false == location instanceof WebEndPoint) {
			throw new IllegalArgumentException(WebEndPoint.class +
			" is expected.");
		} else if (false == invocation instanceof WebInvocation) {
			throw new IllegalArgumentException(WebInvocation.class +
			" is expected.");
		}
		WebEndPoint proxiedLocation = (WebEndPoint) location;
		InetSocketAddress address = proxiedLocation.getInetSocketAddress();
		
		WebInvocation context = (WebInvocation) invocation;
		doProxy(address, context);
	}
	
	protected abstract void doProxy(InetSocketAddress address, WebInvocation context) throws ProxyingException;
	
}
