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

package org.codehaus.wadi.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - there's a little more to it than this:-)

// TODO - conform to HTTP rfc for proxying
// TODO - how do we indicate that we are proxying a secure connection - ask Greg

public class
  HttpProxy
{
  protected static Log _log = LogFactory.getLog(HttpProxy.class);

  public static class
    StreamCopy
    implements Runnable
    {
      protected final InputStream  _is;
      protected final OutputStream _os;

      public
	StreamCopy(InputStream is, OutputStream os)
	{
	  _is=is;
	  _os=os;
	}

      public void
	run()
	{
	  byte[] b=new byte[1024];	// TODO - what size should this be ?
	  int bytes;

	  try
	  {
	    while ((bytes=_is.read(b))>-1)
	      _os.write(b, 0, bytes);
	    _os.flush();
	  }
	  catch (IOException e)
	  {
	    e.printStackTrace();	// TODO - log properly
	  }
	}
    }

  //   public static void
  //     proxy2(String hostname, int port, HttpServletRequest request, HttpServletResponse response)
  //     {
  //       HttpConnection conn=new HttpConnection(hostname, port); // TODO - consider protocol...

  //       String scheme="http";	// TODO - hardwired
  //       String line=
  // 	request.getMethod()+
  // 	" "+
  // 	request.getContextPath()+
  // 	request.getServletPath()+
  // 	(request.isRequestedSessionIdFromURL()?";jsessionid="+request.getRequestedSessionId():"")+
  // 	"";

  //       _log.trace("proxying request to: "+scheme+"://"+hostname+":"+port);

  //       try
  //       {
  // 	conn.open();

  // 	OutputStream os=conn.getRequestOutputStream();
  // 	InputStream  is=conn.getResponseInputStream();

  // 	PrintWriter pw=new PrintWriter(os);

  // 	_log.trace(line);
  // 	pw.println(line);

  // 	for (Enumeration e=request.getHeaderNames(); e.hasMoreElements();)
  // 	{
  // 	  String header=(String)e.nextElement();
  // 	  String values="";
  // 	  for (Enumeration f=request.getHeaders(header); f.hasMoreElements();)
  // 	  {
  // 	    Object value=f.nextElement();
  // 	    values+=(values.length()>0?",":"")+value;
  // 	  }

  // 	  _log.trace(header+": "+values);
  // 	  pw.println(header+": "+values);
  // 	}

  // 	_log.trace("");
  // 	pw.println();
  // 	pw.flush();

  // 	Thread req=new Thread(new StreamCopy(request.getInputStream(), os));
  // 	Thread res=new Thread(new StreamCopy(is, response.getOutputStream()));

  // 	req.start();
  // 	res.start();
  // 	req.join();
  // 	res.join();
  // 	os.close();
  // 	is.close();

  // 	conn.close();
  //       }
  //       catch (IOException e)
  //       {
  // 	_log.warn("could not hook up request io streams", e);
  //       }
  //       catch (InterruptedException e)
  //       {
  // 	_log.warn("unexpected interrupt whilst proxying request", e);
  //       }
  //     }

  public static boolean
    proxy(String hostname, int port, HttpServletRequest request, HttpServletResponse response)
    {
      HttpMethod method=null;

      String m=request.getMethod();

      _log.trace("method="+m);

      // why doesn't commons-httpclient provide an abstract factory to
      // create HttpMethods from e.g. "GET", "HEAD" etc ?

      if (m.equals("GET"))
	method=new GetMethod();
      else
      {
	_log.warn("method NYI: "+m); // TODO
	return false;
      }

      String cpath=request.getContextPath()+request.getServletPath();
      if (request.isRequestedSessionIdFromURL())
	cpath+=";jsessionid="+request.getRequestedSessionId();

      _log.info("cpath::"+cpath);
      method.setPath(cpath);
      method.setQueryString(request.getQueryString());

      //      (request.isRequestedSessionIdFromURL()?";jsessionid="+request.getRequestedSessionId():"")

      // headers
      for (Enumeration e=request.getHeaderNames(); e.hasMoreElements();)
      {
	String header=(String)e.nextElement();
	String values="";
	for (Enumeration f=request.getHeaders(header); f.hasMoreElements();)
	{
	  Object value=f.nextElement();
	  values+=(values.length()>0?",":"")+value;
	}

	_log.trace("req - Header: "+header+": "+values);
	method.setRequestHeader(header, values);
      }

      // cookies...

      // DOH! - an org.apache.commons.httpclient.Cookie is NOT a
      // javax.servlet.http.Cookie - and it looks like the two don't
      // map onto each other without data loss...
      HttpState state=new HttpState();
      javax.servlet.http.Cookie[] cookies=request.getCookies();
      if (cookies!=null)
      {
 	for (int i=0;i<cookies.length;i++)
 	{
 	  javax.servlet.http.Cookie c=cookies[i];
 	  String domain=c.getDomain();
	  if (domain==null) domain="192.168.0.2"; // TODO - tmp test
 	  String path=c.getPath();
	  if (path==null) path=request.getContextPath(); // fix for Jetty
 	  _log.trace("PATH: value="+path+" length="+(path==null?0:path.length()));
 	  Cookie cookie=new Cookie(domain, c.getName(), c.getValue(), path, c.getMaxAge(), c.getSecure()); // TODO - sort out domain
 	  _log.trace("Cookie: "+cookie.getDomain()+","+ cookie.getName()+","+ cookie.getValue()+","+ cookie.getPath()+","+ cookie.getExpiryDate()+","+ cookie.getSecure());
 	  state.addCookie(cookie);
 	  _log.trace("Cookie: "+cookie.toString());
 	}
      }

      String scheme=request.getScheme();

      _log.trace("proxying request to: "+scheme+"://"+hostname+":"+port);

      try
      {
	HttpClient client=new HttpClient();
	HostConfiguration hc=new HostConfiguration();
	hc.setHost(hostname, port);
	client.executeMethod(hc, method, state);
	//client.executeMethod(hc, method);
      }
      catch (IOException e)	// TODO
      {
	_log.warn("problem proxying connection:",e);
      }

      Header[] headers=method.getResponseHeaders();
      for (int i=0;i<headers.length;i++)
      {
	String header=headers[i].toExternalForm();
	int colon=header.indexOf(':');
	String name=header.substring(0, colon).trim();
	//	if (name.equals("Set-Cookie")) continue; // ignore
	String value=header.substring(colon+1, header.length()).trim();

	_log.trace("res - Header: "+name+": "+value);

	if (!name.equals("Transfer-Encoding")) // Jetty will chuck entire response if I pass this in...
	  response.setHeader(name, value);
      }

      // Cookies
      Cookie[] cookee=state.getCookies();
      for (int i=0;i<cookee.length;i++)
	_log.trace("res - Cookie: "+cookee[i]+", domain="+cookee[i].getDomain()+", path="+cookee[i].getPath());

      try
      {
	//	assert !response.isCommitted();
	response.flushBuffer();

	if (method instanceof EntityEnclosingMethod)
	  ((EntityEnclosingMethod)method).setRequestBody(request.getInputStream());

	InputStream is=method.getResponseBodyAsStream();
	if (is!=null)
	{
	  Thread res=new Thread(new StreamCopy(is, response.getOutputStream()));
	  res.start();
	  res.join();
	  is.close();
	}
	else
	{
	  _log.warn("null response body - failed request - is "+hostname+";"+port+" available ?");
	}

	_log.trace("response successfully transferred");

	return true;
      }
      catch (IOException e)
      {
	_log.warn("problem proxying connection:",e);
      }
      catch (InterruptedException e)
      {
	_log.warn("problem proxying connection:",e);
      }

      return false;
    }
}
