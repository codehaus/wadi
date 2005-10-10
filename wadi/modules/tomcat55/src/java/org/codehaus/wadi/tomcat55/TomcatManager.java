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
package org.codehaus.wadi.tomcat55;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.impl.Filter;
import org.codehaus.wadi.impl.SpringManagerFactory;
import org.codehaus.wadi.impl.StandardManager;

public class TomcatManager implements ManagerConfig, Lifecycle, Manager
{
	protected static final Log _log = LogFactory.getLog(TomcatManager.class);

	protected StandardManager _wadi;
	protected Container _container;
	protected boolean _distributable;
	protected int _sessionCounter; // push back into WADI - TODO
	protected int _maxActive; // exactly what does this mean ? - probably only valid for some Evicter types... - TODO
	protected int _expiredSessions; // TODO - wire up
	protected int _rejectedSessions; // TODO - wire up
	protected PropertyChangeSupport _propertyChangeListeners=new PropertyChangeSupport(this); 	// actual notifications are done by aspects...

	// org.codehaus.wadi.ManagerConfig

	public ServletContext getServletContext() {
		return ((Context)_container).getServletContext();
	}

	public void callback(StandardManager manager) {
		// install Listeners ...
		Context context=((Context)_container);

		Object[] sessionListeners=context.getApplicationLifecycleListeners();
		List sll=new ArrayList();
		for (int i=0; i<sessionListeners.length; i++) {
			Object listener=sessionListeners[i];
			if (listener instanceof HttpSessionListener)
				sll.add((HttpSessionListener)listener);
		}
		manager.setSessionListeners((HttpSessionListener[])sll.toArray(new HttpSessionListener[sll.size()]));

		Object[] attributeListeners=context.getApplicationEventListeners();
		List all=new ArrayList();
		for (int i=0; i<attributeListeners.length; i++) {
			Object listener=attributeListeners[i];
			if (listener instanceof HttpSessionAttributeListener)
				all.add((HttpSessionAttributeListener)listener);
		}
		manager.setAttributelisteners((HttpSessionAttributeListener[])all.toArray(new HttpSessionAttributeListener[all.size()]));
	}

	// org.apache.catalina.Lifecycle

	// actual notifications are done by aspects...
	protected LifecycleSupport _lifecycleListeners=new LifecycleSupport(this);

	public void addLifecycleListener(LifecycleListener listener) {
		_lifecycleListeners.addLifecycleListener(listener);
	}

	public LifecycleListener[] findLifecycleListeners() {
		return _lifecycleListeners.findLifecycleListeners();
	}

	public void removeLifecycleListener(LifecycleListener listener) {
		_lifecycleListeners.removeLifecycleListener(listener);
	}

	public void start() throws LifecycleException {
		try {
			InputStream is=getServletContext().getResourceAsStream("/WEB-INF/wadi-web.xml");
			_wadi=(StandardManager)SpringManagerFactory.create(is, "SessionManager", new TomcatSessionFactory(), new TomcatSessionWrapperFactory());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		_wadi.init(this);
		try {
			_wadi.start();

			if (_container==null)
				_log.warn("container not set - fn-ality will be limited");
			else
			{
				Context context=((Context)_container);

				// install Valve
				((StandardContext)context).addValve(new Valve(Pattern.compile("127\\.0\\.0\\.1|192\\.168\\.0\\.\\d{1,3}")));

				// install filter
				String filterName="WadiFilter";
				FilterDef fd=new FilterDef();
				fd.setFilterName(filterName);
				fd.setFilterClass(Filter.class.getName());
				context.addFilterDef(fd);
				FilterMap fm=new FilterMap();
				fm.setFilterName(filterName);
				fm.setURLPattern("/*");
				context.addFilterMap(fm);

//				// is this a distributable webapp ?
//				boolean distributable=context.getDistributable();
//				if (distributable && !_distributable)
//				setDistributable(distributable);
			}

		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	public void stop() throws LifecycleException {
		try {
			_wadi.stop();
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	// org.apache.catalina.Manager

	public Container getContainer() {
		return _container;
	}

	public void setContainer(Container container) {
		_container=container;
	}

	public boolean getDistributable() {
		return _distributable;
	}

	public void setDistributable(boolean distributable) { // TODO - reconcile with WADI's idea of this setting...
		_distributable=distributable;
	}

	public String getInfo() {
		return "<code>&lt;"+getClass().getName()+"&gt;/&lt;1.0b&gt;</code>";
	}

	public int getMaxInactiveInterval() {
		return _wadi.getMaxInactiveInterval();
	}

	public void setMaxInactiveInterval(int interval) {
		_wadi.setMaxInactiveInterval(interval);
	}

	public int getSessionIdLength() {
		return _wadi.getSessionIdFactory().getSessionIdLength();
	}

	public void setSessionIdLength(int sessionIdLength) {
		_wadi.getSessionIdFactory().setSessionIdLength(sessionIdLength);
	}

	public int getSessionCounter() {
		return _sessionCounter;
	}

	public void setSessionCounter(int sessionCounter) {
		_sessionCounter=sessionCounter;
	}

	public int getMaxActive() {
		return _maxActive;
	}

	public void setMaxActive(int maxActive) {
		_maxActive=maxActive;
	}

	public int getActiveSessions() {
		// probably the size of the Memory map... - TODO
		return 0;
	}

	public int getExpiredSessions() {
		return _expiredSessions;
	}

	public void setExpiredSessions(int expiredSessions) {
		_expiredSessions=expiredSessions;
	}

	public int getRejectedSessions() {
		return _rejectedSessions;
	}

	public void setRejectedSessions(int rejectedSessions) {
		_rejectedSessions=rejectedSessions;
	}

	public int getSessionMaxAliveTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setSessionMaxAliveTime(int arg0) {
		// TODO Auto-generated method stub

	}

	public int getSessionAverageAliveTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setSessionAverageAliveTime(int arg0) {
		// TODO Auto-generated method stub

	}

	public void add(Session session) {
		// perhaps hook up to an Immoter ? - TODO
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		_propertyChangeListeners.addPropertyChangeListener(listener);
	}

	public Session createEmptySession() {
		throw new UnsupportedOperationException();
	}

	public Session createSession() {
		return (TomcatSession)_wadi.create();
	}

	public Session createSession(String arg0) {
	  _log.info("createSession("+arg0+")");
	  return (TomcatSession)_wadi.create();
	}

	public Session findSession(String id) throws IOException {
		throw new UnsupportedOperationException();
	}

	public Session[] findSessions() {
		throw new UnsupportedOperationException();
	}

	public void load() throws ClassNotFoundException, IOException {
		// perhaps hook up to promoteToLocal();
	}

	public void remove(Session session) {
		// perhaps hook up to an Emoter - TODO
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		_propertyChangeListeners.removePropertyChangeListener(listener);
	}

	public void unload() throws IOException {
		// perhaps hook up to demoteToShared();
	}

	public void backgroundProcess() {
		// not used - Evicter is attached to a Timer by super()...
	}

}
