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

package org.codehaus.wadi.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.IO;
import org.mortbay.util.InetAddrPort;

/**
 * EXPERIMENTAL Proxy servlet.
 *
 * @author gregw
 *
 */
public class HttpProxy {
	private int _tunnelTimeoutMs = 3000;
	protected Log _log = LogFactory.getLog(HttpProxy.class);

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

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse)
	 */
	public void proxy(ServletRequest req, ServletResponse res, URL url)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		if ("CONNECT".equalsIgnoreCase(request.getMethod())) {
			handleConnect(request, response);
		} else {
//			String uri = request.getRequestURI();
//			if (request.getQueryString() != null)
//				uri += "?" + request.getQueryString();
//			URL url = new URL(request.getScheme(), request.getServerName(),
//					request.getServerPort(), uri);
//
//			url = new URL("http://localhost:8080/test/admin");
			_log.info("URL=" + url);

			URLConnection connection = url.openConnection();
			connection.setAllowUserInteraction(false);

			// Set method
			HttpURLConnection http = null;
			if (connection instanceof HttpURLConnection) {
				http = (HttpURLConnection) connection;
				http.setRequestMethod(request.getMethod());
				http.setInstanceFollowRedirects(false);
			}

			// check connection header
			String connectionHdr = request.getHeader("Connection");
			if (connectionHdr != null) {
				connectionHdr = connectionHdr.toLowerCase();
				if (connectionHdr.equals("keep-alive")
						|| connectionHdr.equals("close"))
					connectionHdr = null;
			}

			// copy headers
			boolean xForwardedFor = false;
			boolean hasContent = false;
			Enumeration enm = request.getHeaderNames();
			while (enm.hasMoreElements()) {
				// TODO could be better than this!
				String hdr = (String) enm.nextElement();
				String lhdr = hdr.toLowerCase();

				if (_DontProxyHeaders.contains(lhdr))
					continue;
				if (connectionHdr != null && connectionHdr.indexOf(lhdr) >= 0)
					continue;

				if ("content-type".equals(lhdr))
					hasContent = true;

				Enumeration vals = request.getHeaders(hdr);
				while (vals.hasMoreElements()) {
					String val = (String) vals.nextElement();
					if (val != null) {
						connection.addRequestProperty(hdr, val);
						_log.info("req " + hdr + ": " + val);
						xForwardedFor |= "X-Forwarded-For"
								.equalsIgnoreCase(hdr);
					}
				}
			}

			// Proxy headers
			connection.setRequestProperty("Via", "1.1 (WADI)");
			if (!xForwardedFor)
				connection.addRequestProperty("X-Forwarded-For", request
						.getRemoteAddr());

			// a little bit of cache control
			String cache_control = request.getHeader("Cache-Control");
			if (cache_control != null
					&& (cache_control.indexOf("no-cache") >= 0 || cache_control
							.indexOf("no-store") >= 0))
				connection.setUseCaches(false);

			// customize Connection

			try {
				connection.setDoInput(true);

				// do input thang!
				InputStream in = request.getInputStream();
				if (hasContent) {
					connection.setDoOutput(true);
					IO.copy(in, connection.getOutputStream());
				}

				// Connect
				connection.connect();
			} catch (Exception e) {
				_log.info("proxy", e);
			}

			InputStream proxy_in = null;

			// handler status codes etc.
			int code = 500;
			if (http != null) {
				proxy_in = http.getErrorStream();

				code = http.getResponseCode();
				response.setStatus(code, http.getResponseMessage());
				_log.info("response = " + http.getResponseCode());
			}

			if (proxy_in == null) {
				try {
					proxy_in = connection.getInputStream();
				} catch (Exception e) {
					_log.info("stream", e);
					proxy_in = http.getErrorStream();
				}
			}

			// clear response defaults.
			response.setHeader("Date", null);
			response.setHeader("Server", null);

			// set response headers
			int h = 0;
			String hdr = connection.getHeaderFieldKey(h);
			String val = connection.getHeaderField(h);
			while (hdr != null || val != null) {
				String lhdr = hdr != null ? hdr.toLowerCase() : null;
				if (hdr != null && val != null
						&& !_DontProxyHeaders.contains(lhdr))
					response.addHeader(hdr, val);

				_log.info("res " + hdr + ": " + val);

				h++;
				hdr = connection.getHeaderFieldKey(h);
				val = connection.getHeaderField(h);
			}
			response.addHeader("Via", "1.1 (WADI)");

			// Handle
			if (proxy_in != null)
				IO.copy(proxy_in, response.getOutputStream());

		}
	}

	/* ------------------------------------------------------------ */
	public void handleConnect(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String uri = request.getRequestURI();

		_log.info("CONNECT: " + uri);

		InetAddrPort addrPort = new InetAddrPort(uri);

		//if
		// (isForbidden(HttpMessage.__SSL_SCHEME,addrPort.getHost(),addrPort.getPort(),false))
		//{
		//    sendForbid(request,response,uri);
		//}
		//else
		{
			InputStream in = request.getInputStream();
			OutputStream out = response.getOutputStream();

			Socket socket = new Socket(addrPort.getInetAddress(), addrPort
					.getPort());
			_log.info("Socket: " + socket);

			response.setStatus(200);
			response.setHeader("Connection", "close");
			response.flushBuffer();

			System.err.println(response);

			_log.info("out<-in");
			IO.copyThread(socket.getInputStream(), out);
			_log.info("in->out");
			IO.copy(in, socket.getOutputStream());
		}
	}
}
