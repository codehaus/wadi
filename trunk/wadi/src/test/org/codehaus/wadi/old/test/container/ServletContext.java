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

package org.codehaus.wadi.old.test.container;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 * Used to help stub out container impl, so that we can instantiate
 * and test Managers outside Tomcat/Jetty...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  ServletContext
  implements javax.servlet.ServletContext
{
  protected final                        Map _attributes=Collections.synchronizedMap(new HashMap());

  public    javax.servlet.ServletContext getContext(String uripath)                            {return null;}
  public    int                          getMajorVersion()                                     {return 2;}
  public    int                          getMinorVersion()                                     {return 4;}
  public    String                       getMimeType(String file)                              {return null;}
  public    Set                          getResourcePaths(String path)                         {return null;}
  public    URL                          getResource(String path) throws MalformedURLException {return null;}
  //  public    InputStream                  getResourceAsStream(String path)                      {return getClass().getClassLoader().getResourceAsStream(path);}
  public    InputStream                  getResourceAsStream(String path)                      {return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);}
  public    RequestDispatcher            getRequestDispatcher(String path)                     {return null;}
  public    RequestDispatcher            getNamedDispatcher(String name)                       {return null;}
  public    Servlet                      getServlet(String name) throws ServletException       {return null;}
  public    Enumeration                  getServlets()                                         {return null;}
  public    Enumeration                  getServletNames()                                     {return null;}
  public    void                         log(String msg)                                       {}
  public    void                         log(Exception exception, String msg)                  {}
  public    void                         log(String message, Throwable throwable)              {}
  public    String                       getRealPath(String path)                              {return null;}
  public    String                       getServerInfo()                                       {return null;}
  public    String                       getInitParameter(String name)                         {return null;}
  public    Enumeration                  getInitParameterNames()                               {return null;}
  public    Object                       getAttribute(String name)                             {return _attributes.get(name);}
  public    Enumeration                  getAttributeNames()                                   {return null;}
  public    void                         setAttribute(String name, Object object)              {_attributes.put(name, object);}
  public    void                         removeAttribute(String name)                          {}
  public    String                       getServletContextName()                               {return "wadi";}
}
