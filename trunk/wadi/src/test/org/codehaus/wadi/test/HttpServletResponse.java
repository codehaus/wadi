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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

public class HttpServletResponse
  implements javax.servlet.http.HttpServletResponse
{
 public String              encodeRedirectURL(String url)                    {return null;}
 public String              encodeRedirectUrl(String url)                    {return null;}
 public String              encodeURL(String url)                            {return null;}
 public String              encodeUrl(String url)                            {return null;}
 public boolean             containsHeader(String name)                      {return false;}
 public void                addCookie(Cookie cookie)                         {}
 public void                addDateHeader(String name, long date)            {}
 public void                addHeader(String name, String value)             {}
 public void                addIntHeader(String name, int value)             {}
 public void                sendError(int sc) throws IOException             {}
 public void                sendError(int sc, String msg) throws IOException {}
 public void                sendRedirect(String location) throws IOException {}
 public void                setDateHeader(String name, long date)            {}
 public void                setHeader(String name, String value)             {}
 public void                setIntHeader(String name, int value)             {}
 public void                setStatus(int sc)                                {}
 public void                setStatus(int sc, String sm)                     {}

 public Locale              getLocale()                                      {return null;}
 public PrintWriter         getWriter() throws IOException                   {return null;}
 public ServletOutputStream getOutputStream() throws IOException             {return null;}
 public String              getCharacterEncoding()                           {return null;}
 public String              getContentType()                                 {return null;}
 public boolean             isCommitted()                                    {return false;}
 public int                 getBufferSize()                                  {return 0;}
 public void                flushBuffer() throws IOException                 {}
 public void                reset()                                          {}
 public void                resetBuffer()                                    {}
 public void                setBufferSize(int size)                          {}
 public void                setCharacterEncoding(String charset)             {}
 public void                setContentLength(int len)                        {}
 public void                setContentType(String type)                      {}
 public void                setLocale(Locale loc)                            {}
}
