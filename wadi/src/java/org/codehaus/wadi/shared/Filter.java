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
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO

// will filter do anything unless manager is 'distributable' ? it will
// still need to fix session id...

// TODO - we need, in some cases, to wrap the req/res and rewrite the
// session cookie to exclude/include mod_jk routing info. mod_jk and
// client need to see this. webapp and container do not.

// TODO - if we were not accepting sessions, we would need to know if
// an accessible session was local or evicted, so that we could proxy
// the request and force it's loading in another container...

// TODO - if we wanted to spill a few sessions we could wait for them
// to become free, evict them and then proxy subsequent requests until
// client catches up with new location...

/**
 * Installed at front of Filter stack. Manages WADI-specific fn-ality.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  Filter
  implements javax.servlet.Filter
{
  protected Log _log = LogFactory.getLog(getClass());

  protected Manager _manager;
  protected boolean _distributable;

  // Filter Lifecycle

  public void
    init(FilterConfig filterConfig)
  {
    _manager=(Manager)filterConfig.getServletContext().getAttribute(org.codehaus.wadi.shared.Manager.class.getName());
    if (_manager==null)
      _log.fatal("Manager not found");
    else
      if (_log.isTraceEnabled()) _log.trace("Manager found: "+_manager);

    _manager.setFilter(this);
    _distributable=_manager.getDistributable();

  }

  public void
    destroy()
  {
    _distributable=false;
    _manager=null;
  }

  // Filter fn-ality

  public void
    doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException
  {
    if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse))
    {
      _log.warn("not an HttpServlet req/res pair - therefore stateless - ignored by WADI");
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest req=(HttpServletRequest)request;
    HttpServletResponse res=(HttpServletResponse)response;
    String id=req.getRequestedSessionId();
    HttpSessionImpl impl=null;

    try
    {
      if (id==null)
      {
	// TODO - at the moment, nothing, but later we should consider
	// whether we want to run this request here - it may create a
	// session - or somewhere else...
	;
      }
      else
      {
	// Ensure that Manager.get() is called before entry (this may
	// have already happened) - this will pull in and lock our
	// session even if remote.
	if((impl=_manager.get(id))!=null && // KEEP
	   ((HttpSession)impl.getFacade()).isValid())
	{
	  // restick lb to this node if necessary...
	  if (req.isRequestedSessionIdFromCookie())
	    _manager.getRoutingStrategy().rerouteCookie(req, res, _manager, id);
	  else if (req.isRequestedSessionIdFromURL())
	    _manager.getRoutingStrategy().rerouteURL();	// NYI
	}
	// if id is non-null, but session does not exist locally -
	// consider relocating session or request....
      }
      // we need to ensure that a shared lock has been taken on the
      // session before proceeding...

      chain.doFilter(request, response);
    }
    finally
    {
      // ensure that this request's current session's shared lock is
      // released...

      // we have to look up the session again as it may have been
      // invalidated and even replaced during the request.
      javax.servlet.http.HttpSession session=req.getSession(false);
      if (session==null)
      {
	// no valid session - no lock to release...
	_log.trace("no outgoing session");
      }
      else
      {
	String newId=session.getId();
	newId=_manager.getRoutingStrategy().strip(newId);

	boolean reuse=_manager.getReuseSessionIds();
	// we have to release a lock
	if (id!=null && !reuse && id.equals(newId))
	{
	  // an optimisation, hopefully the most common case -
	  // saves us a lookup that we have already done...
	  impl.getApplicationLock().release();
	  if (_log.isTraceEnabled()) _log.trace(newId+": original session maintained throughout request");
	}
	else
	{
	  // we cannot be sure that the session coming out of the
	  // request is the same as the one that went in to it, so
	  impl=_manager.getLocalSession(newId);
	  // session must still be valid, since we have not yet
	  // released our lock, so no need to check...

	  impl.getApplicationLock().release();
	  if (reuse)
	    if (_log.isTraceEnabled()) _log.trace(newId+": potential session id reuse - outgoing session may be new");
	    else
	      if (_log.isTraceEnabled()) _log.trace(newId+": new outgoing session");
	}
      }
      // in case Jetty or Tomcat is thread-pooling :
      _manager.setFirstGet(true); // ready for next time through...
    }
  }

  public void
    process(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
	    String sessionId)
    throws IOException, ServletException
  {
    //    assert Thread.currentThread().getName().startsWith("PoolThread-"); // TODO - Jetty only

    // every session has a RWLock
    // priority will be given to Readers
    // application threads are Readers
    // container threads that need to ensure exclusive access to application resources are Writers.

    try
    {
      chain.doFilter(request, response);
    }
    finally
    {
      // the session may have just been created - if so we need to
      // look it up and release the read lock...
      javax.servlet.http.HttpSession session=request.getSession(false);
      if (session!=null)
      {
	HttpSessionImpl impl=(HttpSessionImpl)_manager.get(_manager.getRoutingStrategy().strip(session.getId()));
	if (_log.isTraceEnabled()) _log.trace(sessionId+"; just created - releasing");
	if (impl!=null)
	  impl.getApplicationLock().release();
      }
    }

    // TODO - LATER FUNCTIONALITY - REPLICATION WITH VALUE BASED SEMANTICS
    //     try
    //     {
    //       // can we acquire an exclusive lock for session replication
    //       // etc...

    //       // TODO - PROBLEM - we really need to upgrade our RLock to a
    //       // WLock, so there is no time for the housekeeping thread to
    //       // squeeze in and prevent us from signalling the end of a
    //       // request group... how do we ensure that this does not happen ?
    //       if (writeLock!=null && _manager.getUsingRequestGroups() && writeLock.attempt(-1))
    //       {
    // 	// if so, and we are working at request-group granularity,
    // 	// then this is the end of a request group....
    // 	_manager.notifyRequestGroupEnd(sessionId);
    //       }
    //       else
    //       {
    // 	// otherwise this is only the end of a single request within
    // 	// a group
    // 	_manager.notifyRequestEnd(sessionId);
    //       }
    //     }
    //     catch (InterruptedException e)
    //     {
    //       _log.warn("unexpected interruption", e);
    //       Thread.interrupted();
    //       return;
    //     }
    //     finally
    //     {
    //       if (writeLock!=null)
    // 	writeLock.release();
    //     }
  }
}
