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

package org.codehaus.wadi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.IrrecoverableException;
import org.codehaus.wadi.ProxyingException;
import org.codehaus.wadi.RecoverableException;

// My choice of proxy - still suboptimal - servlet spec imposes a very clumsy API
// for copying the headers out of the HttpServletRequest (a proprietary solution
// would be faster), but at least the Cookie headers can be copied straight across
// (see CommonsHttpProxy where they cannot...).

// This does not yet support e.g. WebDav methods like PROPFIND etc...

/**
 * HttpProxy implementation based on java.net.HttpURLConnection
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @author <a href="mailto:gregw@mortbay.com">Greg Wilkins</a>
 * @version $Revision$
 */
public class StandardHttpProxy extends AbstractHttpProxy {

	protected static final Log _log=LogFactory.getLog(StandardHttpProxy.class);

	public StandardHttpProxy(String sessionPathParamKey) {
		super(sessionPathParamKey);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse)
	 */
	public void proxy(InetSocketAddress location, HttpServletRequest req, HttpServletResponse res) throws ProxyingException {

	  String uri=getRequestURI(req);
	  String qs=req.getQueryString();
	  if (qs!=null) {
	    uri=new StringBuffer(uri).append("?").append(qs).toString();
	  }

	  URL url=null;
	  try {
	    url=new URL("http", location.getAddress().getHostAddress(), location.getPort(), uri);
            if (_log.isTraceEnabled()) _log.trace("proxying to: "+url);
	  } catch (MalformedURLException e) {
	    if (_log.isWarnEnabled()) _log.warn("bad proxy url: "+url, e);
	    throw new IrrecoverableException("bad proxy url", e);
	  }

	  long startTime=System.currentTimeMillis();

	  HttpURLConnection huc=null;
	  String m=req.getMethod();
	  try {
	    huc=(HttpURLConnection)url.openConnection(); // IOException
	    huc.setRequestMethod(m); // ProtocolException
	  } catch (ProtocolException e) {
	    if (_log.isWarnEnabled()) _log.warn("unsupported http method: "+m, e);
	    throw new IrrecoverableException("unsupported HTTP method: "+m, e);
	  } catch (IOException e) {
	    if (_log.isWarnEnabled()) _log.warn("proxy IO problem", e);
	    throw new RecoverableException("could not open proxy connection", e);
	  }

	  huc.setAllowUserInteraction(false);
	  huc.setInstanceFollowRedirects(false);

	  // check connection header
	  // TODO - this might need some more time: see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
	  String connectionHdr = req.getHeader("Connection"); // TODO - what if there are multiple values ?
	  if (connectionHdr != null) {
	    connectionHdr = connectionHdr.toLowerCase();
	    if (connectionHdr.equals("keep-alive")|| connectionHdr.equals("close"))
	      connectionHdr = null; // TODO  ??
	  }

	  // copy headers - inefficient, but we are constrained by servlet API
	  {
	    for (Enumeration e=req.getHeaderNames(); e.hasMoreElements();) {
	      String hdr = (String) e.nextElement();
	      String lhdr = hdr.toLowerCase();

	      if (_DontProxyHeaders.contains(lhdr))
		continue;
	      if (connectionHdr != null && connectionHdr.indexOf(lhdr) >= 0) // what is going on here ?
		continue;
	      // HTTP/1.1 proxies MUST parse the Connection header field before a message is forwarded and, for each connection-token in this field, remove any header field(s) from the message with the same name as the connection-token. Connection options are signaled by the presence of a connection-token in the Connection header field, not by any corresponding additional header field(s), since the additional header field may not be sent if there are no parameters associated with that connection option
	      if (_WADI_IsSecure.equals(hdr)) // don't worry about case - we should be the only one messing with this header...
		continue; // strip this out - we may be being spoofed

	      for (Enumeration f=req.getHeaders(hdr); f.hasMoreElements();) {
		String val=(String)f.nextElement();
		if (val!=null) {
		  huc.addRequestProperty(hdr, val);
		}
	      }
	    }
	  }

	  // content ?
	  boolean hasContent=false;
	  {
	    int contentLength=0;
	    String tmp=huc.getRequestProperty("Content-Length");
	    if (tmp!=null) {
	      try {
		contentLength=Integer.parseInt(tmp);
	      } catch (NumberFormatException ignore) {
		// ignore
	      }
	    }

	    if (contentLength>0)
	      hasContent=true;
	    else
	      hasContent=(huc.getRequestProperty("Content-Type")!=null);
	  }

	  // proxy
	  {
	    huc.addRequestProperty("Via", "1.1 "+req.getLocalName()+":"+req.getLocalPort()+" \"WADI\""); // TODO - should we be giving out personal details ?
	    huc.addRequestProperty("X-Forwarded-For", req.getRemoteAddr()); // adds last link in request chain...
	    // String tmp=uc.getRequestProperty("Max-Forwards"); // TODO - do we really need to bother with this ?
	  }

	  // cache-control
	  {
	    String cacheControl=huc.getRequestProperty("Cache-Control");
	    if (cacheControl!=null && (cacheControl.indexOf("no-cache")>=0 || cacheControl.indexOf("no-store")>=0))
	      huc.setUseCaches(false);
	  }

	  // confidentiality
	  {
	    if (req.isSecure()) {
	      huc.addRequestProperty(_WADI_IsSecure, req.getLocalAddr().toString());
	    }

	    // at the other end, if this header is present we must :

	    // wrap the request so that req.isSecure()=true, before processing...
	    // mask the header - so it is never seen by the app.

	    // the code for the other end should live in this class.

	    // this code should also confirm that it not being spoofed by confirming that req.getRemoteAddress() is a cluster member...
	  }
	  // customize Connection
	  huc.setDoInput(true);

	  // client->server
	  int client2ServerTotal=0;
	  {
	    if (hasContent) {
	      huc.setDoOutput(true);

	      OutputStream toServer=null;
	      try {
		InputStream fromClient=req.getInputStream(); // IOException
		toServer=huc.getOutputStream(); // IOException
		client2ServerTotal=copy(fromClient, toServer, 8192);
	      } catch (IOException e) {
		new IrrecoverableException("problem proxying client request to server", e);
	      } finally {
		if (toServer!=null) {
		  try {
		    toServer.close(); // IOException
		  } catch (IOException e) {
		    _log.warn("problem closing server request stream", e);
		  }
		}
	      }
	    }
	  }

	  // Connect
	  try {
	    huc.connect(); // IOException
	  } catch (IOException e) {
	    if (_log.isWarnEnabled()) _log.warn("proxy connection problem: "+url, e);
	    throw new RecoverableException("could not connect to proxy target", e);
	  }

	  InputStream fromServer=null;

	  // handler status codes etc.
	  int code=0;
	  if (huc==null) {
	    try {
	      fromServer = huc.getInputStream(); // IOException
	    } catch (IOException e) {
	      if (_log.isWarnEnabled()) _log.warn("proxying problem", e);
	      throw new IrrecoverableException("problem acquiring client output", e);
	    }
	  } else {
	    code=502;
	    //			String message="Bad Gateway: could not read server response code or message";
	    try {
	      code=huc.getResponseCode(); // IOException
	      //				message=huc.getResponseMessage(); // IOException
	    } catch (IOException e) {
	      if (_log.isWarnEnabled()) _log.warn("proxying problem", e);
	      throw new IrrecoverableException("problem acquiring http server response code/message", e);
	    } finally {
	      //				res.setStatus(code, message); - deprecated
	      res.setStatus(code);
	    }

	    if (code<400) {
	      // 1XX:continue, 2XX:successful, 3XX:multiple-choices...
	      try {
		fromServer=huc.getInputStream(); // IOException
	      } catch (IOException e) {
		if (_log.isWarnEnabled()) _log.warn("proxying problem", e);
		throw new IrrecoverableException("problem acquiring http client output", e);
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
	    String hdr = huc.getHeaderFieldKey(h);
	    String val = huc.getHeaderField(h);
	    while (hdr != null || val != null) {
	      String lhdr = (hdr != null) ? hdr.toLowerCase() : null;
	      if (hdr != null && val != null && !_DontProxyHeaders.contains(lhdr))
		res.addHeader(hdr, val);

	      // if (_log.isDebugEnabled()) _log.debug("res " + hdr + ": " + val);

	      h++;
	      hdr = huc.getHeaderFieldKey(h);
	      val = huc.getHeaderField(h);
	    }
	  } else {
	    // TODO - is it a bug in Jetty that I have to start my loop at 1 ? or that key[0]==null ?
	    // Try this inside Tomcat...
	    String key;
	    for (int i=1; (key=huc.getHeaderFieldKey(i))!=null; i++) {
	      key=key.toLowerCase();
	      String val=huc.getHeaderField(i);
	      if (val!=null && !_DontProxyHeaders.contains(key)) {
		res.addHeader(key, val);
	      }
	    }
	  }

	  // do we need another Via header in the response...

	  // server->client
	  int server2ClientTotal=0;
	  {
	    if (fromServer!=null) {
	      try {
		OutputStream toClient=res.getOutputStream();// IOException
		server2ClientTotal+=copy(fromServer, toClient, 8192);// IOException
	      } catch (IOException e) {
		if (_log.isWarnEnabled()) _log.warn("proxying problem", e);
		throw new IrrecoverableException("problem proxying server response back to client", e);
	      } finally {
		try {
		  fromServer.close();
		} catch (IOException e) {
		  // well - we did our best...
		  _log.warn("problem closing server response stream", e);
		}
	      }
	    }
	  }

	  huc.disconnect();

	  long endTime=System.currentTimeMillis();
	  long elapsed=endTime-startTime;
	  if (_log.isDebugEnabled()) _log.debug("in:"+client2ServerTotal+", out:"+server2ClientTotal+", status:"+code+", time:"+elapsed+", url:"+url);
	}
}
