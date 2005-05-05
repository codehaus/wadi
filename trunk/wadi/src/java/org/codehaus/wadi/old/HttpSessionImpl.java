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

package org.codehaus.wadi.old;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;

/**
 * Common HttpSessionImpl fn-ality
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class
  HttpSessionImpl
  extends AbstractHttpSessionImpl
  implements SerializableContent, Serializable // temporary
{
  public abstract javax.servlet.http.HttpSession createFacade();

  public void setCreationTime(long time){_creationTime=time;}
  protected static final Log _log=LogFactory.getLog(HttpSessionImpl.class);

  protected String _id;		// TODO - could be transient :-)
  public String getId(){return _wadiManager.getRoutingStrategy().augment(_id);}	// TODO - cache ?
  public String getRealId(){return _id;}

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
    if (_log.isTraceEnabled()) _log.trace(id+": initialising");
    _wadiManager               =manager;
    _id                        =id;
    _creationTime              =creationTime;
    _lastAccessedTime          =_creationTime;
    _maxInactiveInterval       =maxInactiveInterval;
    _actualMaxInactiveInterval =actualMaxInactiveInterval;
    _facade=createFacade();
  }

  public void
    destroy()
  {
    if (_log.isTraceEnabled()) _log.trace(_id+": destroying");
    _wadiManager=null;
    _id=null;
    _creationTime=0;
    _lastAccessedTime=0;
    _maxInactiveInterval=0;
    _actualMaxInactiveInterval=0;
    _attributes.clear();
    _facade=null;		// more secure to drop this - S.O. may have taken a reference...

    // TODO - reuse _attributes, _rwlock and _facade
  }

  // TODO - stubbed out - implement later
  public ServletContext     getServletContext(){return _wadiManager.getServletContext();}
  public HttpSessionContext getSessionContext(){return _wadiManager.getSessionContext();}

  // This stuff is only needed if session needs to be migrated whilst
  // the container is active. It should be broken out either into a
  // session impl subclass, or perhaps a separate LockManager...

  public final static int INVALIDATION_PRIORITY=4;
  public final static int TIMEOUT_PRIORITY=3;
  public final static int EMMIGRATION_PRIORITY=2;
  public final static int EVICTION_PRIORITY=1;
  public final static int NO_PRIORITY=0;

  protected final int MAX_PRIORITY=INVALIDATION_PRIORITY;

  protected final transient RWLock _rwlock=new RWLock(MAX_PRIORITY);

  // TODO- if I try to initialise these fields outside a ctor,
  // deserialisation results in them being empty - why ?
  public
    HttpSessionImpl()
  {
    super();
  }

  public Sync getApplicationLock()  {return _rwlock.readLock();} // allows concurrent app threads
  public Sync getContainerLock() {return _rwlock.writeLock();} // container threads exclude all others
  public RWLock getRWLock() {return _rwlock;}

  public void
    acquireApplicationLock()
  {
    for (boolean locked=false;!locked;)
    {
      try
      {
	_rwlock.readLock().acquire();
	locked=true;
      }
      catch(InterruptedException ignore)
      {
          // ignored
      }
    }
  }

  public void
    acquireContainerLock()
  {
    for (boolean locked=false;!locked;)
    {
      try
      {
	_rwlock.writeLock().acquire();
	locked=true;
      }
      catch(InterruptedException ignore)
      {
          // ignored
      }
    }
  }

  public boolean
    hasTimedOut(long currentTimeMillis)
  {
    return ((currentTimeMillis-_lastAccessedTime)>_maxInactiveInterval*1000);
  } // TODO - actualMII

  public String
    toString()
  {
    return "<"+getClass().getName()+": "+getRealId()+":"+_attributes+">";
  }

  private void readObject(java.io.ObjectInputStream in)
    throws java.io.IOException, ClassNotFoundException
  {
    assert false;
  }

  private void writeObject(java.io.ObjectOutputStream os)
    throws java.io.IOException
  {
    assert false;
  }

  public void
    readContent(java.io.ObjectInput is)
    throws java.io.IOException, ClassNotFoundException
  {
    _id                        =(String)is.readObject();
    _creationTime              =is.readLong();
    _lastAccessedTime          =is.readLong();
    _maxInactiveInterval       =is.readInt();
    _actualMaxInactiveInterval =is.readInt();

    // read in map contents manually...
    assert _attributes.size()==0;
    for (int i=is.readInt();i>0;i--)
      _attributes.put(is.readObject(), is.readObject());

    // initialise non-final transients ?
    _facade=createFacade();
  }

  public void
    writeContent(java.io.ObjectOutput os)
    throws java.io.IOException
  {
    os.writeObject(_id);
    os.writeLong(_creationTime);
    os.writeLong(_lastAccessedTime);
    os.writeInt(_maxInactiveInterval);
    os.writeInt(_actualMaxInactiveInterval);

    // write out attributes - forget the map itself - unecessary
    // overhead and may change type/version....
    os.writeInt(_attributes.size());
    for (Iterator i=_attributes.entrySet().iterator(); i.hasNext();)
    {
      Map.Entry e=(Map.Entry)i.next();
      os.writeObject(e.getKey());
      os.writeObject(e.getValue());
    }

    // TODO - we could write attrs into a byte[] and only deserialise it
    // lazily on the other side, as we do for attributes...
  }

  public void setId(String id){_id=id;}

  protected transient SynchronizedBoolean _beingInvalidated=new SynchronizedBoolean(false);
  public SynchronizedBoolean getBeingInvalidated(){return _beingInvalidated;}
}
