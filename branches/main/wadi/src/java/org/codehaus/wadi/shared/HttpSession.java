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

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - we will probably need the following:
// JettySession, JettyDistributableSession, TomcatSession,
// TomcatDistributableSession, PortableSession,
// PortableDistributableSession

/**
 * A simple facade for an AbstractHttpSessionImpl to which there is no
 * public way to navigate from this class. i.e. a secure HttpSession
 * implementation to pass out to the user.
 *
 * @author <a href="mailto:jules@mortbay.com">Jules Gosnell</a>
 * @version 1.0
 */
public class
  HttpSession
  implements javax.servlet.http.HttpSession, Serializable
{
  protected static final Log _log=LogFactory.getLog(HttpSession.class);

  protected AbstractHttpSessionImpl _impl; // when null, this session is invalid...

  public HttpSession(AbstractHttpSessionImpl impl) {_impl=impl;}

  // public i/f

  // Getters
  public long               getCreationTime()        {return _impl.getCreationTime();}
  public long               getLastAccessedTime()    {return _impl.getLastAccessedTime();}
  public ServletContext     getServletContext()      {return _impl.getServletContext();}
  public int                getMaxInactiveInterval() {return _impl.getMaxInactiveInterval();}
  public HttpSessionContext getSessionContext()      {return _impl.getSessionContext();}
  public Object             getAttribute(String key) {return _impl.getAttribute(key);}
  public Object             getValue(String key)     {return _impl.getAttribute(key);}
  public Enumeration        getAttributeNames()      {return _impl.getAttributeNameEnumeration();}
  public String[]           getValueNames()          {return _impl.getAttributeNameStringArray();}
  public boolean            isNew()                  {return _impl.isNew();}

  // Setters
  public void setMaxInactiveInterval(int i)        {_impl.setMaxInactiveInterval(i);}
  public void setAttribute(String key, Object val) {if (val==null){_impl.removeAttribute(key,false);}else{_impl.setAttribute(key, val, false);}}
  public void putValue(String key, Object val)     {setAttribute(key, val);}
  public void removeAttribute(String key)          {_impl.removeAttribute(key, false);}
  public void removeValue(String key)              {removeAttribute(key);}

  protected boolean _invalidated;
  public boolean getInvalidated(){return _invalidated;}
  public void setInvalidated(boolean flag) {_invalidated=flag;}

  public void
    invalidate()
  {
    _log.trace(_impl.getId()+" : explicitly invalidating");
    setInvalidated(true);
  }

  // extra methods... package scope...

  void setLastAccessedTime(long t){_impl.setLastAccessedTime(t);}

  // when an HttpSession comes across the wire it needs to
  // reinitialise the backptr that it's impl holds...

  // TODO - why ?
  private synchronized void
    readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();
    _impl.setFacade(this);
  }

  public String
    toString()
  {
    return "<"+getClass().getName()+": "+_impl+">";
  }

  public boolean isValid()
  {return !_invalidated;}

  public void access()
  {_impl.setLastAccessedTime(System.currentTimeMillis());}

  // TODO - not sure if this is the right way to go, but we need it now...
//   protected String _bucketName;
//   public String getBucketName(){return _bucketName;}
//   public void setBucketName(String bucketName){_bucketName=bucketName;}

  // can this be moved into some policy ?

  public String
    getId()
    {
      String session=_impl.getId();
      String bucket=_impl.getWadiManager().getBucketName();
      if (bucket==null)
	return session;
      else
	return _impl.getWadiManager().getRoutingStrategy().augment(bucket, session); // TODO - cache...
    }
}
