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

package org.codehaus.wadi;

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
 * public way to navigate from this class. I.e. a secure HttpSession
 * implementation to pass out to the user.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  HttpSession
  implements javax.servlet.http.HttpSession
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
  public String             getId()                  {return _impl.getId();}

  // Setters
  public void setMaxInactiveInterval(int i)        {_impl.setMaxInactiveInterval(i);}
  public void setAttribute(String key, Object val) {if (val==null){_impl.removeAttribute(key,false);}else{_impl.setAttribute(key, val, false);}}
  public void putValue(String key, Object val)     {setAttribute(key, val);}
  public void removeAttribute(String key)          {_impl.removeAttribute(key, false);}
  public void removeValue(String key)              {removeAttribute(key);}

  protected boolean _valid=true;

  // N.B. called by application-space thread - this must release its
  // shared lock first in order that we can try for an exclusive
  // lock...
  public void
    invalidate()
  {
    HttpSessionImpl impl=((HttpSessionImpl)_impl);
    String id=impl.getRealId();

    RWLock lock=impl.getRWLock();
    boolean acquired=false;

    try
    {
      lock.setPriority(HttpSessionImpl.INVALIDATION_PRIORITY);
      lock.overlap();
      acquired=true;

      String newId=impl.getRealId();
      if (newId!=null && newId.equals(id))
      {
	// no other invalidation thread beat us to it...

	// notification MUST be done synchronously on the request thread
	// because Servlet-2.4 insists that it is given BEFORE
	// invalidation!
	Manager mgr=impl.getWadiManager();
	mgr.releaseImpl(impl);
	mgr._sessionInvalidationCounter++;
	setValid(false);		// do we need this flag ?

	if (_log.isDebugEnabled()) _log.debug(id+": invalidation");
      }
      else
      {
	// another invalidation thread beat us to it - but the net
	// effect is the same...
	_log.warn("lost invalidation race");
      }
    }
    catch (InterruptedException e)
    {
      _log.warn("interrupted during invalidation - session not invalidated", e);
    }
    finally
    {
      if (acquired)
	lock.writeLock().release();

      lock.setPriority(HttpSessionImpl.NO_PRIORITY);
    }

    _impl=null;
  }

  // we should get rid of all of these...

  public boolean isValid(){return _valid;}

  // extra methods... package scope...

  void setValid(boolean valid){_valid=valid;}
  void setLastAccessedTime(long t){_impl.setLastAccessedTime(t);}

  // when an HttpSession comes across the wire it needs to
  // reinitialise the backptr that it's impl holds...

  public String
    toString()
  {
    return "<"+getClass().getName()+": "+_impl+">";
  }

  public void access()
  {_impl.setLastAccessedTime(System.currentTimeMillis());}

  // TODO - not sure if this is the right way to go, but we need it now...
//   protected String _bucketName;
//   public String getBucketName(){return _bucketName;}
//   public void setBucketName(String bucketName){_bucketName=bucketName;}

  // TODO - I don't like adding this to API - but for impls that have
  // come off the wire I don't see much choice...
  public void
    setWadiManager(Manager manager)
    {
      _impl.setWadiManager(manager);
    }
}
