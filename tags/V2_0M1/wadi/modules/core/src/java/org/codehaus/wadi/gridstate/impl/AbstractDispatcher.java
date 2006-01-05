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
package org.codehaus.wadi.gridstate.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

//TODO - has grown and grown - could do with pruning/refactoring...

/**
 * The portable aspects of a Dispatcher implementation
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractDispatcher implements Dispatcher, MessageListener {
	
	protected final String _nodeName;
	protected final String _clusterName;
	protected final long _inactiveTime;
	protected final Map _map;
	protected final PooledExecutor _executor;
	protected final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.MESSAGES");
	
	public AbstractDispatcher(String nodeName, String clusterName, long inactiveTime) {
		_nodeName=nodeName;
		_clusterName=clusterName;
		_inactiveTime=inactiveTime;
		_map=new HashMap();
		_executor=new PooledExecutor(); // parameterise
		//_executor.setMinimumPoolSize(200);
		//_executor.runWhenBlocked();
		_executor.setThreadFactory(new ThreadFactory() {
			protected int _count;
			
			public synchronized Thread newThread(Runnable runnable) {
				//_log.info("CREATING THREAD: "+_count);
				return new Thread(runnable, "WADI Dispatcher ("+(_count++)+")");
			}
		});
	}
	
	protected Log _log = LogFactory.getLog(getClass());
	protected DispatcherConfig _config;
	protected final Map _rvMap = new ConcurrentHashMap();
	
	public void init(DispatcherConfig config) throws Exception {
		_config=config;
	}
	
	class TargetDispatcher implements InternalDispatcher {
		protected final Object _target;
		protected final Method _method;
		protected final ThreadLocal _pair=new ThreadLocal(){protected Object initialValue() {return new Object[2];}};
		
		public TargetDispatcher(Object target, Method method) {
			_target=target;
			_method=method;
		}
		
		public void dispatch(ObjectMessage om, Serializable obj) throws InvocationTargetException, IllegalAccessException {
			Object[] pair=(Object[])_pair.get();
			pair[0]=om;
			pair[1]=obj;
			_method.invoke(_target, pair);
		}
		
		public String toString() {
			return "<TargetDispatcher: "+_method+" dispatched on: "+_target+">";
		}
		
		protected int _count;
		public void incCount() {_count++;}
		public void decCount() {_count--;}
		public synchronized int getCount() {return _count;}
	}
	
	class NewTargetDispatcher implements InternalDispatcher {
		protected final Object _target;
		protected final Method _method;
		protected final ThreadLocal _singleton=new ThreadLocal(){protected Object initialValue() {return new Object[1];}};
		
		public NewTargetDispatcher(Object target, Method method) {
			_target=target;
			_method=method;
		}
		
		public void dispatch(ObjectMessage message, Serializable request) throws InvocationTargetException, IllegalAccessException {
			Object[] singleton=(Object[])_singleton.get();
			singleton[0]=request;
			Object response=_method.invoke(_target, singleton);
			reply(message, (Serializable)response);
		}
		
		public String toString() {
			return "<TargetDispatcher: "+_method+" dispatched on: "+_target+">";
		}
		
		protected int _count;
		public void incCount() {_count++;}
		public void decCount() {_count--;}
		public synchronized int getCount() {return _count;}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#register(java.lang.Object, java.lang.String, java.lang.Class)
	 */
	public InternalDispatcher register(Object target, String methodName, Class type) {
		try {
			Method method=target.getClass().getMethod(methodName, new Class[] {ObjectMessage.class, type});
			if (method==null) return null;
			
			InternalDispatcher nuw=new TargetDispatcher(target, method);
			
			InternalDispatcher old=(InternalDispatcher)_map.put(type, nuw);
			if (old!=null)
				if (_log.isWarnEnabled()) _log.warn("later registration replaces earlier - multiple dispatch NYI: "+old+" -> "+nuw);
			
			if (_log.isTraceEnabled()) _log.trace("registering: "+type.getName()+"."+methodName+"()");
			return nuw;
		} catch (NoSuchMethodException e) {
			if (_log.isErrorEnabled()) _log.error("no method: " + methodName + "(" + type.getName() + ") on class: " + target.getClass().getName(), e);
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#newRegister(java.lang.Object, java.lang.String, java.lang.Class)
	 */
	public InternalDispatcher newRegister(Object target, String methodName, Class type) {
		try {
			Method method=target.getClass().getMethod(methodName, new Class[] {type});
			if (method==null) return null;
			
			InternalDispatcher nuw=new NewTargetDispatcher(target, method);
			
			InternalDispatcher old=(InternalDispatcher)_map.put(type, nuw);
			if (old!=null)
				if (_log.isWarnEnabled()) _log.warn("later registration replaces earlier - multiple dispatch NYI: "+old+" -> "+nuw);
			
			if (_log.isTraceEnabled()) _log.trace("registering: "+type.getName()+"."+methodName+"()");
			return nuw;
		} catch (NoSuchMethodException e) {
			if (_log.isErrorEnabled()) _log.error("no method: " + methodName + "(" + type.getName() + ") on class: " + target.getClass().getName(), e);
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#deregister(java.lang.String, java.lang.Class, int)
	 */
	public boolean deregister(String methodName, Class type, int timeout) {
		TargetDispatcher td=(TargetDispatcher)_map.get(type);
		if (td==null)
			return false;
		else
			// this isn't failproof - if onMessage has not yet been called,
			// the counter may still read 0 - but it's the best we can do...
			for (int i=timeout; td._count>0 && i>0; i--) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// ignore - TODO
				}
			}
		
		_map.remove(type);
		return td._count<=0;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#newDeregister(java.lang.String, java.lang.Class, int)
	 */
	public boolean newDeregister(String methodName, Class type, int timeout) {
		NewTargetDispatcher td=(NewTargetDispatcher)_map.get(type);
		if (td==null)
			return false;
		else
			// this isn't failproof - if onMessage has not yet been called,
			// the counter may still read 0 - but it's the best we can do...
			for (int i=timeout; td._count>0 && i>0; i--) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// ignore - TODO
				}
			}
		
		_map.remove(type);
		return td._count<=0;
	}
	
	class RendezVousDispatcher implements InternalDispatcher {
		protected final Map _rvMap2;
		protected final long _timeout;
		
		public RendezVousDispatcher(Map rvMap, long timeout) {
			_rvMap2=rvMap;
			_timeout=timeout;
		}
		
		public void dispatch(ObjectMessage om, Serializable obj) throws Exception {
			// rendez-vous with waiting thread...
			String correlationId=getIncomingCorrelationId(om);
			synchronized (_rvMap2) {
				Quipu rv=(Quipu)_rvMap2.get(correlationId);
				if (rv==null) {
					if (_log.isWarnEnabled()) _log.warn("no-one waiting: {"+correlationId+"} - "+obj);
				} else {
					if (_log.isTraceEnabled()) _log.trace("rendez-vous-ing with Quipu: "+correlationId);
					rv.putResult(om);
				}
			}
		}
		
		public String toString() {
			return "<RendezVousDispatcher>";
		}
		
		protected int _count;
		public void incCount() {_count++;}
		public void decCount() {_count--;}
		public synchronized int getCount() {return _count;}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#register(java.lang.Class, long)
	 */
	public void register(Class type, long timeout) {
		_map.put(type, new RendezVousDispatcher(_rvMap, timeout));
		if (_log.isTraceEnabled()) _log.trace("registering class: "+type.getName());
	}
	
	class DispatchRunner implements Runnable {
		protected final InternalDispatcher _dispatcher;
		protected final ObjectMessage _objectMessage;
		protected final Serializable _serializable;
		
		public DispatchRunner(InternalDispatcher dispatcher, ObjectMessage objectMessage, Serializable serializable) {
			_dispatcher=dispatcher;
			_objectMessage=objectMessage;
			_serializable=serializable;
		}
		
		public void run() {
			try {
				_dispatcher.dispatch(_objectMessage, _serializable);
				synchronized (_dispatcher) {
					_dispatcher.decCount();
				}
			} catch (Exception e) {
				_log.error("problem dispatching message", e);
			}
		}
	}
	
	//-----------------------------------------------------------------------------------------------
	
	class SimpleCorrelationIDFactory {
		
		protected final SynchronizedInt _count=new SynchronizedInt(0);
		
		public String create() {
			return Integer.toString(_count.increment());
		}
		
	}
	
	protected final SimpleCorrelationIDFactory _factory=new SimpleCorrelationIDFactory();
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#getRendezVousMap()
	 */
	public Map getRendezVousMap() {
		return _rvMap;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#nextCorrelationId()
	 */
	public String nextCorrelationId() {
		return _factory.create();
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#setRendezVous(java.lang.String, int)
	 */
	public Quipu setRendezVous(String correlationId, int numLlamas) {
		Quipu rv=new Quipu(numLlamas);
		_rvMap.put(correlationId, rv);
		return rv;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#attemptRendezVous(java.lang.String, org.codehaus.wadi.impl.Quipu, long)
	 */
	public ObjectMessage attemptRendezVous(String correlationId, Quipu rv, long timeout) {
		// rendez-vous with response/timeout...
		ObjectMessage response=null;
		try {
			do {
				try {
					long startTime=System.currentTimeMillis();
					if (rv.waitFor(timeout)) {
						response=(ObjectMessage)rv.getResults().toArray()[0]; // TODO - Aargh!
						long elapsedTime=System.currentTimeMillis()-startTime;
						if (_log.isTraceEnabled()) _log.trace("successful message exchange within timeframe ("+elapsedTime+"<"+timeout+" millis) {"+correlationId+"}"); // session does not exist
					} else {
						response=null;
						if (_log.isWarnEnabled()) _log.warn("unsuccessful message exchange within timeframe ("+timeout+" millis) {"+correlationId+"}", new Exception());
					}
				} catch (TimeoutException e) {
					if (_log.isWarnEnabled()) _log.warn("no response to request within timeout ("+timeout+" millis)"); // session does not exist
				} catch (InterruptedException e) {
					if (_log.isWarnEnabled()) _log.warn("waiting for response - interruption ignored");
				}
			} while (Thread.interrupted());
		} finally {
			// tidy up rendez-vous
			if (correlationId!=null)
				_rvMap.remove(correlationId);
		}
		return response;
	}
	
	// TODO - rather than owning this, we should be given a pointer to it at init()
	// time, and this accessor should be removed...
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#getExecutor()
	 */
	public PooledExecutor getExecutor() {
		return _executor;
	}
	
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSendLoop(javax.jms.Destination, javax.jms.Destination, java.io.Serializable, long, int)
	 */
	public ObjectMessage exchangeSendLoop(Destination from, Destination to, Serializable body, long timeout, int iterations) {
		ObjectMessage response=null;
		for (int i=0; response==null && i<iterations; i++) {
			response=exchangeSend(from, to, body, timeout);
			if (response==null)
				if (_log.isWarnEnabled()) _log.warn("null response - retrying: " + ( i + 1 ) + "/" + iterations);
		}
		return response;
	}
	
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSend(javax.jms.Destination, javax.jms.Destination, java.io.Serializable, long)
	 */
	public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout) {
		return exchangeSend(from, to, body, timeout, null);
	}
	
	
	public void onMessage(Message message) {
		try {
			ObjectMessage objectMessage=null;
			Serializable body=null;
			InternalDispatcher dispatcher;
			if (
					message instanceof ObjectMessage &&
					(objectMessage=(ObjectMessage)message)!=null &&
					(body=objectMessage.getObject())!=null &&
					(dispatcher=(InternalDispatcher)_map.get(body.getClass()))!=null
			) {
				if (_messageLog.isTraceEnabled()) _messageLog.trace("incoming: "+body+" {"+getNodeName(message.getJMSReplyTo())+"->"+getNodeName(message.getJMSDestination())+"} - "+getIncomingCorrelationId(objectMessage)+"/"+getOutgoingCorrelationId(objectMessage));
				do {
					try {
						synchronized (dispatcher) {
							_executor.execute(new DispatchRunner(dispatcher, objectMessage, body)); // TODO - pool DispatchRunner ?
							dispatcher.incCount();
						}
					} catch (InterruptedException e) {
						// ignore
					}
				} while (Thread.interrupted());
			} else {
				if (_log.isWarnEnabled()) _log.warn("spurious message received: " + message);
			}
		} catch (Exception e) {
			_log.warn("bad message", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#reply(javax.jms.ObjectMessage, java.io.Serializable)
	 */
	public boolean reply(ObjectMessage message, Serializable body) {
		try {
			ObjectMessage om=createObjectMessage();
			Destination from=getLocalDestination();
			om.setJMSReplyTo(from);
			Destination to=message.getJMSReplyTo();
			om.setJMSDestination(to);
			String incomingCorrelationId=getOutgoingCorrelationId(message);
			setIncomingCorrelationId(om, incomingCorrelationId);
			om.setObject(body);
			if (_log.isTraceEnabled()) _log.trace("reply: " + getNodeName(from) + " -> " + getNodeName(to) + " {" + incomingCorrelationId + "} : " + body);
			send(to, om);
			return true;
		} catch (Exception e) {
			_log.error("problem replying to message", e);
			return false;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#send(javax.jms.Destination, javax.jms.Destination, java.lang.String, java.io.Serializable)
	 */
	public boolean send(Destination from, Destination to, String outgoingCorrelationId, Serializable body) {
		try {
			ObjectMessage om=createObjectMessage();
			om.setJMSReplyTo(from);
			om.setJMSDestination(to);
			setOutgoingCorrelationId(om, outgoingCorrelationId);
			om.setObject(body);
			if (_log.isTraceEnabled()) _log.trace("send {" + outgoingCorrelationId + "}: " + getNodeName(from) + " -> " + getNodeName(to) + " : " + body);
			send(to, om);
			return true;
		} catch (Exception e) {
			if (_log.isErrorEnabled()) _log.error("problem sending " + body, e);
			return false;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSend(javax.jms.Destination, javax.jms.Destination, java.io.Serializable, long, java.lang.String)
	 */
	public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout, String targetCorrelationId) {
		try {
			ObjectMessage om=createObjectMessage();
			om.setJMSReplyTo(from);
			om.setJMSDestination(to);
			om.setObject(body);
			String correlationId=nextCorrelationId();
			setOutgoingCorrelationId(om, correlationId);
			if (targetCorrelationId!=null)
				setIncomingCorrelationId(om, targetCorrelationId);
			Quipu rv=setRendezVous(correlationId, 1);
			if (_log.isTraceEnabled()) _log.trace("exchangeSend {" + correlationId + "}: " + getNodeName(from) + " -> " + getNodeName(to) + " : " + body);
			send(to, om);
			return attemptRendezVous(correlationId, rv, timeout);
		} catch (Exception e) {
			if (_log.isErrorEnabled()) _log.error("problem sending " + body, e);
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSend(javax.jms.Destination, javax.jms.Destination, java.lang.String, java.io.Serializable, long)
	 */
	public ObjectMessage exchangeSend(Destination from, Destination to, String outgoingCorrelationId, Serializable body, long timeout) {
		Quipu rv=null;
		// set up a rendez-vous...
		rv=setRendezVous(outgoingCorrelationId, 1);
		// send the message...
		if (send(from, to, outgoingCorrelationId, body)) {
			return attemptRendezVous(outgoingCorrelationId, rv, timeout);
		} else {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#reply(javax.jms.Destination, javax.jms.Destination, java.lang.String, java.io.Serializable)
	 */
	public boolean reply(Destination from, Destination to, String incomingCorrelationId, Serializable body) {
		try {
			ObjectMessage om=createObjectMessage();
			om.setJMSReplyTo(from);
			om.setJMSDestination(to);
			setIncomingCorrelationId(om, incomingCorrelationId);
			om.setObject(body);
			if (_log.isTraceEnabled()) _log.trace("reply: " + getNodeName(from) + " -> " + getNodeName(to) + " {" + incomingCorrelationId + "} : " + body);
			send(to, om);
			return true;
		} catch (Exception e) {
			if (_log.isErrorEnabled()) _log.error("problem sending " + body, e);
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeReply(javax.jms.ObjectMessage, java.io.Serializable, long)
	 */
	public ObjectMessage exchangeReply(ObjectMessage message, Serializable body, long timeout) {
		try {
			ObjectMessage om=createObjectMessage();
			Destination from=getLocalDestination();
			om.setJMSReplyTo(from);
			Destination to=message.getJMSReplyTo();
			om.setJMSDestination(to);
			String incomingCorrelationId=getOutgoingCorrelationId(message);
			setIncomingCorrelationId(om, incomingCorrelationId);
			String outgoingCorrelationId=nextCorrelationId();
			setOutgoingCorrelationId(om, outgoingCorrelationId);
			om.setObject(body);
			Quipu rv=setRendezVous(outgoingCorrelationId, 1);
			if (_log.isTraceEnabled()) _log.trace("exchangeSend {" + outgoingCorrelationId + "}: " + getNodeName(from) + " -> " + getNodeName(to) + " {" + incomingCorrelationId + "} : " + body);
			send(to, om);
			return attemptRendezVous(outgoingCorrelationId, rv, timeout);
		} catch (Exception e) {
			if (_log.isErrorEnabled()) _log.error("problem sending " + body, e);
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeReplyLoop(javax.jms.ObjectMessage, java.io.Serializable, long)
	 */
	public ObjectMessage exchangeReplyLoop(ObjectMessage message, Serializable body, long timeout) { // TODO
		return exchangeReply(message, body, timeout);
	}
	
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#forward(javax.jms.ObjectMessage, javax.jms.Destination)
	 */
	public boolean forward(ObjectMessage message, Destination destination) {
		try {
			return forward(message, destination, message.getObject());
		} catch (JMSException e) {
			_log.error("problem forwarding message with new body", e);
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#forward(javax.jms.ObjectMessage, javax.jms.Destination, java.io.Serializable)
	 */
	public boolean forward(ObjectMessage message, Destination destination, Serializable body) {
		try {
			return send(message.getJMSReplyTo(), destination, getOutgoingCorrelationId(message), body);
		} catch (Exception e) {
			_log.error("problem forwarding message", e);
			return false;
		}
	}
	
	public String getNodeName() {
		return _nodeName;
	}
	
	public long getInactiveTime() {
		return _inactiveTime;
	}
	
}