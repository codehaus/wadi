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

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class
  HttpSessionImpl
  extends AbstractHttpSessionImpl
  implements Serializable
{
  public void setCreationTime(long time){_creationTime=time;}
  protected static final Log _log=LogFactory.getLog(HttpSessionImpl.class);

  protected String _id;		// TODO - could be transient :-)
  public String getId(){return _id;}

  protected long _creationTime;	// millis
  public long getCreationTime(){return _creationTime;}

  protected long _lastAccessedTime; // millis
  public long getLastAccessedTime(){return _lastAccessedTime;}
  public void setLastAccessedTime(long l){_lastAccessedTime=l;}

  protected int _maxInactiveInterval; // secs
  public int getMaxInactiveInterval(){return _maxInactiveInterval;}
  public void setMaxInactiveInterval(int i){_maxInactiveInterval=i;}

  protected final Map _attributes=new HashMap();

  // Setters

  public Object
    getAttribute(String name)
  {
    return _attributes.get(name);
  }

  //   public Map
  //     getAttributes()
  //     {
  //       return _attributes.size()==0?_emptyMap:Collections.unmodifiableMap(_attributes);
  //     }

  public Enumeration
    getAttributeNameEnumeration()
  {
    return _attributes.size()==0?_emptyEnumeration:Collections.enumeration(_attributes.keySet());
  }

  public String[]
    getAttributeNameStringArray()
  {
    return _attributes.size()==0?_emptyStringArray:(String[])_attributes.keySet().toArray(new String[_attributes.size()]);
  }

  public Object
    setAttribute(String name, Object value, boolean returnValue)
  {
    // we can be sure that name is non-null, because this will have
    // been checked in our facade...
    Object tmp=_attributes.put(name, value);
    return returnValue?tmp:null;
  }

  public Object
    removeAttribute(String name, boolean returnValue)
  {
    Object tmp=_attributes.remove(name);
    // we could remove the Map if num entries fell back to '0' -
    // but we would probably be creating more work than saving
    // memory..
    return returnValue?tmp:null;
  }

  public void setWadiManager(Manager wadiManager){_wadiManager=wadiManager;}

  protected int _actualMaxInactiveInterval;

  public void
    init(Manager manager, String id, long creationTime, int maxInactiveInterval, int actualMaxInactiveInterval)
  {
    _log.trace(id+": initialising");
    _wadiManager                   =manager;
    _id                        =id;
    _creationTime              =creationTime-1;// think about this... - see isNew() - TODO
    _lastAccessedTime          =_creationTime;
    _maxInactiveInterval       =maxInactiveInterval;
    _actualMaxInactiveInterval =actualMaxInactiveInterval;
    _facade                    =createFacade();
  }

  public void
    destroy()
  {
    _log.trace(_id+": destroying");
    _wadiManager=null;
    _id=null;
    _creationTime=0;
    _lastAccessedTime=0;
    _maxInactiveInterval=0;
    _actualMaxInactiveInterval=0;
    // attributes ? - TODO
    _facade=null;
    _attributes.clear();
  }

  // TODO - stubbed out - implement later
  public ServletContext     getServletContext(){return null;}
  public HttpSessionContext getSessionContext(){return null;}

  // This stuff is only needed if session needs to be migrated whilst
  // the container is active. It should be broken out either into a
  // session impl subclass, or perhaps a separate LockManager...

  protected transient ReadWriteLock _rwlock;

  // TODO- if I try to initialise these fields outside a ctor,
  // deserialisation results in them being empty - why ?
  public
    HttpSessionImpl()
  {
    super();
    _rwlock=new ReaderPreferenceReadWriteLock();
    _log.info("HttpSessionImpl ctor..."); // TODO
  }

  public Sync getApplicationLock()  {return _rwlock.readLock();} // allows concurrent app threads
  public Sync getContainerLock() {return _rwlock.writeLock();} // container threads exclude all others

  public boolean
    hasTimedOut(long currentTimeMillis)
  {
    return ((currentTimeMillis-_lastAccessedTime)>_maxInactiveInterval*1000);
  }; // TODO - actualMII

  public String
    toString()
  {
    return "<"+getClass().getName()+": "+_attributes+">";
  }

  // why do I have to go to such lengths to initialise these
  // transient fields ?
  private void readObject(java.io.ObjectInputStream in)
    throws java.io.IOException, ClassNotFoundException
  {
    in.defaultReadObject();
    _rwlock=new ReaderPreferenceReadWriteLock();
  }

  public void setId(String id){_id=id;}

  public abstract HttpSession createFacade();
}

