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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;
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

  protected ThreadLocal _firstGet=new ThreadLocal()
    {
      protected synchronized Object initialValue() {return Boolean.TRUE;}
    };

  public void setFirstGet(boolean b){_firstGet.set(b?Boolean.TRUE:Boolean.FALSE);}
  public boolean getFirstGet(){return ((Boolean)_firstGet.get()).booleanValue();}

  public HttpSessionImpl
    get(String id)
  {
    HttpSessionImpl impl=getLocalSession(id);

    if (getFirstGet())
    {
      setFirstGet(false);
      // TODO - how do we synchronise this on a per-session basis - a
      // HashMap of locks - yeugh!
      if (impl==null)
	impl=getRemoteSession(id);

      if (impl!=null)
	try
	{
	  impl.getApplicationLock().acquire(); // locked for duration of request...
	}
	catch (InterruptedException e)
	{
	  _log.warn("unexpected interruption", e);
	}
    }

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
	boolean successfulMigration=false;
	impl=createImpl();

	// If a passivation store has been enabled, we may find the
	// session in it and load it....
	if (_passivationStrategy!=null)
	  successfulMigration=_passivationStrategy.activate(id, impl);

	// If a migration policy has been enabled, we may request it
	// from another node.

	if (!successfulMigration)
  	{
  	  ManagerProxy proxy=locate(id);
  	  if (proxy!=null)
  	    successfulMigration=proxy.relocateSession(_local, _migrating, id, impl);
  	}

	if (successfulMigration)
	{
	  // make newly acquired session impl available to container...
	  assert impl!=null;
	  assert impl.getId()!=null;
	  _acquireImpl(impl);
	}
	else
	{
	  // we were not able to migrate the session to this node -
	  // tidy up the impl into which we were hoping to migrate
	  // it...
	  _releaseImpl(impl);
	  impl=null;
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

  public void
    tidyStore(long currentTime)
  {
    if (_passivationStrategy.isElected())
    {
      // We are responsible for distributed garbage collection...
      Collection list=new LinkedList();
      // load all sessions that have timed out and destroy them...
      Collection c=_passivationStrategy.findTimedOut(currentTime, list);
      int n=c.size();
      if (n>0)
      {
	_log.trace("tidying up "+n+" session[s] expired in long-term storage");

	// we could be a lot cleverer here and :
	// - only reload impls when there is some listener to notify
	// - reuse the same impl for all of them...
	for (Iterator i=c.iterator(); i.hasNext();)
	{
	  HttpSessionImpl impl=createImpl();
	  if (_passivationStrategy.activate((String)i.next(), impl))
	  {
	    impl.setWadiManager(this);
	    _notify(impl);	// TODO - these methods all need renaming/factoring etc..
	    _releaseImpl(impl);
	  }
	  else
	  {
	    destroyImpl(impl); // put back in cache
	  }
	}
	list.clear();		// could be reused on next iteration...
      }
      // we have now served a term - stand down...
      _passivationStrategy.standDown();
    }
    else
    {
      // Someone else has been responsible for distributed garbage
      // collection. It's time we served a term...
      _passivationStrategy.standUp();
    }
  }

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

    long currentTime=System.currentTimeMillis();

    boolean canEvict=_passivationStrategy!=null && _evictionPolicy!=null;

    if (canEvict)
      tidyStore(currentTime);

    for (Iterator i=_local.values().iterator(); i.hasNext();)
    {
      HttpSessionImpl impl=(HttpSessionImpl)i.next();

      // check, with shared access, for a number of reasons why a
      // session should be moved out of local store, then if it
      // should, take an exclusive lock and do so... - if we cannot
      // get the lock, forget this session, we will do it next time...
      boolean hasTimedOut=false;
      boolean shouldBeEvicted=false;

      if (((hasTimedOut=impl.hasTimedOut(currentTime)) ||
	   (shouldBeEvicted=(canEvict && _evictionPolicy.evictable(currentTime, impl)))) &&
	  impl.getContainerLock().attempt(0))
      {
	try
	{
	  if (hasTimedOut)	// implicitly invalidated via time-out
	  {
	    _log.trace(impl.getId()+" : removing (implicit time out)");
	    _log.debug(impl.getId()+" : timed out");
	    releaseImpl(impl);
	    continue;
	  }

	  if (shouldBeEvicted)
	  {
	    _log.trace(impl.getId()+" : removing (migrating to long-term store)");
	    // should this be done asynchronously via another Channel ?
	    if (_passivationStrategy.passivate(impl))
	    {
	      // TODO - we cannot use releaseImpl() as it will fire unecessary notifications...
	      _local.remove(impl.getId());
	      impl.destroy();
	      // TODO - how do we recycle the impl ?
	    }

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

  //  protected abstract HttpSession createFacade(HttpSessionImpl impl);

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

    if (location==null)
    {
      _log.warn(id+": could not locate session - perhaps dead ?");
      return null;
    }

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

  // stats - TODO - since these are ints and updates are atomic we
  // should not need to sync them?

  protected int _sessionCreationCounter=0;
  public void setSessionCreationCounter(int n){_sessionCreationCounter=n;}
  public int getSessionCreationCounter(){return _sessionCreationCounter;}
  protected int _sessionDestructionCounter=0;
  public void setSessionDestructionCounter(int n){_sessionDestructionCounter=n;}
  public int getSessionDestructionCounter(){return _sessionDestructionCounter;}

  protected int _sessionExpirationCounter=0;
  public void setSessionExpirationCounter(int n){_sessionExpirationCounter=n;}
  public int getSessionExpirationCounter(){return _sessionExpirationCounter;}
  protected int _sessionInvalidationCounter=0;
  public void setSessionInvalidationCounter(int n){_sessionInvalidationCounter=n;}
  public int getSessionInvalidationCounter(){return _sessionInvalidationCounter;}
  protected int _sessionRejectionCounter=0;
  public void setSessionRejectionCounter(int n){_sessionRejectionCounter=n;}
  public int getSessionRejectionCounter(){return _sessionRejectionCounter;}

  protected int _sessionLoadCounter;
  public void setSessionLoadCounter(int n){_sessionLoadCounter=n;}
  public int getSessionLoadCounter(){return _sessionLoadCounter;}
  protected int _sessionStoreCounter;
  public void setSessionStoreCounter(int n){_sessionStoreCounter=n;}
  public int getSessionStoreCounter(){return _sessionStoreCounter;}
  protected int _sessionSendCounter;
  public void setSessionSendCounter(int n){_sessionSendCounter=n;}
  public int getSessionSendCounter(){return _sessionSendCounter;}
  protected int _sessionReceivedCounter;
  public void setSessionReceivedCounter(int n){_sessionReceivedCounter=n;}
  public int getSessionReceivedCounter(){return _sessionReceivedCounter;}

  protected int _sessionLocalHitCounter;
  public void setSessionLocalHitCounter(int n){_sessionLocalHitCounter=n;}
  public int getSessionLocalHitCounter(){return _sessionLocalHitCounter;}
  protected int _sessionStoreHitCounter;
  public void setSessionStoreHitCounter(int n){_sessionStoreHitCounter=n;}
  public int getSessionStoreHitCounter(){return _sessionStoreHitCounter;}
  protected int _sessionRemoteHitCounter;
  public void setSessionRemoteHitCounter(int n){_sessionRemoteHitCounter=n;}
  public int getSessionRemoteHitCounter(){return _sessionRemoteHitCounter;}
  protected int _sessionMissCounter;
  public void setSessionMissCounter(int n){_sessionMissCounter=n;}
  public int getSessionMissCounter(){return _sessionMissCounter;}

  protected int _requestAcceptedCounter;
  public void setRequestAcceptedCounter(int n){_requestAcceptedCounter=n;}
  public int getRequestAcceptedCounter(){return _requestAcceptedCounter;}
  protected int _requestRedirectedCounter;
  public void setRequestRedirectedCounter(int n){_requestRedirectedCounter=n;}
  public int getRequestRedirectedCounter(){return _requestRedirectedCounter;}
  protected int _requestProxiedCounter;
  public void setRequestProxiedCounter(int n){_requestProxiedCounter=n;}
  public int getRequestProxiedCounter(){return _requestProxiedCounter;}

  protected int _requestStatefulCounter;
  public void setRequestStatefulCounter(int n){_requestStatefulCounter=n;}
  public int getRequestStatefulCounter(){return _requestStatefulCounter;}
  protected int _requestStatelessCounter;
  public void setRequestStatelessCounter(int n){_requestStatelessCounter=n;}
  public int getRequestStatelessCounter(){return _requestStatelessCounter;}

  // number of local sessions
  // number of sessions in store (too expensive?)
  // it would be nice to know the actual number of bytes moved an hour
  // etc...

  protected HttpSessionImpl acquireImpl(Manager manager){return acquireImpl(manager, null);}

  protected HttpSessionImpl
    acquireImpl(Manager manager, String id)
  {
    if (id==null)
      id=(String)getIdGenerator().take();

    HttpSessionImpl impl=createImpl();
    impl.init(Manager.this, id, System.currentTimeMillis(), _maxInactiveInterval, _maxInactiveInterval); // TODO need actual...
    _acquireImpl(impl);
    notifySessionCreated(impl.getFacade());
    _log.debug(impl.getId()+": created");
    return impl;
  }

  protected void
    _acquireImpl(HttpSessionImpl impl)
  {
    impl.setWadiManager(this);
    // v. important - before we go public, take the rlock to indicate a request thread is busy in this session
    // TODO assert() that this is a request thread

    // TODO - should this locking be here - check in filter. do we
    // need to do it on creation or immigration ?
    try
    {
      impl.getApplicationLock().acquire();
    }
    catch (InterruptedException e)
    {
      _log.warn("unable to acquire rlock on new session");
    }

    _local.put(impl.getId(), impl);
  }

  // N.B. called by application-space thread - this must release its
  // shared lock first in order that we can try for an exclusive
  // lock...
  public void
    invalidateImpl(HttpSessionImpl impl)
  {
    Sync lock=impl.getContainerLock();
    String id=impl.getId();

    boolean acquired=false;
    try
    {
      _log.trace("acquiring exclusive lock for session invalidation");
      lock.acquire();
      _log.trace("lock acquired");
      acquired=true;
      // notification MUST be done synchronously on the request thread
      // because Servlet-2.4 insists that it is given BEFORE
      // invalidation!
      releaseImpl(impl);
      _log.debug(id+": invalidated");
    }
    catch (InterruptedException e)
    {
      _log.warn("interrupted during invalidation - session not invalidated", e);
    }
    finally
    {
      if (acquired)
	lock.release();
    }
  }

  protected void
    _notify(HttpSessionImpl impl)
  {
    HttpSession session=(HttpSession)impl.getFacade();

    // The session is marked as invalid until it has been removed
    // from _local. It must be temporarily switched back to valid
    // status so that it can be passed back into application space
    // as part of the destruction notification protocol...
    session.setValid(true);

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

    session.setValid(false);
  }

  protected void
    releaseImpl(HttpSessionImpl impl)
  {
    _local.remove(impl.getId());
    _notify(impl);
    _releaseImpl(impl);
  }

  protected void
    _releaseImpl(HttpSessionImpl impl)
  {
    impl.setWadiManager(null);
    impl.destroy();
    destroyImpl(impl);
  }

  protected abstract HttpSessionImpl createImpl();
  protected abstract void destroyImpl(HttpSessionImpl impl);

  boolean _reuseSessionIds=false;
  public boolean getReuseSessionIds(){return _reuseSessionIds;}
}
