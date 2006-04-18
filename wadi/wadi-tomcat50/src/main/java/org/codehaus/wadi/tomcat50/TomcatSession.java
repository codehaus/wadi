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
package org.codehaus.wadi.tomcat50;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DistributableSessionConfig;
import org.codehaus.wadi.impl.DistributableSession;

/**
 * Interestingly, in Tomcat a Session is a facade (for internal use) over an HttpSession (for external use), but
 * in WADI we have a Session (rich implementation for internal use) which is facaded by an HttpSession (constrains
 * available API for external use). Thus, our implementation is Tomcat's facade and vice versa ! Confused ? I was...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TomcatSession extends DistributableSession implements Session {

  protected static final Log _log = LogFactory.getLog(TomcatSession.class);

  public TomcatSession(DistributableSessionConfig config) {
    super(config);
  }

  public void destroy() {
	  _authType=null;
	  _principal=null;
	  _notes.clear();
	  _listeners.clear();
	  try {
		  super.destroy();
	  } catch (Exception e) {
		  _log.warn("unexpected problem destroying session", e);
	  }
  }

  public void setId(String id) {
    // we set our id via init() method...
    throw new UnsupportedOperationException("WADI does not support the use of Session.setId(String)");
  }

  public void setCreationTime(long creationTime) {
    // we set our creationTime via init() method...
    throw new UnsupportedOperationException("WADI does not support the use of Session.setCreationTime(long)");
  }

  public String getInfo() {
    return getClass().getName()+" v2.0";
  }

  public HttpSession getSession() {
    return _wrapper;
  }

  public Manager getManager() {
    return (Manager)_config;
  }

  public void setManager(Manager manager) {
    // initialises backptr to Manager - we already have this as we
    // were given it as our _config on construction...

    // called by Manager, but we don't call it and do not expect to be called...
    throw new UnsupportedOperationException("WADI does not support the use of Session.setManager(Manager)");
  }

  protected transient String _authType;

  public String getAuthType() {
    return _authType;
  }

  public void setAuthType(String authType) {
    _authType=authType;
  }

  protected transient Principal _principal;

  public Principal getPrincipal() {
    return _principal;
  }

  public void setPrincipal(Principal principal) {
    _principal=principal;
  }

  protected transient final Map _notes=Collections.synchronizedMap(new HashMap());

  public void setNote(String name, Object value) {
    _notes.put(name, value);
  }

  public Object getNote(String name) {
    return _notes.get(name);
  }

  public void removeNote(String name) {
    _notes.remove(name);
  }

  public Iterator getNoteNames() {
    return _notes.keySet().iterator();
  }

  protected List _listeners=Collections.synchronizedList(new ArrayList());

  public List getSessionListeners(){return _listeners;} // not Tomcat - used by aspect

  public void addSessionListener(SessionListener listener) {
    _listeners.add(listener);
  }

  public void removeSessionListener(SessionListener listener) {
    _listeners.remove(listener);
  }

  public void setValid(boolean isValid) {
    // WADI invalidates a Session by releasing its Id. Thus
    // we don't need to maintain a seperate flag in every Session.

    // Tomcat only seems to call this from Manager and Session -
    // we don't use it and are not expecting it to be called.
    throw new UnsupportedOperationException("WADI does not support the use of Session.setValid(boolean)");
  }

  public boolean isValid() {
    // Tomcat checks for expiry in this method and uses it to validate
    // Session access...

    // WADI takes the approach that if a Session is in the Container
    // at the time a request arrives, it would be foolish to invalidate
    // it - in fact, you would not be able to, because the visiting thread
    // would already have a lock...

    // I don't think we use this method...
    throw new UnsupportedOperationException("WADI does not support the use of Session.isValid()");
  }

  public void setNew(boolean isNew) {
    // WADI tests whether creation and lastAccessed time are the same
    // to determine whether a session has been joined... It saves carrying
    // an extra flag in every session.

    // Tomcat only seems to call this from Manager and Session - we do not use it...
    throw new UnsupportedOperationException("WADI does not support the use of Session.setNew(boolean)");
  }

  public void access() {
    // since there is no way to externally query whether the session is currently
    // being accessed, these must be notifications for the session's internal use.
    // WADI does not currently require them, so they remain unimplemented.

    // if/when WADI implements e.g. writing sessions out at the end of an event group,
    // it will be done by acquiring a exclusive lock on the session, so won't need
    // this aproach...

    // doesn't need to do anything - wasted cycles
  }

  public void endAccess() {
    // used to maintain an accessCount - not needed (see above)
    // used to unset 'new' flag - not needed - see setNew()

    // doesn't need to do anything - wasted cycles
  }

  public void recycle() {
    // used by some Tomcat Managers to recycle Session objects
    // Our Manager uses alternate means - we are not expecting to be called...

    throw new UnsupportedOperationException("WADI does not support the use of Session.recycle()");
  }

  public void expire() {
    // only seems to be called by Manager
    // We don't use it and don't expect it to be called...

    throw new UnsupportedOperationException("WADI does not support the use of Session.expire()");
  }

}
