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

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.plugins.FilePassivationStrategy;
import org.codehaus.wadi.plugins.NoRoutingStrategy;
import org.codehaus.wadi.plugins.RelativeEvictionPolicy;
import org.codehaus.wadi.plugins.TomcatIdGenerator;
import org.mortbay.xml.XmlConfiguration; // do I really want to do this ?

// TODO - replace some form of location discovery protocol

// TODO - refactor distributed GC election algorithm

// TODO - use assert() to confirm that correct type of threads are
// taking correct locks

// TODO - what are we going to do about background threads? reinstate
// the Security Manager...

// when we become cluster aware the node performing GC of evicted
// sessions should be elected according to a round-robin policy
// (perhaps) so that it is not always the same node ding it. A does it
// once, then passes the baton to B etc... This is probably a job for
// Geronimo's clustering layer.

// TODO - needs a destroy() method in which to call e.g. _sessionListeners.clear(); _attributeListeners.clear();

// NOTES

// who will be responsible for GC-ing timed out passivated sessions?
// it depends on the migration policy... - but it should be the
// sessions primary owner - does the migration involve a change of
// ownership ?

// how do we know that they were not owned by a container that crashed and might still be valid ?

// we need a IdGenerator interface and really secure impl

// once evicted, no proxy is kept for a session. It must be loaded,
// on-demand, by the PassivationStrategy, or else we must redirect a
// request to it elsewhere...

// TODO - setLastAccessedTime should have an aspect which mods the
// time by e.g. 5 seconds - so that we do fewer updates...

// all major components need MBean interfaces for Jetty, TC, Geronimo
// & ?JBoss?.

// TODO - Greg needs to integrate this properly with Jetty, so that
// Manager.initialize(Handler) gets called at the correct time. Then
// the Manager can insall the Filter and other required components.

// TODO - we need to consider how we are going to ensure that request
// child threads terminate before their parent...

// TODO - we need a JdbcPassivationStrategy, EjbPassivationStrategy,
// JavaSpacesPassivationStrategy etc..

// TODO - we need to figure out the differences between a migration
// and replication policy and be able to use/mix the same underlying
// policies for both. Migration policies remove their off-node copy
// whilst session is loaded locally, replication policies do not...

// In a Clustered situation Only one Manager should be doing GC of the
// migration policy - perhaps the migration policy itself should
// provide a means of election ? i.e. all nodes try to lock a file and
// only the successful locker does gc. On each sweep, the other nodes
// try to acquire the lock, in case the winner last time died etc...

