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

package org.codehaus.wadi.tomcat;

import java.security.Principal;
import java.util.Iterator;
import java.util.List;
import org.apache.catalina.SessionListener;

// what's the point of having this facade that looks like an
// HttpSession, if it has all these extra methods on it - should they
// be in package scope ?

public class
  HttpSession
  extends org.codehaus.wadi.shared.HttpSession
  implements org.apache.catalina.Session
{
  HttpSession(HttpSessionImpl impl) {super(impl);}

  public List getSessionListeners()                           {return getImpl().getSessionListeners();}
  public void addSessionListener(SessionListener sl)          {getImpl().addSessionListener(sl);}
  public void removeSessionListener(SessionListener sl)       {getImpl().removeSessionListener(sl);}

  public org.apache.catalina.Manager getManager()             {return getImpl().getManager();}
  public void setManager(org.apache.catalina.Manager manager) {getImpl().setManager(manager);}

  public void setNew(boolean isNew)                           {getImpl().setNew(isNew);}

  public Principal getPrincipal()                             {return getImpl().getPrincipal();}
  public void setPrincipal(Principal principal)               {getImpl().setPrincipal(principal);}

  public Object getNote(String key)                           {return getImpl().getNote(key);}
  public void setNote(String key , Object note)               {getImpl().setNote(key, note);}
  public void removeNote(String key)                          {getImpl().removeNote(key);}
  public Iterator getNoteNames()                              {return getImpl().getNoteNames();}

  public String getAuthType()                                 {return getImpl().getAuthType();}
  public void setAuthType(String at)                          {getImpl().setAuthType(at);}

  public String getInfo()                                     {return getImpl().getInfo();}

  public javax.servlet.http.HttpSession getSession()          {return getImpl().getSession();} // TODO - DOH!

  public void endAccess()                                     {getImpl().endAccess();}
  public void expire()                                        {getImpl().expire();}
  public void recycle()                                       {getImpl().recycle();}
  public void setCreationTime(long t)                         {getImpl().setCreationTime(t);}
  public void setId(String id)                                {getImpl().setId(id);}
  public void setValid(boolean valid)                         {getImpl().setValid(valid);}

  protected HttpSessionImpl getImpl(){return (HttpSessionImpl)_impl;} // TODO - ugly
}
