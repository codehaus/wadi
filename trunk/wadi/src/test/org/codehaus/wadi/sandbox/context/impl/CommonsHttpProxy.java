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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.MultipartPostMethod;

// This does not seem as performant as the StandardHttpProxy - it also seems to be able to crash Firefox reproducibly !
// commons-httpclient does not [seem to] allow us to pass Cookie headers straight through. So, we have
// to deal with them explicitly - This leaves scope for cookie mutation as request passes through proxy - Bad news.

/**
 * Enterprise HttpProxy implementation.
 *
 * @author gregw@mortbay.com, jules@coredevelopers.net
 *
 */
public class CommonsHttpProxy extends AbstractHttpProxy {
	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse)
	 */
	public void proxy2(InetSocketAddress location, HttpServletRequest req, HttpServletResponse res) throws IOException	{

	    String m=req.getMethod();

	    HttpMethod httpMethod=null;
	    if ("GET".equals(m))
	    	httpMethod=new GetMethod();
	    else if ("POST".equals(m))
	    	httpMethod=new MultipartPostMethod();
	    //... ConnectMethod, DeleteMethod, HeadMethod, OptionsMethod, TraceMethod
	    else {
	    	_log.warn("NYI: "+m);
	    	return;
	    }
	    
	    String contextPath=req.getContextPath();
	    contextPath=(contextPath==null?"":contextPath);
	    String servletPath=req.getServletPath();
	    servletPath=(servletPath==null?"":servletPath);
	    String path=contextPath+servletPath;
	    httpMethod.setPath(path);// TODO - url params ?
	    String queryString=req.getQueryString();
    	httpMethod.setQueryString(queryString);

	    String uri=path+(queryString==null?"":("?"+queryString)); // used later
	    httpMethod.setFollowRedirects(false);

		long startTime=System.currentTimeMillis();

		// check connection header
		String connectionHdr = req.getHeader("Connection"); // TODO - what if there are multiple values ?
		if (connectionHdr != null) {
			connectionHdr = connectionHdr.toLowerCase();
			if (connectionHdr.equals("keep-alive")|| connectionHdr.equals("close"))
				connectionHdr = null; // TODO  ??
		}
		
		// copy headers
		boolean xForwardedFor = false;
		boolean hasContent = false;
		int contentLength=0;
		Enumeration enm = req.getHeaderNames();
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
					contentLength=req.getIntHeader(hdr);
					hasContent=contentLength>0;
				} catch (NumberFormatException e) {
					_log.info("bad Content-Length header value: "+req.getHeader(hdr), e);
				}
			}
				
			if ("content-type".equals(lhdr)) {
				hasContent=true;
			}
			
			
			Enumeration vals = req.getHeaders(hdr);
			while (vals.hasMoreElements()) {
				String val = (String) vals.nextElement();
				if (val != null) {
					httpMethod.addRequestHeader(hdr, val);
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
		//state.setCookiePolicy(CookiePolicy.COMPATIBILITY);
		javax.servlet.http.Cookie[] cookies=req.getCookies();
		if (cookies!=null)
		{
			for (int i=0;i<cookies.length;i++)
			{
				javax.servlet.http.Cookie c=cookies[i];
				String domain=c.getDomain();
				if (domain==null) {
					domain=req.getServerName(); // TODO - tmp test
					//_log.warn("defaulting cookie domain");
				}
				//	  domain=null;
				String cpath=c.getPath();
				if (cpath==null) {
					cpath=req.getContextPath(); // fix for Jetty
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
		httpMethod.setRequestHeader("Via", "1.1 (WADI)");
		if (!xForwardedFor)
			httpMethod.setRequestHeader("X-Forwarded-For", req.getRemoteAddr());

		// a little bit of cache control
//		String cache_control = hreq.getHeader("Cache-Control");
//		if (cache_control != null && (cache_control.indexOf("no-cache") >= 0 || cache_control.indexOf("no-store") >= 0))
//			httpMethod.setUseCaches(false);

		// customize Connection
//		uc.setDoInput(true);

		int client2ServerTotal=0;
		if (hasContent) {
//			uc.setDoOutput(true);
			
			if (httpMethod instanceof EntityEnclosingMethod)
				((EntityEnclosingMethod)httpMethod).setRequestBody(req.getInputStream());
			// TODO - do we need to close response stream at end... ?
		}

		try
		{
			HttpClient client=new HttpClient();
			HostConfiguration hc=new HostConfiguration();
			hc.setHost(location.getAddress().getHostAddress(), location.getPort());
			client.executeMethod(hc, httpMethod, state);
		}
		catch (IOException e)	// TODO
		{
			_log.warn("problem proxying connection:",e);
		}
		
		InputStream fromServer = null;
		
		// handler status codes etc.
		int code=502;
		String message="Bad Gateway: could not read server response code or message";
		
		code=httpMethod.getStatusCode(); // IOException
		message=httpMethod.getStatusText(); // IOException
		res.setStatus(code, message);
		
		try {
			fromServer=httpMethod.getResponseBodyAsStream(); // IOException
		} catch (IOException e) {
			_log.info("problem acquiring http client output", e);
		}
		
		
		// clear response defaults.
		res.setHeader("Date", null);
		res.setHeader("Server", null);
		
		// set response headers
		// TODO - is it a bug in Jetty that I have to start my loop at 1 ? or that key[0]==null ?
		// Try this inside Tomcat...
		Header[] headers=httpMethod.getResponseHeaders();
		for (int i=0; i<headers.length; i++) {
			String h=headers[i].toExternalForm();
			int index=h.indexOf(':');
			String key=h.substring(0, index).trim().toLowerCase();
			String val=h.substring(index+1, h.length()).trim();
			if (val!=null && !_DontProxyHeaders.contains(key)) {
				res.addHeader(key, val);
				//_log.info("Response: "+key+" - "+val);
			}
		}
		
		res.addHeader("Via", "1.1 (WADI)");

		// copy server->client
		int server2ClientTotal=0;
		if (fromServer!=null) {
			try {
				OutputStream toClient=res.getOutputStream();// IOException
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
		_log.info("in:"+client2ServerTotal+", out:"+server2ClientTotal+", status:"+code+", time:"+elapsed+", url:"+location.getHostName()+":"+location.getPort()+"/"+uri);
	}
}
