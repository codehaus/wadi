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

package org.codehaus.wadi.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.HttpSessionImpl;

public class HttpServletRequest
  implements javax.servlet.http.HttpServletRequest
{
  protected Manager _manager;
  public void setManager(Manager manager){_manager=manager;}
  public Manager getManager(){return _manager;}

  protected String _sessionId;
  public void setSessionId(String sessionId){_sessionId=sessionId;}
  public String getSessionid(){return _sessionId;}

  public HttpSession
    getSession()
  {
    return getSession(false);
  }

  public HttpSession
    getSession(boolean create)
  {
    String realId=_sessionId;	// may later need stripping...

    if (realId==null)
      if (create)
	return _manager.acquireImpl(_manager).getFacade();
      else
	return null;
    else
    {
      HttpSessionImpl impl=_manager.get(realId);
      if (impl==null)
	if (create)
	  return _manager.acquireImpl(_manager, realId).getFacade();
	else
	  return null;
      return impl.getFacade();
    }
  }

  public String
    getRequestedSessionId()
  {
    return _sessionId;
  }

  public Cookie[]     getCookies()                     {return null;}
  public Enumeration  getHeaderNames()                 {return null;}
  public Enumeration  getHeaders(String name)          {return null;}
  public Principal    getUserPrincipal()               {return null;}
  public String       getAuthType()                    {return null;}
  public String       getContextPath()                 {return null;}
  public String       getHeader(String name)           {return null;}
  public String       getMethod()                      {return null;}
  public String       getPathInfo()                    {return null;}
  public String       getPathTranslated()              {return null;}
  public String       getQueryString()                 {return null;}
  public String       getRemoteUser()                  {return null;}
  public String       getRequestURI()                  {return null;}
  public String       getServletPath()                 {return null;}
  public StringBuffer getRequestURL()                  {return null;}
  public boolean      isRequestedSessionIdFromCookie() {return true;}
  public boolean      isRequestedSessionIdFromURL()    {return true;}
  public boolean      isRequestedSessionIdFromUrl()    {return true;}
  public boolean      isRequestedSessionIdValid()      {return true;}
  public boolean      isUserInRole(String role)        {return true;}
  public int          getIntHeader(String name)        {return 0;}
  public long         getDateHeader(String name)       {return 0;}


 public BufferedReader     getReader() throws IOException                                       {return null;}
 public Enumeration        getAttributeNames()                                                  {return null;}
 public Enumeration        getLocales()                                                         {return null;}
 public Enumeration        getParameterNames()                                                  {return null;}
 public Locale             getLocale()                                                          {return null;}
 public Map                getParameterMap()                                                    {return null;}
 public Object             getAttribute(String name)                                            {return null;}
 public RequestDispatcher  getRequestDispatcher(String path)                                    {return null;}
 public ServletInputStream getInputStream() throws IOException                                  {return null;}
 public String             getCharacterEncoding()                                               {return null;}
 public String             getContentType()                                                     {return null;}
 public String             getLocalAddr()                                                       {return null;}
 public String             getLocalName()                                                       {return null;}
 public String             getParameter(String name)                                            {return null;}
 public String             getProtocol()                                                        {return null;}
 public String             getRealPath(String path)                                             {return null;}
 public String             getRemoteAddr()                                                      {return null;}
 public String             getRemoteHost()                                                      {return null;}
 public String             getScheme()                                                          {return null;}
 public String             getServerName()                                                      {return null;}
 public String[]           getParameterValues(String name)                                      {return null;}
 public boolean            isSecure()                                                           {return true;}
 public int                getContentLength()                                                   {return 0;}
 public int                getLocalPort()                                                       {return 0;}
 public int                getRemotePort()                                                      {return 0;}
 public int                getServerPort()                                                      {return 0;}
 public void               removeAttribute(String name)                                         {return;}
 public void               setAttribute(String name, Object o)                                  {return;}
 public void               setCharacterEncoding(String env) throws UnsupportedEncodingException {return;}
}
