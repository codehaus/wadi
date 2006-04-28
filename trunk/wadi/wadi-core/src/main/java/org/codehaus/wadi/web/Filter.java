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
package org.codehaus.wadi.web;

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
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.impl.StandardManager;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class Filter implements javax.servlet.Filter {
    
    protected final Log _log = LogFactory.getLog(getClass());
    
    protected StandardManager _manager;
    
    // Filter
    
    public void init(FilterConfig filterConfig) throws ServletException {
        _manager=(StandardManager)filterConfig.getServletContext().getAttribute(StandardManager.class.getName());
        if (_manager==null)
            _log.fatal("Manager not found");
        else
            if (_log.isInfoEnabled())_log.info("Manager found: "+_manager);
        
        _manager.setFilter(this);
    }
    
    public void destroy() {
        _manager=null;
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            WebInvocation invocation=WebInvocation.getThreadLocalInstance();
            invocation.init((HttpServletRequest)request, (HttpServletResponse)response, filterChain);
            try {
                _manager.around(invocation);
            } catch (InvocationException e) {
                Throwable throwable = e.getCause();
                if (throwable instanceof IOException) {
                    throw (IOException) throwable;
                } else if (throwable instanceof ServletException) {
                    throw (ServletException) throwable;
                } else {
                    throw new ServletException(e);
                }
            } catch (IOException e) {
                throw e;   
            } catch (ServletException e) {
                throw e;
            } catch (Exception e) {
                _log.error("unexpected exception", e);
                throw new ServletException(e);
            }
        } else // this is not HTTP - therefore it is stateless - not for us...
            filterChain.doFilter(request, response);
    }
    
}
