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
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// My choice of proxy - still suboptimal - servlet spec imposes a very clumsy API
// for copying the headers out of the HttpServletRequest (a proprietary solution 
// would be faster), but at least the Cookie headers can be copied straight across 
// (see CommonsHttpProxy).

/**
 * Enterprise HttpProxy implementation.
 *
 * @author gregw@mortbay.com, jules@coredevelopers.net
 *
 */
public class StandardHttpProxy extends AbstractHttpProxy {

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse)
	 */
	public void proxy2(InetSocketAddress location, HttpServletRequest req, HttpServletResponse res) throws IOException	{

		String uri=req.getRequestURI();
		String qs=req.getQueryString();
		if (qs!=null) {
			uri=new StringBuffer(uri).append("?").append(qs).toString();
		}

		URL url=new URL(req.getScheme(), location.getAddress().getHostAddress(), location.getPort(), uri);
		
		long startTime=System.currentTimeMillis();

		URLConnection uc=url.openConnection(); // IOException

		uc.setAllowUserInteraction(false);

		// Set method - TODO - are we ever going to make a non-HttpURLConnection ?
		HttpURLConnection huc=null;
		if (uc instanceof HttpURLConnection) {
			huc = (HttpURLConnection) uc;
			String method=req.getMethod();
			huc.setRequestMethod(method);
			huc.setInstanceFollowRedirects(false);
		}

		// check connection header
		// TODO - this might need some more time: see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
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
					uc.addRequestProperty(hdr, val);
					//_log.debug("req " + hdr + ": " + val);
					xForwardedFor |= "X-Forwarded-For".equalsIgnoreCase(hdr); // why is this not in the outer loop ?
				}
			}
		}
		
		// Proxy headers
		uc.setRequestProperty("Via", "1.1 (WADI)");
		if (!xForwardedFor)
			uc.addRequestProperty("X-Forwarded-For", req.getRemoteAddr());

		// a little bit of cache control
		String cache_control = req.getHeader("Cache-Control");
		if (cache_control != null && (cache_control.indexOf("no-cache") >= 0 || cache_control.indexOf("no-store") >= 0))
			uc.setUseCaches(false);

		// customize Connection
		uc.setDoInput(true);

		int client2ServerTotal=0;
		if (hasContent) {
			uc.setDoOutput(true);
			
			OutputStream toServer=null;
			try {
				InputStream fromClient=req.getInputStream(); // IOException
				toServer=uc.getOutputStream(); // IOException
				client2ServerTotal=copy(fromClient, toServer, 8192);
			} catch (IOException e) {
				_log.info("problem proxying client request to server", e);
			} finally {
				if (toServer!=null) {
					try {
						toServer.close(); // IOException
					} catch (IOException e) {
						_log.info("problem closing server request stream", e);
					}
				}
			}
		}

		// Connect - TODO - should we really leave it this late before trying to connect ?
		uc.connect(); // IOException

		InputStream fromServer = null;

		// handler status codes etc.
		int code=0;
		if (huc==null) {
			try {
				fromServer = uc.getInputStream(); // IOException
			} catch (IOException e) {
				_log.info("problem acquiring client output", e);
			}
		} else {
			code=502;
			String message="Bad Gateway: could not read server response code or message";
			try {
				code=huc.getResponseCode(); // IOException
				message=huc.getResponseMessage(); // IOException
			} catch (IOException e) {
				_log.info("problem acquiring http server response code/message", e);
			} finally {
				res.setStatus(code, message);
			}

			if (code<400) {
				// 1XX:continue, 2XX:successful, 3XX:multiple-choices...
				try {
					fromServer=huc.getInputStream(); // IOException
				} catch (IOException e) {
					_log.info("problem acquiring http client output", e);
				}
			} else {
				// 4XX:client, 5XX:server error...
				fromServer = huc.getErrorStream(); // why does this not throw IOException ?
				// TODO - do we need to use sendError()?
			}
		}

		// clear response defaults.
		res.setHeader("Date", null);
		res.setHeader("Server", null);

		// set response headers
		if (false) {
			int h = 0;
			String hdr = uc.getHeaderFieldKey(h);
			String val = uc.getHeaderField(h);
			while (hdr != null || val != null) {
				String lhdr = (hdr != null) ? hdr.toLowerCase() : null;
				if (hdr != null && val != null && !_DontProxyHeaders.contains(lhdr))
					res.addHeader(hdr, val);
				
				//_log.debug("res " + hdr + ": " + val);
				
				h++;
				hdr = uc.getHeaderFieldKey(h);
				val = uc.getHeaderField(h);
			}
		} else {	
			// TODO - is it a bug in Jetty that I have to start my loop at 1 ? or that key[0]==null ?
			// Try this inside Tomcat...
			String key;
			for (int i=1; (key=uc.getHeaderFieldKey(i))!=null; i++) {
				key=key.toLowerCase();
				String val=uc.getHeaderField(i);
				if (val!=null && !_DontProxyHeaders.contains(key)) {
					res.addHeader(key, val);
				}
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
		_log.info("in:"+client2ServerTotal+", out:"+server2ClientTotal+", status:"+code+", time:"+elapsed+", url:"+url);
	}
}
