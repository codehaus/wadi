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
package org.codehaus.wadi.web.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationContext;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.web.HttpInvocationContext;
import org.codehaus.wadi.web.WADIHttpSession;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1886 $
 */
public class StatefulHttpServletRequestWrapper extends HttpServletRequestWrapper implements HttpInvocationContext {

    protected HttpSession _session;

    public StatefulHttpServletRequestWrapper(Invocation invocation, Session context) {
        super(((WebInvocation) invocation).getHreq());
        _session = context == null ? null : ((WADIHttpSession) context).getWrapper();
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    public HttpSession getSession(boolean create) {
        // TODO - I'm assuming single threaded access to request objects...
        // so no synchronization ?
        if (null == _session) {
            return (_session = ((HttpServletRequest) getRequest()).getSession(create));
        } else {
            return _session;
        }
    }

}
