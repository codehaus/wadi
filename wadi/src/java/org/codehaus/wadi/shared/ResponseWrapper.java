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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class
  ResponseWrapper
  extends javax.servlet.http.HttpServletResponseWrapper
{
  public ResponseWrapper(HttpServletResponse response) {super(response);}

  private HttpServletResponse _getHttpServletResponse() {return (HttpServletResponse) super.getResponse();}

  // definitely need fixing...
  public void    addCookie(Cookie cookie)                         {_getHttpServletResponse().addCookie(cookie);}
  public String  encodeRedirectURL(String url)                    {return _getHttpServletResponse().encodeRedirectURL(url);}
  public String  encodeRedirectUrl(String url)                    {return _getHttpServletResponse().encodeRedirectUrl(url);}
  public void    setHeader(String name, String value)             {_getHttpServletResponse().setHeader(name, value);}
  public void    addHeader(String name, String value)             {_getHttpServletResponse().addHeader(name, value);}

  // need to think about these...
  public boolean containsHeader(String name)                      {return _getHttpServletResponse().containsHeader(name);}
  public void    setDateHeader(String name, long date)            {_getHttpServletResponse().setDateHeader(name, date);}
  public void    addDateHeader(String name, long date)            {_getHttpServletResponse().addDateHeader(name, date);}
  public void    setIntHeader(String name, int value)             {_getHttpServletResponse().setIntHeader(name, value);}
  public void    addIntHeader(String name, int value)             {_getHttpServletResponse().addIntHeader(name, value);}

//   public String  encodeURL(String url)                            {return _getHttpServletResponse().encodeURL(url);}
//   public void    setStatus(int sc)                                {_getHttpServletResponse().setStatus(sc);}
//   public void    setStatus(int sc, String sm)                     {_getHttpServletResponse().setStatus(sc, sm);}
//   public void    sendError(int sc, String msg) throws IOException {_getHttpServletResponse().sendError(sc, msg);}
//   public void    sendError(int sc) throws IOException             {_getHttpServletResponse().sendError(sc);}
//   public void    sendRedirect(String location) throws IOException {_getHttpServletResponse().sendRedirect(location);}
}
