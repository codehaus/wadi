//this started life as org.mortbay.servlet.ProxyServlet. I copied the
//whole thing to use as a starting point - Thanks Greg !

// HTTP/1.1 Methods:

// OPTIONS - RO, IDEM, SL?
// GET - RO, IDEM, SF,
// HEAD - RO, IDEM, SF?
// POST - RW, SF
// PUT - RW, IDEM, SF?
// DELETE - RW, IDEM, SF?
// TRACE - RO, IDEM, SL
// CONNECT - RO, IDEM, SL

// SL methods need not be proxied
// some SF methods may not use state and hence need not be proxied
// HTTPS may result in immediate migration - or can we find a way of proxying it ? Are we ever likely to encounter it in a cluster ? Perhaps we could use an java.net.HttpsURLCommection ?

// how about some regexps: stateful-methods, stateful-uris
// if request does not relate to a session and match both regexps in this order, it will be given a stateless context and executed locally.
// if it matches (and is not https) it will be proxied
// if it is https, state will immigrate underneath it immediately.
// if lb integration allows updated routing info - do that too...

// we should establish the connection to server, before delegating to proxy code. This way we know if server is valid. Once control has been passed to proxy code, all subsequent errors will be passed directly to client...
// or we could leave the code here and use an IOException to signify to our caller that something under its control has gone wrong...

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

package org.codehaus.wadi.test.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Enterprise HttpProxy implementation.
 *
 * @author gregw@mortbay.com, jules@coredevelopers.net
 *
 */
public class HttpProxy {
	protected Log _log = LogFactory.getLog(HttpProxy.class);

	// need to understand why these should not be proxied...
	protected HashSet _DontProxyHeaders = new HashSet();

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

	class Copier implements Runnable {

		protected final InputStream _is;
		protected final OutputStream _os;
		protected final int _length;
		protected final byte[] _buffer;

		protected int _total;
		public int getTotal(){return _total;}

		protected Exception _exception;
		public Exception getException(){return _exception;}

		protected Thread _thread;
		public Thread getThread(){return _thread;}

		public Copier(InputStream is, OutputStream os, int length) {
			_is=is;
			_os=os;
			_length=length;
			_buffer=new byte[_length];
		}

		public void runSynchronously() throws IOException {
			_total=0;
			for (int n=0; (n=_is.read(_buffer, 0, _length))>=0;) {
				_os.write(_buffer,0,n);
				_total+=n;
			}
		}

		public void runAsynchronously() {
			(_thread=new Thread(this)).start();
		}

		public void run() {
			try {
				runSynchronously();
			} catch (IOException e) {
				_exception=e; // report asynchronously
			}
		}
	}

