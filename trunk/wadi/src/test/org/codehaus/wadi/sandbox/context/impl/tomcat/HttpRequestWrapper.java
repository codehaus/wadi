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
package org.codehaus.wadi.sandbox.context.impl.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Wrapper;
import org.apache.tomcat.util.buf.MessageBytes;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class HttpRequestWrapper implements HttpRequest {
	
	protected final HttpRequest _request;
	
	public HttpRequestWrapper(HttpRequest request){_request=request;}
	
	public Connector getConnector()                                  {return _request.getConnector();}
	public Context getContext()                                      {return _request.getContext();}
	public FilterChain getFilterChain()                              {return _request.getFilterChain();}
	public Host getHost()                                            {return _request.getHost();}
	public InputStream getStream()                                   {return _request.getStream();}
	public Iterator getNoteNames()                                   {return _request.getNoteNames();}
	public MessageBytes getContextPathMB()                           {return _request.getContextPathMB();}
	public MessageBytes getDecodedRequestURIMB()                     {return _request.getDecodedRequestURIMB();}
	public MessageBytes getPathInfoMB()                              {return _request.getPathInfoMB();}
	public MessageBytes getRequestPathMB()                           {return _request.getRequestPathMB();}
	public MessageBytes getServletPathMB()                           {return _request.getServletPathMB();}
	public Object getNote(String name)                               {return _request.getNote(name);}
	public Response getResponse()                                    {return _request.getResponse();}
	public ServletInputStream createInputStream() throws IOException {return _request.createInputStream();}
	public ServletRequest getRequest()                               {return _request.getRequest();}
	public Socket getSocket()                                        {return _request.getSocket();}
	public String getAuthorization()                                 {return _request.getAuthorization();}
	public String getDecodedRequestURI()                             {return _request.getDecodedRequestURI();}
	public String getInfo()                                          {return _request.getInfo();}
	public ValveContext getValveContext()                            {return _request.getValveContext();}
	public Wrapper getWrapper()                                      {return _request.getWrapper();}
	public void addCookie(Cookie cookie)                             {_request.addCookie(cookie);}
	public void addHeader(String name, String value)                 {_request.addHeader(name, value);}
	public void addLocale(Locale locale)                             {_request.addLocale(locale);}
	public void addParameter(String name, String[] values)           {_request.addParameter(name, values);}
	public void clearCookies()                                       {_request.clearCookies();}
	public void clearHeaders()                                       {_request.clearHeaders();}
	public void clearLocales()                                       {_request.clearLocales();}
	public void clearParameters()                                    {_request.clearParameters();}
	public void finishRequest() throws IOException                   {_request.finishRequest();}
	public void recycle()                                            {_request.recycle();}
	public void removeNote(String name)                              {_request.removeNote(name);}
	public void setAuthType(String type)                             {_request.setAuthType(type);}
	public void setAuthorization(String authorization)               {_request.setAuthorization(authorization);}
	public void setConnector(Connector connector)                    {_request.setConnector(connector);}
	public void setContentLength(int length)                         {_request.setContentLength(length);}
	public void setContentType(String type)                          {_request.setContentType(type);}
	public void setContext(Context context)                          {_request.setContext(context);}
	public void setContextPath(String path)                          {_request.setContextPath(path);}
	public void setDecodedRequestURI(String uri)                     {_request.setDecodedRequestURI(uri);}
	public void setFilterChain(FilterChain filterChain)              {_request.setFilterChain(filterChain);}
	public void setHost(Host host)                                   {_request.setHost(host);}
	public void setMethod(String method)                             {_request.setMethod(method);}
	public void setNote(String name, Object value)                   {_request.setNote(name, value);}
	public void setPathInfo(String path)                             {_request.setPathInfo(path);}
	public void setProtocol(String protocol)                         {_request.setProtocol(protocol);}
	public void setQueryString(String query)                         {_request.setQueryString(query);}
	public void setRemoteAddr(String remote)                         {_request.setRemoteAddr(remote);}
	public void setRequestURI(String uri)                            {_request.setRequestURI(uri);}
	public void setRequestedSessionCookie(boolean flag)              {_request.setRequestedSessionCookie(flag);}
	public void setRequestedSessionId(String id)                     {_request.setRequestedSessionId(id);}
	public void setRequestedSessionURL(boolean flag)                 {_request.setRequestedSessionURL(flag);}
	public void setResponse(Response response)                       {_request.setResponse(response);}
	public void setScheme(String scheme)                             {_request.setScheme(scheme);}
	public void setSecure(boolean secure)                            {_request.setSecure(secure);}
	public void setServerName(String name)                           {_request.setServerName(name);}
	public void setServerPort(int port)                              {_request.setServerPort(port);}
	public void setServletPath(String path)                          {_request.setServletPath(path);}
	public void setSocket(Socket socket)                             {_request.setSocket(socket);}
	public void setStream(InputStream stream)                        {_request.setStream(stream);}
	public void setUserPrincipal(Principal principal)                {_request.setUserPrincipal(principal);}
	public void setValveContext(ValveContext valveContext)           {_request.setValveContext(valveContext);}
	public void setWrapper(Wrapper wrapper)                          {_request.setWrapper(wrapper);}
}
