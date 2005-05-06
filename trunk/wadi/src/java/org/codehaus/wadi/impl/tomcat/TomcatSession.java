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
package org.codehaus.wadi.impl.tomcat;

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
import org.codehaus.wadi.DistributableSessionConfig;
import org.codehaus.wadi.impl.DistributableSession;

public class TomcatSession extends DistributableSession implements Session {

    public TomcatSession(DistributableSessionConfig config) {
        super(config);
    }

    public String getInfo() {
        return getClass().getName()+" v2.0";
    }

    public HttpSession getSession() {
        return _wrapper;
    }

    protected transient Manager _manager;
    
    public Manager getManager() {
        return _manager;
    }

    public void setManager(Manager manager) {
        _manager=manager;
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
        // TODO Auto-generated method stub
    }

    public boolean isValid() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setNew(boolean isNew) {
        // TODO Auto-generated method stub
    }

    public void access() {
        // TODO Auto-generated method stub
    }

    public void endAccess() {
        // TODO Auto-generated method stub
    }

    public void recycle() {
        // TODO Auto-generated method stub
    }

    public void expire() {
        // TODO Auto-generated method stub
    }

}
