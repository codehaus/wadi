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
package org.codehaus.wadi.impl.tomcat;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.util.LifecycleSupport;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.DistributableManager;
import org.codehaus.wadi.impl.Filter;

public class TomcatManager extends DistributableManager implements Manager, Lifecycle {
	
	public TomcatManager(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map sessionMap, Router router, Streamer streamer, boolean accessOnLoad, String clusterUri, String clusterName, String nodeName, HttpProxy httpProxy, InetSocketAddress httpAddress, int numBuckets) {
		super(sessionPool, attributesFactory, valuePool, sessionWrapperFactory, sessionIdFactory, contextualiser, sessionMap, router, streamer, accessOnLoad, clusterUri, clusterName, nodeName, httpProxy, httpAddress, numBuckets);
	}
	
	// org.apache.catalina.Lifecycle
	
	public void start() throws LifecycleException {
		init();
		try {
			super.start();
			
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
			super.stop();
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}
	
	// actual notifications are done by aspects...
	protected LifecycleSupport _lifecycleListeners=new LifecycleSupport(this);
	
	public void addLifecycleListener(LifecycleListener listener) {
		_lifecycleListeners.addLifecycleListener(listener);
	}
	
	public void removeLifecycleListener(LifecycleListener listener) {
		_lifecycleListeners.removeLifecycleListener(listener);
	}
	
	public LifecycleListener[] findLifecycleListeners() {
		return _lifecycleListeners.findLifecycleListeners();
	}
	
	// org.apache.catalina.Manager
	
	public String getInfo() {
		return "<code>&lt;"+getClass().getName()+"&gt;/&lt;1.0b&gt;</code>";
	}
	
	public Session createEmptySession() {
		throw new UnsupportedOperationException();
	}
	
	public Session createSession() {
		return (TomcatSession)create();
	}
	
	public void add(Session session) {
		// perhaps hook up to an Immoter ?
	}
	
	public void remove(Session session) {
		// perhaps hook up to an Emoter
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
	
	public void unload() throws IOException {
		// perhaps hook up to demoteToShared();
	}
	
	public void backgroundProcess() {
		// not used - Evicter is attached to a Timer by super()...
	}
	
	public void setDistributable(boolean distributable) {
		// TODO Auto-generated method stub
		
	}
	
	protected Container _container;
	protected ServletContext _servletContext; // TODO - push back
	
	public Container getContainer() {
		return _container;
	}
	
	public void setContainer(Container container) {
		_container=container;
		_servletContext=((Context)_container).getServletContext();
	}
	
	public ServletContext getServletContext() {
		return _servletContext;
	}
	
	protected DefaultContext _defaultContext;
	
	public DefaultContext getDefaultContext() {
		return _defaultContext;
	}
	
	public void setDefaultContext(DefaultContext defaultContext) {
		_defaultContext=defaultContext;
	}
	
	public int getSessionIdLength() {
		return _sessionIdFactory.getSessionIdLength();
	}
	
	public void setSessionIdLength(int sessionIdLength) {
		_sessionIdFactory.setSessionIdLength(sessionIdLength);
	}
	
	protected int _sessionCounter; // push back into shared code - TODO
	
	public int getSessionCounter() {
		return _sessionCounter;
	}
	
	public void setSessionCounter(int sessionCounter) {
		_sessionCounter=sessionCounter;
	}
	
	protected int _maxActive; // exactly what does this mean ? - probably only valid for some Evicter types...
	
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
	
	protected int _expiredSessions; // TODO - wire up
	
	public int getExpiredSessions() {
		return _expiredSessions;
	}
	
	public void setExpiredSessions(int expiredSessions) {
		_expiredSessions=expiredSessions;
	}
	
	protected int _rejectedSessions; // TODO - wire up
	
	public int getRejectedSessions() {
		return _rejectedSessions;
	}
	
	public void setRejectedSessions(int rejectedSessions) {
		_rejectedSessions=rejectedSessions;
	}
	
	// actual notifications are done by aspects...
	protected PropertyChangeSupport _propertyChangeListeners=new PropertyChangeSupport(this);
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		_propertyChangeListeners.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		_propertyChangeListeners.removePropertyChangeListener(listener);
	}
	
	// org.codehaus.impl.Manager
	
	public void
	setFilter(Filter filter)
	{
		// this is a hack - but the last place where we get a chance to do
		// something during TC's startup routine - we have to wait until
		// this point, because TC does not instantiate these listeners
		// until after starting the session manager...
		initialiseListeners();
		super.setFilter(filter);
	}
	
	protected interface Test { public boolean test(Object o); }
	protected static final Test _sessionListenerTest=new Test(){ public boolean test(Object o){return o instanceof HttpSessionListener;} };
	protected static final Test _attributeListenerTest=new Test(){ public boolean test(Object o){return o instanceof HttpSessionAttributeListener;} };
	
	protected void
	copySubset(Object[] src, List tgt, Test test)
	{
		if (src!=null)
			for (int i=0; i<src.length; i++)
			{
				Object tmp=src[i];
				if (test.test(tmp))
					tgt.add(tmp);
			}
	}
	
	protected void
	initialiseListeners()
	{
		if (_container!=null)
		{
			Context context=((Context)_container);
			copySubset(context.getApplicationLifecycleListeners(), _sessionListeners,   _sessionListenerTest);
			copySubset(context.getApplicationEventListeners(),     _attributeListeners, _attributeListenerTest);
		}
	}
	
	
	// may not need these...
	
	// TODO - These are here so that Container and Session Notification
	// aspects can get a grip on them. If I write the aspects on
	// shared/Manager it pulls all the tomcat stuff into the shared
	// build - no good. Is there not a better way ... ?
//	public void notifySessionCreated(HttpSessionListener listener, HttpSessionEvent event){super.notifySessionCreated(listener,event);}
//	public void notifySessionDestroyed(HttpSessionListener listener, HttpSessionEvent event){super.notifySessionDestroyed(listener, event);}
//	public void notifySessionAttributeAdded(HttpSessionAttributeListener listener, HttpSessionBindingEvent event){super.notifySessionAttributeAdded(listener, event);}
//	public void notifySessionAttributeRemoved(HttpSessionAttributeListener listener, HttpSessionBindingEvent event){super.notifySessionAttributeRemoved(listener, event);}
//	public void notifySessionAttributeReplaced(HttpSessionAttributeListener listener, HttpSessionBindingEvent event){super.notifySessionAttributeReplaced(listener, event);}
	
	//----------------------------------------------------------
	
}
