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

import EDU.oswego.cs.dl.util.concurrent.Sync;
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

public class
  Filter
  implements javax.servlet.Filter
{
  protected Log _log = LogFactory.getLog(getClass());

  protected Manager _manager;
  protected boolean _distributable;

  public void
    init(FilterConfig filterConfig)
  {
    _log.info("WADI v1.0 Filter installed");

    _manager=(Manager)filterConfig.getServletContext().getAttribute(org.codehaus.wadi.shared.Manager.class.getName());
    if (_manager==null)
      _log.fatal("Manager not found");
    else
      _log.trace("Manager found: "+_manager);

    _distributable=_manager.getDistributable();
  }

  public void
    doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException
    {
//	HttpServletRequest req=new RequestWrapper((HttpServletRequest)request);
//	HttpServletResponse res=new ResponseWrapper((HttpServletResponse)response);
	HttpServletRequest req=(HttpServletRequest)request;
	HttpServletResponse res=(HttpServletResponse)response;
      String sessionId=req.getRequestedSessionId();
      if (sessionId==null)
      {
	_log.trace("no session id: "+req.getHeader("Cookie"));
	// request is not taking part in a session - do we want to
	// risk it creating a new one here, or should we proxy it to a
	// node that is carrying fewer sessions than we are...? - TODO
	process(req, res, chain, sessionId, null, null);
      }
      else
      {
	_log.trace("external session id: "+sessionId);
	sessionId=_manager.getRoutingStrategy().strip(_manager.getBucketName(), sessionId); // we may be using mod_jk etc...
	_log.trace("internal session id: "+sessionId);
	// request claims to be associated with a session...
	HttpSessionImpl impl=(HttpSessionImpl)_manager.get(sessionId);
	if (impl==null)
	{
	  _log.trace(sessionId+": not local or passivated");
	  // the session is not available on this node...
	  ManagerProxy proxy=null;
	  if ((proxy=_manager.locate(sessionId))!=null)
	  {
	    _log.trace(sessionId+": is remote - relocating request");
	    proxy.relocateRequest(req, res, _manager);
	  }
	  else
	  {
	    // either:
	    // give up and process request without session here
	    // or:
	    // give up and process request somewhere else

	    _log.warn(sessionId+": must have expired or be an invalid id"); // TODO - incr cache miss counter ?
	    // TODO - no-one has the session - we can either risk a new
	    // one being created here and process locally, proxy to
	    // another node carrying fewer sessions, or strip off
	    // session id and redirect (somehow) back up to lb to make
	    // decision for us...
	    process(req, res, chain, sessionId, null, null);
	  }
	}
	else
	{
	  _log.trace(sessionId+": is local");
	  process(req, res, chain, sessionId, impl.getApplicationLock(), impl.getContainerLock());
	}
      }
    }

  public void
    process(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
	    String sessionId, Sync readLock, Sync writeLock)
    throws IOException, ServletException
  {
    //    assert Thread.currentThread().getName().startsWith("PoolThread-"); // TODO - Jetty only

    // every session has a RWLock
    // priority will be given to Readers
    // application threads are Readers
    // container threads that need to ensure synchronous access to application resources are Writers.

    try
    {
      if (readLock!=null)
	readLock.acquire();

      chain.doFilter(request, response);
    }
    catch (InterruptedException e)
    {
      _log.warn("unexpected interruption", e);
      Thread.interrupted();
      return;
    }
    finally
    {
      if(readLock!=null)
	readLock.release();
      else
      {
	// the session may have just been created - if so we need to
	// look it up and release the read lock...
	javax.servlet.http.HttpSession session=request.getSession();
	if (session!=null)
	{
	  HttpSessionImpl impl=(HttpSessionImpl)_manager.get(_manager.getRoutingStrategy().strip(_manager.getBucketName(), session.getId()));
	  _log.trace(sessionId+"; just created - releasing");
	  if (impl!=null)
	    impl.getApplicationLock().release();
	}
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
//       if (writeLock!=null && _manager.getUsingRequestGroups() && writeLock.attempt(0))
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

  public void
    destroy()
  {
    _distributable=false;
    _manager=null;
  }
}
