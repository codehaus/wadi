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
import org.apache.catalina.Manager;
import org.apache.catalina.SessionListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class
  HttpSessionImpl
  extends org.codehaus.wadi.shared.HttpSessionImpl
  implements org.apache.catalina.Session
{
  //--------//
  // Tomcat //
  //--------//

  //  public HttpSession(){super();}

  public String getInfo(){return "org.codehaus.wadi.tomcat.HttpSession v1.0";}

  protected transient String _authType;
  public String getAuthType(){return _authType;}
  public void setAuthType(String authType){_authType=authType;}

  protected transient org.apache.catalina.Manager _manager;
  public org.apache.catalina.Manager getManager(){return _manager;};
  public void setManager(org.apache.catalina.Manager manager){_manager=manager;}

  protected transient Principal _principal;
  public Principal getPrincipal(){return _principal;}
  public void setPrincipal(Principal principal){_principal=principal;}

  protected transient final Map _notes=Collections.synchronizedMap(new HashMap());
  public Object getNote(String name){return _notes.get(name);}
  public void setNote(String name, Object value){_notes.put(name, value);}
  public void removeNote(String name){_notes.remove(name);}
  public Iterator getNoteNames(){return _notes.keySet().iterator();} // TODO - is iterator thread safe ?

  public void addSessionListener(SessionListener listener){} // TODO
  public void removeSessionListener(SessionListener listener){}	// TODO

  public javax.servlet.http.HttpSession getSession(){return _facade;}

  public void expire(){}//{_facade.setInvalidated(true);}
  public void setValid(boolean valid){}//{_facade.setInvalidated(!valid);}
  public boolean isValid(){return true;}//{return !_facade.getInvalidated();}

  public void recycle(){}	// TODO

  protected boolean _new=true;
  public void setNew(boolean nuw){_new=nuw;}

  public void access(){setLastAccessedTime(System.currentTimeMillis());}

  public void endAccess(){}	// TODO - what does this do ?

  public org.codehaus.wadi.shared.HttpSession createFacade(){return new HttpSession(this);}
}