public abstract class
  Manager
{
  protected final Log _log=LogFactory.getLog(getClass());

  // Tomcat
  //   public Session createEmptySession();
  //   public Session createSession();

  // Jetty
  //   public HttpSession newHttpSession(HttpServletRequest request);

  //-----//
  // Map //
  //-----//

  protected final Map _local=new ConcurrentReaderHashMap();

  public HttpSessionImpl
    put(String id, HttpSessionImpl session)
  {
    return (HttpSessionImpl)_local.put(id, session);
  }

  protected final Map _migrating  =new HashMap();

  public boolean
    owns(String id)
  {
    if (_local.containsKey(id))
      return true;
    else
      synchronized(_migrating){return _migrating.containsKey(id);} // TODO - we should probably wait for lock here...
  }

  public HttpSessionImpl
    get(String id)
  {
    HttpSessionImpl impl=getLocalSession(id);

    if (impl==null)
      impl=getRemoteSession(id);

    return impl;
  }

  protected HttpSessionImpl
    getLocalSession(String id)
  {
    return (HttpSessionImpl)_local.get(id);
  }

  protected HttpSessionImpl
    getRemoteSession(String id)
  {
    HttpSessionImpl impl=null;

    // maybe session is remote and requires migrating here from disc or another jvm?
    Mutex oldMigrationLock=null; // in case another thread has already initiated migration
    Mutex newMigrationLock=new Mutex(); // in case we initiate migration - may not be used :-(
    try
    {
      newMigrationLock.acquire(); // do this outsode synchronized{}

      // this first block checks that the session is not already
      // local, or in the process of being migrated to this node. If
      // not, it places a lock, indicating that a migration is now
      // underway, in case other threads come looking for the same
      // session. This lock will be released when the migration is
      // finished.
      synchronized (_migrating)
      {
	// we will have already checked to see if session is local,
	// but not within this lock. We test outside to reduce
	// unecessary contention on this lock, then again within it,
	// in case this session has been successfully migrated to this
	// node since we last tested...
	if ((impl=getLocalSession(id))!=null)
	  return impl;

	if ((oldMigrationLock=(Mutex)_migrating.get(id))==null)
	{
	  _migrating.put(id, newMigrationLock);
	}
	else
	{
	  // migration is already under way...
	}
      }

      // the second block either waits for an existing migration to
      // finish, or implements a new migration, depending on the
      // result of the first block.
      if (oldMigrationLock!=null)
      {
	// There is already a migration underway. The lock will be
	// released by the migration thread when the migration is
	// complete. We will wait for this to occur, then try finding
	// the session again.

	try
	{
	  oldMigrationLock.acquire();	// wait for current migration to finish
	  return getLocalSession(id); // return resulting session
	}
	catch (InterruptedException e)
	{
	  _log.warn("unexpected interruption whilst waiting for existing migration to finish", e);
	  return null;
	}
	finally
	{
	  oldMigrationLock.release();
	}
      }
      else
      {
	// We are initiating a migration.

	// If a passivation store has been enabled, we may find the
	// session in it and load it....
	if (_passivationStrategy!=null)
	  impl=_passivationStrategy.activate(id);

	// If a migration policy has been enabled, we may request it
	// from another node.

//  	if (impl==null)
//  	{
//  	  ManagerProxy proxy=locate(id);
//  	  if (proxy!=null)
//  	    impl=proxy.relocateSession(_local, _migrating, id);
//  	}

	// make newly acquired session impl available to container...
	if (impl!=null)
	{
	  impl.setWadiManager(this);
	  impl.setFacade(createFacade(impl));
	  _local.put(id, impl);
	}

	// the migration is complete and the session is now local -
	// remove the lock and release it - see finally{}
	_migrating.remove(id);	// protect properly from UnknownHostException
      }
    }
    catch (InterruptedException e)
    {
      _log.warn("unexpected interruption acquiring new migration lock");
    }
    finally
    {
      newMigrationLock.release(); // migration complete (if it occurred)
    }

    return impl;
  }

  public HttpSessionImpl
    remove(String id)
  {
    // could we be trying to remove a session that is passivated....
    return (HttpSessionImpl)_local.remove(id);
  }

  public Collection
    values()
  {
    return _local.values();
  }

  //---------//
  // Manager //
  //---------//

  // TODO - granularity of synchronisation ?
  protected int _maxInactiveInterval=60*30; // seconds
  public int getMaxInactiveInterval(){return _maxInactiveInterval;}
  public void setMaxInactiveInterval(int seconds){_maxInactiveInterval=seconds;}

  protected boolean _distributable=false;
  public void setDistributable(boolean distributable){_distributable=distributable;}
  public boolean getDistributable(){return _distributable;}

  protected EvictionPolicy _evictionPolicy;
  public EvictionPolicy getEvictionPolicy(){return _evictionPolicy;}
  public void setEvictionPolicy(EvictionPolicy policy){_evictionPolicy=policy;}

  protected PassivationStrategy _passivationStrategy;
  public PassivationStrategy getPassivationStrategy(){return _passivationStrategy;}
  public void setPassivationStrategy(PassivationStrategy policy){_passivationStrategy=policy;}

  //-----------//
  // Lifecycle //
  //-----------//

  protected boolean _running=false;
  protected ClassLoader _loader;
  public ClassLoader getLoader(){return _loader;}
  public void setLoader(ClassLoader loader){_loader=loader;}

  //Jetty
  //   public void start() throws Exception {}
  //   public void stop() throws InterruptedException {}
  // Tomcat
  //   public void start() throws LifecycleException {}
  //   public void stop() throws LifecycleException {}


  protected String _configurationResource="WEB-INF/wadi-web.xml";

  public synchronized void
    start()
      throws Exception
  {
    _log.debug("starting");
    _log.info("WADI-1.0rc1 - Web Application Distribution Infrastructure (http://wadi.codehaus.org)");

    ServletContext ctx=getServletContext();

    // load config
    try
    {
      InputStream is=ctx.getResourceAsStream(_configurationResource);
      if (is!=null)
      {
	new XmlConfiguration(is).configure(this);
	_log.trace("configured from: "+_configurationResource);
      }
    }
    catch (Exception e)
    {
      _log.warn("problem configuring from: "+_configurationResource, e);
    }

    // TODO - is putting ourselves in an attribute a security risk ?
    ctx.setAttribute(Manager.class.getName(), this);
    _loader=Thread.currentThread().getContextClassLoader();
    //      System.setSecurityManager(new SecurityManager(System.getSecurityManager()));// TODO

    // default migration policy
    if (_passivationStrategy==null) _passivationStrategy=new FilePassivationStrategy(new File("/tmp/wadi"));
    // default eviction policy
    if (_evictionPolicy==null) _evictionPolicy=new RelativeEvictionPolicy(0.5F);
    //default id generation strategy
    if (_idGenerator==null) _idGenerator=new TomcatIdGenerator();
    // default routing strategy
    if (_routingStrategy==null) _routingStrategy=new NoRoutingStrategy();

    if (_autoLocationAddress==null) setAutoLocationAddress("228.5.6.7");

    _locationServer=new LocationServer(getAutoLocationAddress(),
				       getAutoLocationPort(),
				       5000L,	// 5 seconds
				       InetAddress.getLocalHost(), // TODO - resolve
				       getHttpPort()
				       );
    _locationClient=new LocationClient(getAutoLocationAddress(), getAutoLocationPort(), 5000L);
    _locationServer.start();
    _migrationServer=new MigrationService.Server(_local, _migrating);
    _migrationServer.start();
    _running=true;
    _log.debug("started");
  }

  public synchronized boolean
    isStarted()
  {
    return _running;
  }

  public synchronized void
    stop()
      throws Exception
  {
    _log.debug("stopping");
    _running=false;
    _locationServer.stop();
    _locationServer=null;
    _migrationServer.stop();
    _migrationServer=null;

    // what about housekeeping thread ?

    //    System.setSecurityManager(((SecurityManager)System.getSecurityManager()).getDelegate());
    _loader=null;
    _log.debug("stopped");
  }

  //----------------------------------------
  // housekeeping...
  //----------------------------------------

  // Tomcat will run this as it's backgroundProcess() - I assume with the correct CCL?
  // Jetty's SessionManager will manage it as part of it's lifecycle.

  /**
   * Called periodically to do the housekeeping (session invalidation,
   * migration, etc...)
   *
   */
  public void
    housekeeper()		// pass in eviction decision parameters
    throws InterruptedException
  {
    _log.trace("housekeeping beginning");
    boolean canEvict=_passivationStrategy!=null && _evictionPolicy!=null;
    long currentTime=System.currentTimeMillis();
    Collection list=new LinkedList();

    // this is probably the wrong way to do this - load all timed out
    // sessions back into memory, so that they are included in normal housekeeping...

    _passivationStrategy.standDown();
    // TODO - we are very likely to be reelected here...
    _passivationStrategy.standUp();
    if (_passivationStrategy.isElected())
      for (Iterator i=_passivationStrategy.findTimedOut(currentTime, list).iterator(); i.hasNext();)
	get((String)i.next());
    list.clear();		// could be reused on next iteration...

    for (Iterator i=_local.values().iterator(); i.hasNext();)
    {
      HttpSessionImpl impl=(HttpSessionImpl)i.next();
      boolean acquired=impl.getContainerLock().attempt(0);
      if (!acquired) // if this fails, is it a guarantee that there are active threads ? TODO
      {
	// we could not get an exclusive lock on this session,
	// therefore there are associated threads active within the
	// container - do not touch it...
	_log.trace(impl.getId()+" : failed to acquire exclusive access for housekeeping");
	continue;
      }
      else
      {
	//	  _log.trace(impl.getId()+" : testing for invalidation or eviction");
	// we are free to do as we will :-)
	try
	{
	  if (((org.codehaus.wadi.shared.HttpSession)impl.getFacade()).getInvalidated()) // explicitly invalidated
	  {
	    _log.trace(impl.getId()+" : marking as invalidation candidate (explicit invalidation)");
	    getReadySessionPool().put(impl);
	    continue;
	  }

	  if (impl.hasTimedOut(currentTime))	// implicitly invalidated via time-out
	  {
	    _log.trace(impl.getId()+" : marking as invalidation candidate (implicit time out)");
	    ((org.codehaus.wadi.shared.HttpSession)impl.getFacade()).setInvalidated(true);
	    getReadySessionPool().put(impl);
	    continue;
	  }

	  if (canEvict && _evictionPolicy.evictable(currentTime, impl))
	  {
	    _log.trace(impl.getId()+" : marking as migration candidate");
	    // should this be done asynchronously via another Channel ?
	    if (_passivationStrategy.passivate(impl))
	      _local.remove(impl.getId());
	    continue;
	  }
	}
	finally
	{
	  impl.getContainerLock().release();
	}
      }
    }
    _log.trace("housekeeping ended");
  }

  //----------------------------------------
  // session lifecycle...
  //----------------------------------------

  public String getSpecificationVersion(){return "2.4";} // TODO - read from DD

  /**
   * Put a session impl that we have finished with, back into the
   * ready pool for recycling...
   *
   * @param session a <code>javax.servlet.http.HttpSession</code> value
   */
  protected void
    destroyHttpSession(HttpSessionImpl impl)
  {
    getReadySessionPool().put(impl);
  }

  public abstract class SessionPool
    implements Channel
  {
    public abstract Object take();
    public Object poll(long millis){return take();}

    public abstract void put(Object o);
    public boolean offer(Object o, long millis){put(o);return true;}

    public Object peek(){return null;}
  }

  protected abstract SessionPool getBlankSessionPool();
  protected abstract void setBlankSessionPool(SessionPool pool);

  protected IdGenerator _idGenerator;
  public IdGenerator getIdGenerator(){return _idGenerator;}
  public void setIdGenerator(IdGenerator generator){_idGenerator=generator;}

  //----------------------------------------
  // Listeners

  // These lists are only modified at webapp [un]deployment time, by a
  // single thread, so although read by multiple threads whilst the
  // Manager is running, need no synchronization...

  protected final List _sessionListeners  =new ArrayList();
  public List getSessionListeners(){return _sessionListeners;}
  protected final List _attributeListeners=new ArrayList();
  public List getAttributeListeners(){return _attributeListeners;}

  public synchronized void
    addEventListener(EventListener listener)
      throws IllegalArgumentException, IllegalStateException
  {
    if (isStarted())
      throw new IllegalStateException("EventListeners must be added before a Session Manager starts");

    boolean known=false;
    if (listener instanceof HttpSessionAttributeListener)
    {
      _log.debug("adding HttpSessionAttributeListener: "+listener);
      _attributeListeners.add(listener);
      known=true;
    }
    if (listener instanceof HttpSessionListener)
    {
      _log.debug("adding HttpSessionListener: "+listener);
      _sessionListeners.add(listener);
      known=true;
    }

    if (!known)
      throw new IllegalArgumentException("Unknown EventListener type "+listener);
  }

  public synchronized void
    removeEventListener(EventListener listener)
      throws IllegalStateException
  {
    boolean known=false;

    if (isStarted())
      throw new IllegalStateException("EventListeners may not be removed while a Session Manager is running");

    if (listener instanceof HttpSessionAttributeListener)
    {
      _log.debug("removing HttpSessionAttributeListener: "+listener);
      known|=_attributeListeners.remove(listener);
    }
    if (listener instanceof HttpSessionListener)
    {
      _log.debug("removing HttpSessionListener: "+listener);
      known|=_sessionListeners.remove(listener);
    }

    if (!known)
      _log.warn("EventListener not registered: "+listener);
  }

  //--------------------
  // session events
  //--------------------

  public void
    notifySessionCreated(javax.servlet.http.HttpSession session)
  {
    int n=_sessionListeners.size();
    if (n>0)
    {
      _log.debug(session.getId()+" : notifying session creation");
      HttpSessionEvent event = new HttpSessionEvent(session);

      for(int i=0;i<n;i++)
	notifySessionCreated((HttpSessionListener)_sessionListeners.get(i), event);

      event=null;
    }
  }

  public void
    notifySessionDestroyed(javax.servlet.http.HttpSession session)
  {
    int n=_sessionListeners.size();
    if (n>0)
    {
      _log.debug(session.getId()+" : notifying session destruction");
      HttpSessionEvent event = new HttpSessionEvent(session);

      for(int i=0;i<n;i++)
	notifySessionDestroyed((HttpSessionListener)_sessionListeners.get(i), event);

      event=null;
    }
  }

  protected void
    notifySessionAttributeAdded(javax.servlet.http.HttpSession session, String key, Object val)
    {
      int n=_attributeListeners.size();
      if (n>0)
      {
	_log.debug(session.getId()+" : notifying attribute addition : "+key+" : null --> "+val);

	HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, key, val);
	for(int i=0;i<n;i++)
	  notifySessionAttributeAdded((HttpSessionAttributeListener)_attributeListeners.get(i), event);

	event=null;
      }
    }

  protected void
    notifySessionAttributeReplaced(javax.servlet.http.HttpSession session, String key, Object oldVal, Object newVal)
    {
      int n=_attributeListeners.size();
      if (n>0)
      {
	_log.debug(session.getId()+" : notifying attribute replacement : "+key+" : "+oldVal+" --> "+newVal);
	HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, key, oldVal);

	for(int i=0;i<n;i++)
	  notifySessionAttributeReplaced((HttpSessionAttributeListener)_attributeListeners.get(i), event);

	event=null;
      }
    }

  protected void
    notifySessionAttributeRemoved(javax.servlet.http.HttpSession session, String key, Object val)
    {
      int n=_attributeListeners.size();
      if (n>0)
      {
	_log.debug(session.getId()+" : notifying attribute removal : "+key+" : "+val+" --> null");
	HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, key, val);

	for(int i=0;i<n;i++)
	  notifySessionAttributeRemoved((HttpSessionAttributeListener)_attributeListeners.get(i), event);

	event=null;
      }
    }

  // these are split out so that they may be easily aspected (event notifications for tomcat...
  public void notifySessionCreated(HttpSessionListener listener, HttpSessionEvent event){listener.sessionCreated(event);}
  public void notifySessionDestroyed(HttpSessionListener listener, HttpSessionEvent event){listener.sessionDestroyed(event);}
  public void notifySessionAttributeAdded(HttpSessionAttributeListener listener, HttpSessionBindingEvent event){listener.attributeAdded(event);}
  public void notifySessionAttributeRemoved(HttpSessionAttributeListener listener, HttpSessionBindingEvent event){listener.attributeRemoved(event);}
  public void notifySessionAttributeReplaced(HttpSessionAttributeListener listener, HttpSessionBindingEvent event){listener.attributeReplaced(event);}

  // this should be abstracted into a ReplicatingManager...

  public void
    replicate(String id, Method method, Object[] args)
  {
    _log.debug("replicating delta: "+id+" -> "+method);

    // this method should be invoked on a dynamic proxy which
    // represents the cluster to which the deltas are being sent...

    // lots of clever synch/asynch stuff, batching, compression etc
    // can be done here or in the replication medium...
  }

  public void
    invoke(String id, Method method, Object[] args)
  {
    // look up a session and invoke the given method and args upon
    // it...
    _log.debug("invoking delta: "+id+" -> "+method);
  }

  protected abstract HttpSession createFacade(HttpSessionImpl impl);

  public void
    notifyRequestEnd(String id)
  {
    _log.trace(id+": request end");
  }

  public void
    notifyRequestGroupEnd(String id)
  {
    _log.trace(id+": request group end");
  }

  public boolean getUsingRequestGroups(){return true;}

  //----------------------------------------
  // autolocation API
  //----------------------------------------

  protected InetAddress _autoLocationAddress=null;
  public InetAddress getAutoLocationAddress(){return _autoLocationAddress;}
  public void setAutoLocationAddress(InetAddress address){_autoLocationAddress=address;}

  public void
    setAutoLocationAddress(String address)
  {
    try
    {
      setAutoLocationAddress(InetAddress.getByName(address));
    }
    catch (Exception e)
    {
      _log.warn("could not resolve address: "+address, e);
    }
  }

  protected int _autoLocationPort=6789;
  public int getAutoLocationPort(){return _autoLocationPort;}
  public void setAutoLocationPort(int port){_autoLocationPort=port;}

  protected int _autoLocationTimeout=5;	// seconds
  public int getAutoLocationTimeout(){return _autoLocationTimeout;}
  public void setAutoLocationTimeout(int timeout){_autoLocationTimeout=timeout;}

  //----------------------------------------
  // location
  //----------------------------------------

  protected LocationServer _locationServer;
  protected LocationClient _locationClient;

  public ManagerProxy
    locate(String id)
  {
    String location=_locationClient.run("org.codehaus.wadi"+","+ "locate"+","+id);

    try
    {
      return new ManagerProxy(id, location);
    }
    catch (Exception  e)
    {
      _log.warn("bad location response", e);
      return null;
    }
  }

  class
    LocationClient
    extends DiscoveryService.Client
  {
    public
      LocationClient(InetAddress address, int port, long timeout)
    {
      super(address, port, timeout);
    }
  }

  class
    LocationServer
    extends DiscoveryService.Server
  {
    protected long        _timeout=2000; // TODO - 0does not quit properly
    protected InetAddress _httpIpAddress;
    protected int         _httpPort;

    public
      LocationServer(InetAddress serverIpAddress, int serverPort,
		     long timeout, InetAddress httpIpAddress, int httpPort)
    {
      super(serverIpAddress, serverPort);
      _httpIpAddress=httpIpAddress;
      _httpPort=httpPort;
    }

    public String
      process(String request)
    {
      String response=null;

      String params[]=request.split(",");

      if (params.length==3 &&
	  params[0].equals("org.codehaus.wadi") &&
	  params[1].equals("locate") &&
	  _local.containsKey(params[2]))
      {
	response=
	  "org.codehaus.wadi"+","+
	  "locate"+","+
	  params[2]+","+
	  _httpIpAddress.getHostAddress()+","+
	  _httpPort+","+
	  _migrationServer.getAddress().getHostAddress()+","+
	  _migrationServer.getPort()+","+
	  getBucketName();
      }

      return response;
    }
  }

  // TODO - not sure if this is the right way to go, but we need it now...
  protected String _bucketName;
  public String getBucketName(){return _bucketName;}
  public void setBucketName(String bucketName){_bucketName=bucketName;}

  protected RoutingStrategy _routingStrategy;
  public RoutingStrategy getRoutingStrategy(){return _routingStrategy;}
  public void setRoutingStrategy(RoutingStrategy routingStrategy){_routingStrategy=routingStrategy;}

  //----------------------------------------
  // Migration

  MigrationService.Server _migrationServer;

  //----------------------------------------
  // Migration

  public abstract String getSessionCookieName();
  public abstract String getSessionCookiePath(HttpServletRequest req);
  public abstract String getSessionCookieDomain();

  public abstract String getSessionUrlParamName();


  /**
   * Is this <address:port> one that we are serving on?
   *
   * @param address an <code>InetAddress</code> value
   * @param port an <code>int</code> value
   * @return a <code>boolean</code> value
   */
  public abstract boolean isServing(InetAddress address, int port);

  /**
   * return the/a port on which this container is listening for http
   * requests, this will be used by our location server to reply to
   * other nodes looking for a session held locally on this node,
   * instructing them where to redirect or proxy requests to to find
   * this session.
   *
   * @return an <code>int</code> value
   */
  public abstract int getHttpPort();

  public abstract ServletContext getServletContext();

  //----------------------------------------

  //----------------------------------------

  protected org.codehaus.wadi.shared.Manager.SessionPool _readySessionPool=new ReadySessionPool();
  protected org.codehaus.wadi.shared.Manager.SessionPool getReadySessionPool(){return _readySessionPool;}
  protected void setReadySessionPool(org.codehaus.wadi.shared.Manager.SessionPool pool){_readySessionPool=pool;}

  /**
   * A logical pool of initialised session impls. They are put here upon
   * invalidation/destruction and taken from here when a fresh session
   * is required.
   *
   */
  class ReadySessionPool
    extends org.codehaus.wadi.shared.Manager.SessionPool
  {
    public Object
      take()
    {
      String id=(String)getIdGenerator().take();
      HttpSessionImpl impl=(HttpSessionImpl)getBlankSessionPool().take();
      impl.init(Manager.this, id, System.currentTimeMillis(), _maxInactiveInterval, _maxInactiveInterval); // TODO need actual...

      // v. important - before we go public, take the rlock to indicate a request thread is busy in this session
      // TODO assert() that this is a request thread
      try
      {
	impl.getApplicationLock().acquire();
      }
      catch (InterruptedException e)
      {
	_log.warn("unable to acquire rlock on new session");
      }

      _local.put(id, impl);
      notifySessionCreated(impl.getFacade());
      return impl;
    }

    // initially we will do this synchronously, but later it should be
    // done asynchronously, by the housekeeping thread...
    public void
      put(Object o)
    {
      HttpSessionImpl impl=(HttpSessionImpl)o;
      _local.remove(impl.getId());
      org.codehaus.wadi.shared.HttpSession session=(org.codehaus.wadi.shared.HttpSession)impl.getFacade();

      // The session is marked as invalid until it has been removed
      // from _local. It must be temporarily switched back to valid
      // status so that it can be passed back into application space
      // as part of the destruction notification protocol...
      session.setInvalidated(false);

      if ("2.4".equals(getSpecificationVersion()))
	notifySessionDestroyed(session);

      // TODO - if we could get an iter directly from the attr map, we
      // could do this much faster... - IMPORTANT...
      String[] names=session.getValueNames();
      int len=names.length;
      for (int i=0;i<len;i++)
	session.removeAttribute(names[i]);
      names=null;

      if ("2.3".equals(getSpecificationVersion())) // TODO - 2.2? etc...
	notifySessionDestroyed(session);

      session.setInvalidated(true);
      impl.destroy();
      getBlankSessionPool().put(impl);
    }
  }

  // stats - TODO - since these are ints and updates are atomic we
  // should not need to sync them?

  protected int _sessionCreationCounter=0;
  public void setSessionCreationCounter(int n){_sessionCreationCounter=n;}
  public int getSessionCreationCounter(){return _sessionCreationCounter;}
  protected int _sessionExpirationCounter=0;
  public void setSessionExpirationCounter(int n){_sessionExpirationCounter=n;}
  public int getSessionExpirationCounter(){return _sessionExpirationCounter;}
  protected int _sessionInvalidationCounter=0;
  public void setSessionInvalidationCounter(int n){_sessionInvalidationCounter=n;}
  public int getSessionInvalidationCounter(){return _sessionInvalidationCounter;}
  protected int _sessionRejectionCounter=0;
  public void setSessionRejectionCounter(int n){_sessionRejectionCounter=n;}
  public int getSessionRejectionCounter(){return _sessionRejectionCounter;}

  // I think we also want to know how many sessions are activated,
  // passivated, migrated to us, migrated from us ? or simply how many
  // serialisations and deserialisations to store or other node we
  // have done. Also how many request relocations...
}
