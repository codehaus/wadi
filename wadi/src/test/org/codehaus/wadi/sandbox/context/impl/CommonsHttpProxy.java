//this started life as org.mortbay.servlet.ProxyServlet. I copied the
//whole thing to use as a starting point - Thanks Greg !

// ========================================================================
// $Id$
// Copyright 2004-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.ConnectMethod;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// This does not seem as performant as the StandardHttpProxy - it also seems to be able to crash Firefox reproducibly !
// commons-httpclient does not [seem to] allow us to pass Cookie headers straight through. So, we have
// to deal with them explicitly - This leaves scope for cookie mutation as request passes through proxy - Bad news.

// I've tried moving up to 3.0rc1, but ProxyServlet with this HttpProxy produces wierd browser effects...

/**
 * HttpProxy implementation based on commons-httpclient
 *
 * @author gregw@mortbay.com, jules@coredevelopers.net
 */
public class CommonsHttpProxy extends AbstractHttpProxy {

	protected static final Log _log=LogFactory.getLog(CommonsHttpProxy.class);

	protected static final Map _methods=new HashMap();
	static {
		_methods.put("CONNECT",ConnectMethod.class);
		_methods.put("DELETE",DeleteMethod.class);
		_methods.put("GET", GetMethod.class);
		_methods.put("HEAD",HeadMethod.class);
		_methods.put("OPTIONS",OptionsMethod.class);
		_methods.put("TRACE",TraceMethod.class);
		_methods.put("POST",MultipartPostMethod.class);
		_methods.put("PUT",PutMethod.class);
		// WebDav methods ? e.g. PROCFIND ?
	}
	
