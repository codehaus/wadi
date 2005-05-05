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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ObjectMessage;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.activecluster.Cluster;
import org.activecluster.ClusterEvent;
import org.activecluster.ClusterFactory;
import org.activecluster.ClusterListener;
import org.activecluster.Node;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.impl.RWLock;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.old.impl.FilePassivationStrategy;
import org.codehaus.wadi.old.impl.NoRoutingStrategy;
import org.codehaus.wadi.old.impl.RelativeEvictionPolicy;
import org.codehaus.wadi.old.impl.TotalEvictionPolicy;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

// TODO - replace some form of location discovery protocol

// TODO - refactor distributed GC election algorithm

// TODO - use assert() to confirm that correct type of threads are
// taking correct locks

// TODO - what are we going to do about background threads? reinstate
// the Security Manager...

// when we become cluster aware the node performing GC of evicted
// sessions should be elected according to a round-robin policy
// (perhaps) so that it is not always the same node doing it. A does it
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

/**
 * Common parts of the WADI session manager.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
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
  protected final Object _localLock=new Object();

  public HttpSessionImpl
    put(String realId, HttpSessionImpl session)
  {
    return (HttpSessionImpl)_local.put(realId, session);
  }

  // is this useful if we don't take a lock at the same time?
  public boolean
    owns(String realId)
  {
    if (_local.containsKey(realId))
      return true;
    else
      return false;
  }

  public HttpSessionImpl
    get(String realId)
  {
    if (!getInside())
    {
      _log.trace(realId+": looked up from outside the container");
      return null;		// this WILL become a dummy...
    }
    else
      return getLocalSession(realId);
  }

  /**
   * returns an UNLOCKED session
   *
   * @param realId a <code>String</code> value
   * @return a <code>HttpSessionImpl</code> value
   */
  public HttpSessionImpl
    getLocalSession(String realId)
  {
    return (HttpSessionImpl)_local.get(realId);
  }

  /**
   * returns a LOCKED session
   *
   * @param realId a <code>String</code> value
   * @return a <code>HttpSessionImpl</code> value
   */
  protected HttpSessionImpl
    getRemoteSession(String realId)
  {
    HttpSessionImpl impl=null;

    // this is to collapse multiple incoming threads looking for the
    // same session into one immigration...
    synchronized (_localLock)
    {
      // TODO - we need a more efficient locking mechanism here - we
      // should be able to release the collapsing lock as soon as we
      // have found the session, without waiting to acquire its
      // lock...
      if ((impl=getLocalSession(realId))!=null)
      {
	// session already local - return it...
	_log.debug(realId+": collapsing secondary immigration...");

	impl.acquireApplicationLock();

	HttpSession facade=(HttpSession)impl.getFacade();
	if (facade!=null && facade.isValid())
	  return impl;
	else
	  return null;

	// TODO - wee need to be able to indicate the difference
	// between a session that disappeared as we were trying to
	// gain a lock on it (i.e. we know existed until very
	// recently) and a session which did not exist locally at the
	// time of the test (i.e. may not have existed for some time,
	// if ever). Think...
      }
      else
      {
	// session is remote/dead - we will try to immigrate it
	// here. Take an exclusive lock on an empty session and insert
	// into local table as a placeholder.
	impl=createImpl();
	impl.setWadiManager(this);
	impl.acquireContainerLock();
	_local.put(realId, impl);
      }
    }

    if (getRemoteSession(realId, impl))
    {
      impl.getRWLock().downgrade();
    }
    else
    {
      // tidy up
      _log.info(realId+": failed to immigrate session - tidying up");

      _local.remove(realId);
      impl.getContainerLock().release();
      _releaseImpl(impl);
      impl=null;
    }

    return impl;
  }

  protected boolean
    getRemoteSession(String realId, HttpSessionImpl impl)
  {
    // if we get to here we know that session is remote/dead and that
    // it is up to us to find it and migrate it here...

    // We are initiating a migration.
    boolean successfulMigration=false;

    // If a passivation store has been enabled, we may find the
    // session in it and load it....
    if (_passivationStrategy!=null)
      if (true==(successfulMigration=_passivationStrategy.activate(realId, impl)))
	_sessionLoadCounter++;

    // If a migration policy has been enabled, we may request it
    // from another node.
    if (!successfulMigration)
    {
      long timeout=6000L;	// parameterise - TODO
      successfulMigration=_migrationService.getClient().immigrateSingleSession(realId, impl, timeout, _cluster.getDestination());
    }

    return successfulMigration;
  }

  public HttpSessionImpl
    remove(String realId)
  {
    // could we be trying to remove a session that is passivated....
    return (HttpSessionImpl)_local.remove(realId);
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

  protected MigrationService _migrationService;
  public MigrationService getMigrationService(){return _migrationService;}
  public void setMigrationService(MigrationService migrationService){_migrationService=migrationService;}

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


  protected String _configurationResource="/WEB-INF/wadi-web.xml";

  public synchronized void
  start()
  throws Exception
  {
      _log.debug("starting");
      _log.info("WADI-0.9 - Web Application Distribution Infrastructure (http://wadi.codehaus.org)");
      
      // load config
      try
      {
          // hack to get around the fact that dd is only found on maven's
          // first test run and subsequently does not get picked up due to
          // some classloader wierdness - revisit - TODO
          InputStream is=null;
          String config=System.getProperty("wadi.config");
          if (config!=null)
              is=new FileInputStream((_configurationResource=config));
          else
              is=_servletContext.getResourceAsStream(_configurationResource);
          
          if (is!=null)
          {
              DefaultListableBeanFactory dlbf=new DefaultListableBeanFactory();
              PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
              dlbf.registerSingleton("Manager", this);
              new XmlBeanDefinitionReader(dlbf).loadBeanDefinitions(new InputStreamResource(is));
              cfg.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
              cfg.postProcessBeanFactory(dlbf);
              
              Manager me=(Manager)dlbf.getBean("ConfiguredManager");
              assert me==this;
              
              _log.info("configured from WADI descriptor: "+_configurationResource);
          }
          else
              _log.warn("could not find WADI descriptor: "+_configurationResource);
      }
      catch (Exception e)
      {
          if (_log.isWarnEnabled()) _log.warn("problem configuring from: "+_configurationResource, e);
      }
      
      // TODO - is putting ourselves in an attribute a security risk ?
      _servletContext.setAttribute(Manager.class.getName(), this);
      _loader=Thread.currentThread().getContextClassLoader();
      //      System.setSecurityManager(new SecurityManager(System.getSecurityManager()));// TODO
      
      // default migration policy
      if (_migrationService==null) _migrationService=new org.codehaus.wadi.old.impl.MessagedMigrationService();
      if (_passivationStrategy==null) _passivationStrategy=new FilePassivationStrategy(new File("/tmp/wadi"));
      if (_streamingStrategy==null) _streamingStrategy=new SimpleStreamer();
      if (_passivationStrategy.getStreamingStrategy()==null) _passivationStrategy.setStreamingStrategy(_streamingStrategy);
      // default eviction policy
      if (_evictionPolicy==null) _evictionPolicy=new RelativeEvictionPolicy(0.5F);
      //default id generation strategy
      if (_idGenerator==null) _idGenerator=new TomcatSessionIdFactory();
      // default routing strategy
      if (_routingStrategy==null) _routingStrategy=new NoRoutingStrategy();
      
      // TODO - activecluster stuff - replace with config ASAP...
      if (_connectionFactory!=null)
      {
          (_connection=_connectionFactory.createConnection()).start();
          _clusterFactory=new DefaultClusterFactory(_connectionFactory);
          _cluster=_clusterFactory.createCluster("org.codehaus.wadi#cluster");
          _cluster.addClusterListener(new MembershipListener());
          
          _migrationService.setManager(this);
          _migrationService.setHttpSessionImplMap(_local);
          MigrationService.Server server=_migrationService.getServer();
          server.setHttpSessionImplFactory(_implFactory);
          server.start();
          
          _cluster.start(); // should include webapp context
      }
      
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

    // assume that impls have stopped their housekeeping thread/process
    // by now...

    if (getDistributable() && _passivationStrategy!=null)
    {
      // now we need to alter the eviction decision policy, so that
      // every session is invalidate or passivated, then run the
      // housekeeper again....
      EvictionPolicy oldEvictionPolicy=_evictionPolicy;
      _evictionPolicy=new TotalEvictionPolicy();
      int oldSize=_local.size();
      housekeeper();
      int newSize=_local.size();
      // this is inaccurate - currently sessions may also still be
      // created/[e/i]mmigrated whilst this is going on. sessions with
      // current requests will remain in container :-(. We need a fix in
      // Filter which will relocate all incoming requests...
      if (_log.isInfoEnabled()) _log.info("emmigrated "+(oldSize-newSize)+"/"+oldSize+" sessions");
      _evictionPolicy=oldEvictionPolicy;
    }

    // since housekeeping may take time, we'll keep these processes
    // running until afterwards for the moment...
    _migrationService.getServer().stop();

    // what about housekeeping thread ?

    //    System.setSecurityManager(((SecurityManager)System.getSecurityManager()).getDelegate());
    _loader=null;

    if (_connectionFactory!=null)
    {
      _cluster.stop();
      _clusterFactory=null;
      _connectionFactory.stop();
      _connectionFactory=null;
      Thread.sleep(2000);
    }

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
	if (_log.isTraceEnabled()) _log.trace("tidying up "+n+" session[s] expired in long-term storage");

	// we could be a lot cleverer here and :
	// - only reload impls when there is some listener to notify
	// - reuse the same impl for all of them...
	for (Iterator i=c.iterator(); i.hasNext();)
	{
	  HttpSessionImpl impl=createImpl();
	  if (_passivationStrategy.activate((String)i.next(), impl))
	  {
	    _sessionLoadCounter++;
	    impl.setWadiManager(this);
	    if (_log.isDebugEnabled()) _log.debug(impl.getId()+": expiration (in long-term storage)");
	    _notify(impl);	// TODO - these methods all need renaming/factoring etc..
	    _releaseImpl(impl);
	    _sessionExpirationCounter++;
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

  // Tomcat will run this as its backgroundProcess() - I assume with the correct CCL?
  // Jetty's SessionManager will manage it as part of its lifecycle.

  /**
   * Called periodically to do the housekeeping (session invalidation,
   * migration, etc...)
   *
   */

  public boolean setPriority(RWLock lock, int priority){lock.setPriority(priority);return true;}

  public void
    housekeeper()		// pass in eviction decision parameters
    throws InterruptedException
  {
    _log.trace("housekeeping beginning");

    long currentTime=System.currentTimeMillis();

    boolean canEvict=getDistributable() && _passivationStrategy!=null && _evictionPolicy!=null;

    if (canEvict)
      tidyStore(currentTime);

    boolean infoEnabled=_log.isInfoEnabled();
    boolean debugEnabled=_log.isDebugEnabled();
    boolean traceEnabled=_log.isTraceEnabled();
    for (Iterator i=_local.values().iterator(); i.hasNext();)
    {
      HttpSessionImpl impl=(HttpSessionImpl)i.next();
      RWLock lock=impl.getRWLock();
      // check, with shared access, for a number of reasons why a
      // session should be moved out of local store, then if it
      // should, take an exclusive lock and do so... - if we cannot
      // get the lock, forget this session, we will do it next time...
      boolean hasTimedOut=false;
      boolean shouldBeEvicted=false;

      boolean nottried=true;
      if ((((hasTimedOut=impl.hasTimedOut(currentTime)) && setPriority(lock, HttpSessionImpl.TIMEOUT_PRIORITY)) ||
	   ((shouldBeEvicted=(canEvict && _evictionPolicy.evictable(currentTime, impl))) && setPriority(lock, HttpSessionImpl.EVICTION_PRIORITY))) &&
	  (nottried=impl.getContainerLock().attempt(-1))) // do not wait
      {
	try
	{
	  String realId=impl.getRealId();
	  if (hasTimedOut)	// implicitly invalidated via time-out
	  {
	    if (debugEnabled) _log.debug(realId+" : expiration ("+currentTime+"-"+impl.getLastAccessedTime()+">"+impl.getMaxInactiveInterval()*1000+")");
	    if (traceEnabled) _log.trace(realId+" : removing (implicit time out)");
	    releaseImpl(impl);
	    continue;
	  }

	  if (shouldBeEvicted)
	  {
	    if (traceEnabled) _log.trace(realId+" : removing (migrating to long-term store)");
	    // should this be done asynchronously via another Channel ?
	    if (_passivationStrategy.passivate(impl))
	    {
	      _sessionStoreCounter++;
	      // TODO - we cannot use releaseImpl() as it will fire unecessary notifications...
	      _local.remove(realId);
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
      else if (!nottried)
      {
	if (infoEnabled) _log.info("tried but failed for lock on:"+impl.getRealId()+" - "+impl.getRWLock());
      }
    }
    _log.trace("housekeeping ended");
  }

  //----------------------------------------
  // session lifecycle...
  //----------------------------------------

  public String getSpecificationVersion(){return "2.4";} // TODO - read from DD

  protected SessionIdFactory _idGenerator;
  public SessionIdFactory getIdGenerator(){return _idGenerator;}
  public void setIdGenerator(SessionIdFactory generator){_idGenerator=generator;}

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
      if (_log.isDebugEnabled()) _log.debug("adding HttpSessionAttributeListener: "+listener);
      _attributeListeners.add(listener);
      known=true;
    }
    if (listener instanceof HttpSessionListener)
    {
      if (_log.isDebugEnabled()) _log.debug("adding HttpSessionListener: "+listener);
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
      if (_log.isDebugEnabled()) _log.debug("removing HttpSessionAttributeListener: "+listener);
      known|=_attributeListeners.remove(listener);
    }
    if (listener instanceof HttpSessionListener)
    {
      if (_log.isDebugEnabled()) _log.debug("removing HttpSessionListener: "+listener);
      known|=_sessionListeners.remove(listener);
    }

    if (!known)
      if (_log.isWarnEnabled()) _log.warn("EventListener not registered: "+listener);
  }

  //--------------------
  // session events
  //--------------------

  public void
    notifySessionCreated(String realId, javax.servlet.http.HttpSession session)
  {
    _sessionCreationCounter++;

    int n=_sessionListeners.size();
    if (n>0)
    {
      if (_log.isTraceEnabled()) _log.trace(realId+" : notifying session creation");
      HttpSessionEvent event = new HttpSessionEvent(session);

      for(int i=0;i<n;i++)
	notifySessionCreated((HttpSessionListener)_sessionListeners.get(i), event);

      event=null;
    }
  }

  public void
    notifySessionDestroyed(String realId, javax.servlet.http.HttpSession session)
  {
    int n=_sessionListeners.size();
    if (n>0)
    {
      if (_log.isTraceEnabled()) _log.trace(realId+" : notifying session destruction");
      HttpSessionEvent event = new HttpSessionEvent(session);

      for(int i=0;i<n;i++)
	notifySessionDestroyed((HttpSessionListener)_sessionListeners.get(i), event);

      event=null;
    }

    // N.B. This event is a misnomer - 2.4 spec says "Notification
    // that a session IS ABOUT TO BE invalidated."...(my caps)
    _sessionDestructionCounter++;
  }

  protected void
    notifySessionAttributeAdded(String realId, javax.servlet.http.HttpSession session, String key, Object val)
  {
    int n=_attributeListeners.size();
    if (n>0)
    {
      if (_log.isTraceEnabled()) _log.trace(realId+" : notifying attribute addition : "+key+" : null --> "+val);

      HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, key, val);
      for(int i=0;i<n;i++)
	notifySessionAttributeAdded((HttpSessionAttributeListener)_attributeListeners.get(i), event);

      event=null;
    }
  }

  protected void
    notifySessionAttributeReplaced(String realId, javax.servlet.http.HttpSession session, String key, Object oldVal, Object newVal)
  {
    int n=_attributeListeners.size();
    if (n>0)
    {
      if (_log.isTraceEnabled()) _log.trace(realId+" : notifying attribute replacement : "+key+" : "+oldVal+" --> "+newVal);
      HttpSessionBindingEvent event = new HttpSessionBindingEvent(session, key, oldVal);

      for(int i=0;i<n;i++)
	notifySessionAttributeReplaced((HttpSessionAttributeListener)_attributeListeners.get(i), event);

      event=null;
    }
  }

  protected void
    notifySessionAttributeRemoved(String realId, javax.servlet.http.HttpSession session, String key, Object val)
  {
    int n=_attributeListeners.size();
    if (n>0)
    {
      if (_log.isTraceEnabled()) _log.trace(realId+" : notifying attribute removal : "+key+" : "+val+" --> null");
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
    replicate(String realId, Method method, Object[] args)
  {
    if (_log.isDebugEnabled()) _log.debug("replicating delta: "+realId+" -> "+method);

    // this method should be invoked on a dynamic proxy which
    // represents the cluster to which the deltas are being sent...

    // lots of clever synch/asynch stuff, batching, compression etc
    // can be done here or in the replication medium...
  }

  public void
    invoke(String realId, Method method, Object[] args)
  {
    // look up a session and invoke the given method and args upon
    // it...
    if (_log.isDebugEnabled()) _log.debug("invoking delta: "+realId+" -> "+method);
  }

  //  protected abstract HttpSession createFacade(HttpSessionImpl impl);

  //   public void
  //     notifyRequestEnd(String id)
  //   {
  //     if (_log.isTraceEnabled()) _log.trace(id+": request end");
  //   }

  //   public void
  //     notifyRequestGroupEnd(String id)
  //   {
  //     if (_log.isTraceEnabled()) _log.trace(id+": request group end");
  //   }

  //   public boolean getUsingRequestGroups(){return true;}

  public ManagerProxy
    locate(String realId)
  {
    //   String location=_migrationService.getClient.immigrate(realId, null, timeout);
    String location=null;

    if (location==null)
    {
      //      if (_log.isWarnEnabled()) _log.warn(realId+": could not locate session - perhaps dead ?");
      return null;
    }

    try
    {
      return new ManagerProxy(realId, location);
    }
    catch (Exception  e)
    {
      _log.warn("bad location response", e);
      return null;
    }
  }

  protected RoutingStrategy _routingStrategy;
  public RoutingStrategy getRoutingStrategy(){return _routingStrategy;}
  public void setRoutingStrategy(RoutingStrategy routingStrategy){_routingStrategy=routingStrategy;}

  //----------------------------------------
  // Migration

  public abstract String getSessionCookieName();
  public abstract String getSessionCookiePath(HttpServletRequest req);
  public abstract String getSessionCookieDomain();

  public abstract String getSessionUrlParamName();

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

  protected ServletContext _servletContext;
  public ServletContext getServletContext(){return _servletContext;}
  public void setServletContext(ServletContext servletContext){_servletContext=servletContext;}

  public abstract HttpSessionContext getSessionContext();

  // stats - TODO - since these are ints and updates are atomic we
  // should not need to sync them?

  // IDEA - if we know what percentage of sessions are timing out at
  // what mii, could we automatically adjust it to be as efficient as
  // possible - throughout the day?

  public int getSessionCurrentCount(){return _local.size();}

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
  protected int _sessionRejectionCounter=0; // TODO - NYI
  public void setSessionRejectionCounter(int n){_sessionRejectionCounter=n;}
  public int getSessionRejectionCounter(){return _sessionRejectionCounter;}

  protected int _sessionLoadCounter;
  public void setSessionLoadCounter(int n){_sessionLoadCounter=n;}
  public int getSessionLoadCounter(){return _sessionLoadCounter;}
  protected int _sessionStoreCounter;
  public void setSessionStoreCounter(int n){_sessionStoreCounter=n;}
  public int getSessionStoreCounter(){return _sessionStoreCounter;}
  protected int _sessionSendCounter; // TODO - NYI
  public void setSessionSendCounter(int n){_sessionSendCounter=n;}
  public int getSessionSendCounter(){return _sessionSendCounter;}
  protected int _sessionReceivedCounter; // TODO - NYI
  public void setSessionReceivedCounter(int n){_sessionReceivedCounter=n;}
  public int getSessionReceivedCounter(){return _sessionReceivedCounter;}

  protected int _sessionLocalHitCounter; // TODO - NYI
  public void setSessionLocalHitCounter(int n){_sessionLocalHitCounter=n;}
  public int getSessionLocalHitCounter(){return _sessionLocalHitCounter;}
  protected int _sessionStoreHitCounter; // TODO - NYI
  public void setSessionStoreHitCounter(int n){_sessionStoreHitCounter=n;}
  public int getSessionStoreHitCounter(){return _sessionStoreHitCounter;}
  protected int _sessionRemoteHitCounter; // TODO - NYI
  public void setSessionRemoteHitCounter(int n){_sessionRemoteHitCounter=n;}
  public int getSessionRemoteHitCounter(){return _sessionRemoteHitCounter;}
  protected int _sessionMissCounter; // TODO - NYI
  public void setSessionMissCounter(int n){_sessionMissCounter=n;}
  public int getSessionMissCounter(){return _sessionMissCounter;}

  protected int _requestAcceptedCounter; // TODO - NYI
  public void setRequestAcceptedCounter(int n){_requestAcceptedCounter=n;}
  public int getRequestAcceptedCounter(){return _requestAcceptedCounter;}
  protected int _requestRedirectedCounter; // TODO - NYI
  public void setRequestRedirectedCounter(int n){_requestRedirectedCounter=n;}
  public int getRequestRedirectedCounter(){return _requestRedirectedCounter;}
  protected int _requestProxiedCounter;	// TODO - NYI
  public void setRequestProxiedCounter(int n){_requestProxiedCounter=n;}
  public int getRequestProxiedCounter(){return _requestProxiedCounter;}

  protected int _requestStatefulCounter; // TODO - NYI
  public void setRequestStatefulCounter(int n){_requestStatefulCounter=n;}
  public int getRequestStatefulCounter(){return _requestStatefulCounter;}
  protected int _requestStatelessCounter; // TODO - NYI
  public void setRequestStatelessCounter(int n){_requestStatelessCounter=n;}
  public int getRequestStatelessCounter(){return _requestStatelessCounter;}

  // number of local sessions
  // number of sessions in store (too expensive?)
  // it would be nice to know the actual number of bytes moved an hour
  // etc...

  public HttpSessionImpl acquireImpl(Manager manager){return acquireImpl(manager, null);}

  public HttpSessionImpl
    acquireImpl(Manager manager, String realId)
  {
    if (realId==null)
      realId=(String)getIdGenerator().create();

    HttpSessionImpl impl=createImpl();
    impl.init(Manager.this, realId, System.currentTimeMillis(), _maxInactiveInterval, _maxInactiveInterval); // TODO need actual...
    _acquireImpl(realId, impl);
    notifySessionCreated(realId, impl.getFacade());
    if (_log.isDebugEnabled()) _log.debug(realId+": creation");
    return impl;
  }

  protected void
    _acquireImpl(String realId, HttpSessionImpl impl)
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

    _local.put(realId, impl);
  }

  protected void
    _notify(HttpSessionImpl impl)
  {
    HttpSession session=(HttpSession)impl.getFacade();
    String realId=impl.getRealId();

    // The session is marked as invalid until it has been removed
    // from _local. It must be temporarily switched back to valid
    // status so that it can be passed back into application space
    // as part of the destruction notification protocol...
    session.setValid(true);

    if ("2.4".equals(getSpecificationVersion()))
      notifySessionDestroyed(realId, session);

    // TODO - if we could get an iter directly from the attr map, we
    // could do this much faster... - IMPORTANT...
    String[] names=session.getValueNames();
    int len=names.length;
    for (int i=0;i<len;i++)
      session.removeAttribute(names[i]);
    names=null;

    if ("2.3".equals(getSpecificationVersion())) // TODO - 2.2? etc...
      notifySessionDestroyed(realId, session);

    session.setValid(false);
  }

  public void
    releaseImpl(HttpSessionImpl impl)
  {
    String realId=impl.getRealId();

    if (realId==null)
    {
      _log.warn("session impl already removed!");
      return;
    }

    _local.remove(realId);
    _notify(impl);
    _releaseImpl(impl);
  }

  public void
    _releaseImpl(HttpSessionImpl impl)
  {
    impl.setWadiManager(null);
    impl.destroy();
    destroyImpl(impl);
  }

  protected HttpSessionImplFactory _implFactory;
  public HttpSessionImplFactory getHttpSessionImplFactory(){return _implFactory;}
  protected HttpSessionImpl createImpl(){return _implFactory.create();}
  protected void destroyImpl(HttpSessionImpl impl){_implFactory.destroy(impl);}

  boolean _reuseSessionIds=false;
  public void setReuseSessionIds(boolean reuseSessionIds){_reuseSessionIds=reuseSessionIds;}
  public boolean getReuseSessionIds(){return _reuseSessionIds;}

  protected Streamer _streamingStrategy;
  public Streamer getStreamingStrategy(){return _streamingStrategy;}
  public void setStreamingStrategy(Streamer streamingStrategy){_streamingStrategy=streamingStrategy;}

  protected Filter _filter;
  public void setFilter(Filter filter){_filter=filter;}

  // activecluster stuff...

  protected Cluster _cluster;
  public void setCluster(Cluster cluster){_cluster=cluster;}
  public Cluster getCluster(){return _cluster;}

  protected ClusterFactory _clusterFactory;
  public void setClusterFactory(ClusterFactory clusterFactory){_clusterFactory=clusterFactory;}
  public ClusterFactory getClusterFactory(){return _clusterFactory;}

  protected Connection _connection;
  public void setConnection(Connection connection){_connection=connection;}
  public Connection getConnection(){return _connection;}

  protected ActiveMQConnectionFactory _connectionFactory;
  public void setConnectionFactory(ActiveMQConnectionFactory connectionFactory){_connectionFactory=connectionFactory;}
  public ActiveMQConnectionFactory getConnectionFactory(){return _connectionFactory;}

  protected Map _conversations=new HashMap();

  protected void
    sendCommandToCluster(Executable command)
    throws Exception
  {
    ObjectMessage om = _cluster.createObjectMessage();
    //Destination dst=_cluster.getDestination();
    Destination dst=_cluster.getLocalNode().getDestination();
    om.setJMSReplyTo(dst);
    _log.info("setting ReplyTo: "+dst);
    om.setObject(command);
    _cluster.send(_cluster.getDestination(), om);
  }

  protected void
    sendCommandToNode(Node node, Executable command)
    throws Exception
  {
    ObjectMessage om = _cluster.createObjectMessage();
    Destination dst=_cluster.getLocalNode().getDestination();
    om.setJMSReplyTo(dst);
    _log.info("setting ReplyTo: "+dst);
    om.setObject(command);
    _cluster.send(node.getDestination(), om);
  }

  class MembershipListener
    implements ClusterListener
  {
    public void onNodeAdd(ClusterEvent event){_log.info("node add");}
    public void onNodeUpdate(ClusterEvent event){_log.info("node updated");}
    public void onNodeRemoved(ClusterEvent event){_log.info("node removed");}
    public void onNodeFailed(ClusterEvent event){_log.info("node failed");}
    public void onCoordinatorChanged(ClusterEvent event){_log.info("coordinator changed");}
  }

  //----------------------------------------

  protected ThreadLocal _inside=new ThreadLocal() {protected synchronized Object initialValue() {return Boolean.FALSE;}};
  public void setInside(boolean b){_inside.set(b?Boolean.TRUE:Boolean.FALSE);}
  public boolean getInside(){return ((Boolean)_inside.get()).booleanValue();}

  //----------------------------------------

  protected int _immigrationAttemptCount=9;
  public void setImmigrationAttemptCount(int n){_immigrationAttemptCount=n;}
  public int getImmigrationAttemptCount(){return _immigrationAttemptCount;}
}
