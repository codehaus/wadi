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
	String realId=_manager.getRoutingStrategy().strip(id);
	// Ensure that Manager.get() is called before entry (this may
	// have already happened) - this will pull in and lock our
	// session even if remote.
	if((impl=_manager.get(realId))==null)
	{
	  if (!_manager.getDistributable())
	  {
	    // we cannot relocate the session to this request, so we
	    // must relocate the request to the session...
	    ManagerProxy proxy=_manager.locate(realId);
	    if (proxy!=null)
	    {
	      proxy.relocateRequest(req, res, _manager);
	      return;
	    }
	  }
	}
	else if (((HttpSession)impl.getFacade()).isValid())
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
      // TODO - This needs to be rewritten to e.g. stash a lock in a
      // threadlocal when taken and remove it when released. If thread
      // still owns lock when it gets to here it should be
      // released....


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
	String newId=session.getId(); // can we not be more clever about this ?
	String newRealId=_manager.getRoutingStrategy().strip(newId);

	boolean reuse=_manager.getReuseSessionIds();
	// we have to release a lock
	if (id!=null && !reuse && id.equals(newRealId))
	{
	  // an optimisation, hopefully the most common case -
	  // saves us a lookup that we have already done...
	  impl.getApplicationLock().release();
	  if (_log.isTraceEnabled()) _log.trace(newRealId+": original session maintained throughout request");
	}
	else
	{
	  // we cannot be sure that the session coming out of the
	  // request is the same as the one that went in to it, so
	  impl=_manager.getLocalSession(newRealId);
	  // session must still be valid, since we have not yet
	  // released our lock, so no need to check...

	  impl.getApplicationLock().release();
	  if (reuse)
	    if (_log.isTraceEnabled()) _log.trace(newRealId+": potential session id reuse - outgoing session may be new");
	    else
	      if (_log.isTraceEnabled()) _log.trace(newRealId+": new outgoing session");
	}
      }
      // in case Jetty or Tomcat is thread-pooling :
      _manager.setFirstGet(true); // ready for next time through...
    }
  }
}