	// move this into ProxyServlet...
	public void proxy(ServletRequest req, ServletResponse res, URL url)	{
		// we could check to scheme here as well - http only supported...

		try {
			proxy2(req, res, url);
		} catch (IOException e) {
			if (res instanceof HttpServletResponse) {
				HttpServletResponse hres = (HttpServletResponse) res;
				hres.setHeader("Date", null);
				hres.setHeader("Server", null);
				hres.addHeader("Via", "1.1 (WADI)");
				// hres.setStatus(502, message);
				try {
					_log.warn("could not establish connection to server: "+url, e);
					hres.sendError(502, "Bad Gateway: proxy could not establish connection to server"); // TODO - why do we need to use sendError ?
				} catch (IOException e2) {
					_log.warn("could not return error to client", e2);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse)
	 */
	public void proxy2(ServletRequest req, ServletResponse res, URL url) throws IOException	{
		HttpServletRequest hreq = (HttpServletRequest) req;
		HttpServletResponse hres = (HttpServletResponse) res;

		Copier client2Server=null;
		Copier server2Client=null;
		long startTime=System.currentTimeMillis();

		URLConnection uc=url.openConnection(); // IOException

		uc.setAllowUserInteraction(false);

		// Set method
		HttpURLConnection huc=null;
		if (uc instanceof HttpURLConnection) {
			huc = (HttpURLConnection) uc;
			String method=hreq.getMethod();
			huc.setRequestMethod(method);
			huc.setInstanceFollowRedirects(false);
		}

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
		Enumeration enm = hreq.getHeaderNames();
		while (enm.hasMoreElements()) {
			// TODO could be better than this! - using javax.servlet ?
			String hdr = (String) enm.nextElement();
			String lhdr = hdr.toLowerCase();

			if (_DontProxyHeaders.contains(lhdr))
				continue;
			if (connectionHdr != null && connectionHdr.indexOf(lhdr) >= 0)
				continue;

			if ("content-type".equals(lhdr))
				hasContent = true;

			Enumeration vals = hreq.getHeaders(hdr);
			while (vals.hasMoreElements()) {
				String val = (String) vals.nextElement();
				if (val != null) {
					uc.addRequestProperty(hdr, val);
					//_log.debug("req " + hdr + ": " + val);
					xForwardedFor |= "X-Forwarded-For".equalsIgnoreCase(hdr);
				}
			}
		}

		// Proxy headers
		uc.setRequestProperty("Via", "1.1 (WADI)");
		if (!xForwardedFor)
			uc.addRequestProperty("X-Forwarded-For", hreq.getRemoteAddr());

		// a little bit of cache control
		String cache_control = hreq.getHeader("Cache-Control");
		if (cache_control != null && (cache_control.indexOf("no-cache") >= 0 || cache_control.indexOf("no-store") >= 0))
			uc.setUseCaches(false);

		// customize Connection
		uc.setDoInput(true);

		// TODO - if client has ommitted content-type header, this will be missed - OK ?
		OutputStream toServer=null;
		if (hasContent) {
			uc.setDoOutput(true);

			try {
				InputStream fromClient=hreq.getInputStream(); // IOException
				toServer=uc.getOutputStream(); // IOException
				(client2Server=new Copier(fromClient, toServer, 8192)).runAsynchronously(); // IOException
			} catch (IOException e) {
				_log.info("problem proxying client request to server", e);
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
				hres.setStatus(code, message);
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
		hres.setHeader("Date", null);
		hres.setHeader("Server", null);

		// set response headers
		int h = 0;
		String hdr = uc.getHeaderFieldKey(h);
		String val = uc.getHeaderField(h);
		while (hdr != null || val != null) {
			String lhdr = (hdr != null) ? hdr.toLowerCase() : null;
			if (hdr != null && val != null && !_DontProxyHeaders.contains(lhdr))
				hres.addHeader(hdr, val);

			//_log.debug("res " + hdr + ": " + val);

			h++;
			hdr = uc.getHeaderFieldKey(h);
			val = uc.getHeaderField(h);
		}

//		// TODO - check with Greg that a null header key is impossible -
//		// why did he write the loop like this [above]?
//		String key;
//		for (int i=0; (key=uc.getHeaderFieldKey(i))!=null; i++) {
//			key=key.toLowerCase();
//			String val=uc.getHeaderField(i);
//			if (val!=null && !_DontProxyHeaders.contains(key)) {
//				hres.addHeader(key, val);
//			}
//		}

		hres.addHeader("Via", "1.1 (WADI)");

		// copy server->client
		if (fromServer!=null) {
			try {
				OutputStream toClient=hres.getOutputStream();// IOException
				(server2Client=new Copier(fromServer, toClient, 8192)).runSynchronously();// IOException
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

		int client2ServerTotal=0;
		if (toServer!=null) {
			do {
				try {
					client2Server.getThread().join();
				} catch (InterruptedException ignore) {
					// ignore
				}
			} while (Thread.interrupted());

			{
				try {
					toServer.close(); // IOException
				} catch (IOException e) {
					// well - we did our best...
					_log.info("problem closing server request stream", e);
				}
			}

			client2ServerTotal=client2Server.getTotal();
//			Exception c2se=client2Server.getException();
		}

		long endTime=System.currentTimeMillis();
		long elapsed=endTime-startTime;
		_log.info("in:"+client2ServerTotal+", out:"+server2Client.getTotal()+", status:"+code+", time:"+elapsed+", url:"+url);
	}
}
