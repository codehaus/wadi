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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;
import org.codehaus.wadi.shared.Manager;

interface
  HttpSessionGetters
{
  public long               getCreationTime();
  public String             getId();
  public long               getLastAccessedTime();
  public ServletContext     getServletContext();
  public int                getMaxInactiveInterval();
  public HttpSessionContext getSessionContext();
  public Object             getAttribute(String key);
  public Enumeration        getAttributeNameEnumeration();
  public String[]           getAttributeNameStringArray();
  public boolean            isNew();
}

interface
  HttpSessionSetters
{
  public void               setLastAccessedTime(long l);
  public void               setMaxInactiveInterval(int i);
  public Object             setAttribute(String key, Object val, boolean returnVal);
  public Object             removeAttribute(String key, boolean returnVal);
  //  public void               invalidate();
}

public abstract class
  AbstractHttpSessionImpl
  implements HttpSessionGetters, HttpSessionSetters, java.io.Serializable
{
  public AbstractHttpSessionImpl(){}

  protected transient javax.servlet.http.HttpSession _facade;
  public javax.servlet.http.HttpSession getFacade(){return _facade;}
  public void setFacade(javax.servlet.http.HttpSession facade){_facade=facade;}

  protected static Map         _emptyMap         =Collections.EMPTY_MAP;
  protected static Enumeration _emptyEnumeration =Collections.enumeration(Collections.EMPTY_LIST);
  protected static String[]    _emptyStringArray =new String[0];

  protected transient Manager _wadiManager;
  public Manager getWadiManager(){return _wadiManager;}

  // this is a clever way to do this - but if different nodes have slightly different times, is it possible that we will get the wrong answer back ?
  public boolean isNew(){return getLastAccessedTime()==getCreationTime();} // think about this...
}
