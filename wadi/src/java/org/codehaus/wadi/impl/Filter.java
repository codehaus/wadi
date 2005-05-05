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
package org.codehaus.wadi.impl;

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
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.HttpServletRequestWrapperPool;
import org.codehaus.wadi.PoolableHttpServletRequestWrapper;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.impl.Manager;

public class Filter implements javax.servlet.Filter {

    protected final Log _log = LogFactory.getLog(getClass());

    protected Manager _manager;
    protected boolean _distributable;
    protected Contextualiser _contextualiser;
    protected Router _router;
    protected HttpServletRequestWrapperPool _pool=new DummyStatefulHttpServletRequestWrapperPool(); // TODO - init from _manager

    // Filter Lifecycle

    public void
      init(FilterConfig filterConfig) throws ServletException
    {
      _manager=(Manager)filterConfig.getServletContext().getAttribute(Manager.class.getName());
      if (_manager==null)
        _log.fatal("Manager not found");
      else
          if (_log.isInfoEnabled())_log.info("Manager found: "+_manager);

      _manager.setFilter(this);
      _distributable=_manager.getDistributable();
      _contextualiser=_manager.getContextualiser();
      _router=_manager.getRouter();
    }

    public void
    destroy()
    {
        _pool=null;
        _contextualiser=null;
        _distributable=false;
        _manager=null;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest)
            doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
        else // this one is not HTTP - therefore it is and will remain, stateless - not for us...
            chain.doFilter(request, response);
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String sessionId=request.getRequestedSessionId();
        if (_log.isTraceEnabled()) _log.trace("potentially stateful request: "+sessionId);

        if (sessionId==null) {
            // no session yet - but may initiate one...
            PoolableHttpServletRequestWrapper wrapper=_pool.take();
            wrapper.init(request, null);
            chain.doFilter(wrapper, response);
            wrapper.destroy();
            _pool.put(wrapper);
        } else {
            // already associated with a session...
            sessionId=_router.strip(sessionId); // strip off any routing info...
            // _router.rerouteCookie(request, response, null, sessionId); // TODO - do we 'restick' client ?
            _contextualiser.contextualise(request, response, chain, sessionId, null, null, false);
        }
    }
}
