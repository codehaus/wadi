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
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import org.codehaus.wadi.Manager;

public class HttpServletRequest
  implements javax.servlet.http.HttpServletRequest
{
  protected final Manager _manager;
  protected final String  _id;

  public
    HttpServletRequest(Manager manager, String id)
  {
    _manager=manager;
    _id=id;
  }

  public HttpSession
    getSession()
  {
    return getSession(false);
  }

  public HttpSession
    getSession(boolean create)
  {
    //    HttpSession session=_manager.get(id);
    return null;		// TODO ??
  }

  public String
    getRequestedSessionId()
  {
    return _id;
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
