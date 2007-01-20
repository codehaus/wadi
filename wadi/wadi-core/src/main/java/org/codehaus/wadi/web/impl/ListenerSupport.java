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

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SessionMonitor;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.web.WADIHttpSessionListener;
import org.codehaus.wadi.web.WebSessionConfig;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ListenerSupport {

	protected final Log _log = LogFactory.getLog(getClass());
	protected final List _sessionListeners = new ArrayList();
	protected final List _attributeListeners = new ArrayList();

	public synchronized void addEventListener(EventListener listener) throws IllegalArgumentException, IllegalStateException {
//		if (isStarted())
//			throw new IllegalStateException("EventListeners must be added before a Session Manager starts");

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

	public synchronized void removeEventListener(EventListener listener) throws IllegalStateException {
//		if (isStarted())
//			throw new IllegalStateException("EventListeners may not be removed while a Session Manager is running");

		boolean known=false;
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

	public void installListeners(StandardManager manager, SessionMonitor sessionMonitor, WebSessionConfig sessionConfig) {
	    HttpSessionListener[] listeners = (HttpSessionListener[])_sessionListeners.toArray(new HttpSessionListener[_sessionListeners.size()]);
        sessionMonitor.addSessionListener(new WADIHttpSessionListener(listeners));
        
        HttpSessionAttributeListener[] attributeListeners = (HttpSessionAttributeListener[]) _attributeListeners
                .toArray(new HttpSessionAttributeListener[_attributeListeners.size()]);
        sessionConfig.setAttributeListeners(attributeListeners);
	}

}
