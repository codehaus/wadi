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
package org.codehaus.wadi.jetty5;

import java.io.IOException;
import javax.servlet.Filter;
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
import org.codehaus.wadi.web.impl.WebInvocation;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1846 $
 */
public class MyFilter implements Filter {
	protected final Log _log;
	protected final MyServlet _servlet;

	public MyFilter(String name, MyServlet servlet) {
		_log=LogFactory.getLog(getClass().getName()+"#"+name);
		_servlet=servlet;
	}

	public void init(FilterConfig config) {
		_log.info("Filter.init()");
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
	throws ServletException, IOException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest hreq=(HttpServletRequest)req;
			HttpServletResponse hres=(HttpServletResponse)res;
			String sessionId=hreq.getRequestedSessionId();
			if ( _log.isInfoEnabled() ) {
				_log.info("Filter.doFilter("+((sessionId==null)?"":sessionId)+")"+(hreq.isSecure()?" - SECURE":""));
			}
			boolean found;
			try {
				WebInvocation invocation=new WebInvocation();
				invocation.init(hreq, hres, chain, null);
				found = _servlet.getContextualiser().contextualise(invocation, sessionId, null, null, _exclusiveOnly);
			} catch (InvocationException e) {
				throw new ServletException(e);
			}

			// only here for testing...
			if (!found) {
				if (_log.isErrorEnabled()) _log.error("could not locate session: "+sessionId);
				hres.sendError(410, "could not locate session: "+sessionId);
			}

		} else {
			// not http - therefore stateless...
			chain.doFilter(req, res);
		}
	}

	public void destroy() {
		// can't be bothered...
	}

	protected boolean _exclusiveOnly=false;
	public void setExclusiveOnly(boolean exclusiveOnly){_exclusiveOnly=exclusiveOnly;}

}
