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

package  org.codehaus.wadi.shared;

import java.util.Enumeration;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

// how clever do I have to be to completely mask the fact that we are
// adding routing info to the sessionid...?

// TODO - mess with relevent methods - cookies, headers, paths etc and
// remove the rest...

public class
  RequestWrapper
  extends javax.servlet.http.HttpServletRequestWrapper
{
 public  RequestWrapper(HttpServletRequest request) {super(request);}

 private HttpServletRequest _getHttpServletRequest() {return (HttpServletRequest) super.getRequest();}

 // TODO - these definitely need fixing...
 public  Cookie[]           getCookies()                     {return _getHttpServletRequest().getCookies();}
 public  Enumeration        getHeaders(String name)          {return _getHttpServletRequest().getHeaders(name);}
 public  String             getHeader(String name)           {return _getHttpServletRequest().getHeader(name);}
 public  String             getPathInfo()                    {return _getHttpServletRequest().getPathInfo();}
 public  String             getPathTranslated()              {return _getHttpServletRequest().getPathTranslated();}
 public  String             getRequestURI()                  {return _getHttpServletRequest().getRequestURI();}
 public  StringBuffer       getRequestURL()                  {return _getHttpServletRequest().getRequestURL();}
 public  String             getRequestedSessionId()          {return _getHttpServletRequest().getRequestedSessionId();}
 public  String             getServletPath()                 {return _getHttpServletRequest().getServletPath();}

 // TODO - these may need fixing - depends whether they are implemented in terms of above or not...
 public  HttpSession        getSession()                     {return _getHttpServletRequest().getSession();}
 public  HttpSession        getSession(boolean create)       {return _getHttpServletRequest().getSession(create);}
 public  boolean            isRequestedSessionIdFromCookie() {return _getHttpServletRequest().isRequestedSessionIdFromCookie();}
 public  boolean            isRequestedSessionIdFromURL()    {return _getHttpServletRequest().isRequestedSessionIdFromURL();}
 public  boolean            isRequestedSessionIdFromUrl()    {return _getHttpServletRequest().isRequestedSessionIdFromUrl();}
 public  boolean            isRequestedSessionIdValid()      {return _getHttpServletRequest().isRequestedSessionIdValid();}

 // should be OK
 // public  Enumeration        getHeaderNames()                 {return _getHttpServletRequest().getHeaderNames();}
 // public  long               getDateHeader(String name)       {return _getHttpServletRequest().getDateHeader(name);}
 // public  int                getIntHeader(String name)        {return _getHttpServletRequest().getIntHeader(name);}

//  public  String             getAuthType()                    {return _getHttpServletRequest().getAuthType();}
//  public  String             getMethod()                      {return _getHttpServletRequest().getMethod();}
//  public  String             getContextPath()                 {return _getHttpServletRequest().getContextPath();}
//  public  String             getQueryString()                 {return _getHttpServletRequest().getQueryString();}
//  public  String             getRemoteUser()                  {return _getHttpServletRequest().getRemoteUser();}
//  public  boolean            isUserInRole(String role)        {return _getHttpServletRequest().isUserInRole(role);}
//  public  Principal          getUserPrincipal()               {return _getHttpServletRequest().getUserPrincipal();}
}