	public CommonsHttpProxy(String sessionPathParamKey) {
		super(sessionPathParamKey);
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse)
	 */
	public boolean proxy(InetSocketAddress location, HttpServletRequest hreq, HttpServletResponse hres) {
		long startTime=System.currentTimeMillis();

		String m=hreq.getMethod();
	    Class clazz=(Class)_methods.get(m);
	    if (clazz==null) {
	    	_log.warn("unsupported http method: "+m);
	    	return false;
	    }
	    
	    HttpMethod hm=null;
	    try {
	    	hm=(HttpMethod)clazz.newInstance();
	    } catch (Exception e) {
	    	_log.warn("could not create HttpMethod instance", e); // should never happen
	    	return false;
	    }
	    
	    String uri=getRequestURI(hreq);
	    hm.setPath(uri);
	    
	    String queryString=hreq.getQueryString();
	    if (queryString!=null) {
	    	hm.setQueryString(queryString);
	    	uri+=queryString;
	    }
	    
	    hm.setFollowRedirects(false);
	    //hm.setURI(new URI(uri));
	    hm.setStrictMode(false);

		// check connection header
		String connectionHdr = hreq.getHeader("Connection"); // TODO - what if there are multiple values ?
		if (connectionHdr != null) {
			connectionHdr = connectionHdr.toLowerCase();
			if (connectionHdr.equals("keep-alive")|| connectionHdr.equals("close"))
				connectionHdr = null; // TODO  ??
		}
		
		// copy headers
		boolean xForwardedFor = false;
		boolean hasContent = false;
		int contentLength=0;
		Enumeration enm = hreq.getHeaderNames();
		while (enm.hasMoreElements()) {
			// TODO could be better than this! - using javax.servlet ?
			String hdr = (String) enm.nextElement();
			String lhdr = hdr.toLowerCase();

			if (_DontProxyHeaders.contains(lhdr))
				continue;
			if (connectionHdr != null && connectionHdr.indexOf(lhdr) >= 0)
				continue;

			if ("content-length".equals(lhdr)) {
				try {
					contentLength=hreq.getIntHeader(hdr);
					hasContent=contentLength>0;
				} catch (NumberFormatException e) {
					_log.info("bad Content-Length header value: "+hreq.getHeader(hdr), e);
				}
			}
				
			if ("content-type".equals(lhdr)) {
				hasContent=true;
			}
			
			Enumeration vals = hreq.getHeaders(hdr);
			while (vals.hasMoreElements()) {
				String val = (String) vals.nextElement();
				if (val != null) {
					hm.addRequestHeader(hdr, val);
					//_log.info("Request " + hdr + ": " + val);
					xForwardedFor |= "X-Forwarded-For".equalsIgnoreCase(hdr); // why is this not in the outer loop ?
				}
			}	
		}
		
		// cookies...
		
		// although we copy cookie headers into the request abover - commons-httpclient thinks it knows better and strips them out before sending.
		// we have to explicitly use their interface to add the cookies - painful...
		
		// DOH! - an org.apache.commons.httpclient.Cookie is NOT a
		// javax.servlet.http.Cookie - and it looks like the two don't
		// map onto each other without data loss...
		HttpState state=new HttpState();
		javax.servlet.http.Cookie[] cookies=hreq.getCookies();
		if (cookies!=null)
		{
			for (int i=0;i<cookies.length;i++)
			{
				javax.servlet.http.Cookie c=cookies[i];
				String domain=c.getDomain();
				if (domain==null) {
					domain=hreq.getServerName(); // TODO - tmp test
					//_log.warn("defaulting cookie domain");
				}
				//	  domain=null;
				String cpath=c.getPath();
				if (cpath==null) {
					cpath=hreq.getContextPath(); // fix for Jetty
					//_log.warn("defaulting cookie path");
				}
				//if (_log.isTraceEnabled()) _log.trace("PATH: value="+path+" length="+(path==null?0:path.length()));
				Cookie cookie=new Cookie(domain, c.getName(), c.getValue(), cpath, c.getMaxAge(), c.getSecure()); // TODO - sort out domain
				//if (_log.isTraceEnabled()) _log.trace("Cookie: "+cookie.getDomain()+","+ cookie.getName()+","+ cookie.getValue()+","+ cookie.getPath()+","+ cookie.getExpiryDate()+","+ cookie.getSecure());
				state.addCookie(cookie);
				//if (_log.isTraceEnabled()) _log.trace("Cookie: "+cookie.toString());
			}
		}
		
		// Proxy headers
		hm.addRequestHeader("Via", "1.1 "+hreq.getLocalName()+":"+hreq.getLocalPort()+" \"WADI\"");
		if (!xForwardedFor)
			hm.addRequestHeader("X-Forwarded-For", hreq.getRemoteAddr());
		// Max-Forwards...

		// a little bit of cache control
//		String cache_control = hreq.getHeader("Cache-Control");
//		if (cache_control != null && (cache_control.indexOf("no-cache") >= 0 || cache_control.indexOf("no-store") >= 0))
//			httpMethod.setUseCaches(false);

		// customize Connection
//		uc.setDoInput(true);

		int client2ServerTotal=0;
		if (hasContent) {
//			uc.setDoOutput(true);
			
			try {
				if (hm instanceof EntityEnclosingMethod)
					((EntityEnclosingMethod)hm).setRequestBody(hreq.getInputStream());
				// TODO - do we need to close response stream at end... ?
			} catch (IOException e) {
				_log.warn("could not pss request input across proxy", e);
				return false;
			}
		}

		try
		{
			HttpClient client=new HttpClient();
			HostConfiguration hc=new HostConfiguration();
			//String host=location.getAddress().getHostAddress();
			String host=location.getHostName(); // inefficient - but stops httpclient from rejecting half our cookies...
			hc.setHost(host, location.getPort());
			client.executeMethod(hc, hm, state);
		}
		catch (IOException e)	// TODO
		{
			_log.warn("problem proxying connection:",e);
		}
		
		InputStream fromServer = null;
		
		// handler status codes etc.
		int code=502;
		String message="Bad Gateway: could not read server response code or message";
		
		code=hm.getStatusCode(); // IOException
		message=hm.getStatusText(); // IOException
		hres.setStatus(code, message);
		
		try {
			fromServer=hm.getResponseBodyAsStream(); // IOException
		} catch (IOException e) {
			_log.info("problem acquiring http client output", e);
		}
		
		
		// clear response defaults.
		hres.setHeader("Date", null);
		hres.setHeader("Server", null);
		
		// set response headers
		// TODO - is it a bug in Jetty that I have to start my loop at 1 ? or that key[0]==null ?
		// Try this inside Tomcat...
		Header[] headers=hm.getResponseHeaders();
		for (int i=0; i<headers.length; i++) {
			String h=headers[i].toExternalForm();
			int index=h.indexOf(':');
			String key=h.substring(0, index).trim().toLowerCase();
			String val=h.substring(index+1, h.length()).trim();
			if (val!=null && !_DontProxyHeaders.contains(key)) {
				hres.addHeader(key, val);
				//_log.info("Response: "+key+" - "+val);
			}
		}
		
		hres.addHeader("Via", "1.1 (WADI)");

		// copy server->client
		int server2ClientTotal=0;
		if (fromServer!=null) {
			try {
				OutputStream toClient=hres.getOutputStream();// IOException
				server2ClientTotal+=copy(fromServer, toClient, 8192);// IOException
			} catch (IOException e) {
				_log.info("problem proxying server response back to client", e);
			} finally {
				try {
					fromServer.close();
				} catch (IOException e) {
					// well - we did our best...
					_log.info("problem closing server response stream", e);
				}
			}
		}

		long endTime=System.currentTimeMillis();
		long elapsed=endTime-startTime;
		_log.info("in:"+client2ServerTotal+", out:"+server2ClientTotal+", status:"+code+", time:"+elapsed+", url:http://"+location.getHostName()+":"+location.getPort()+uri);
	
		return true;
	}
}
