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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

public class MessageDispatcher implements MessageListener {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final Map _map=new HashMap();
	protected final Cluster _cluster;
	protected final MessageConsumer _consumer;

	public MessageDispatcher(Cluster cluster) throws JMSException {
		_cluster=cluster;
		boolean excludeSelf=true;
	    _consumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
	    _consumer.setMessageListener(this);
	    }
	
	interface Dispatcher {
		void dispatch(ObjectMessage om, Serializable obj) throws Exception;
	}
	
	class TargetDispatcher implements Dispatcher {
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
	}
	
	/**
	 * @param target - the object on which to dispatch messages
	 * @param methodName - the name of the overloaded method to call
	 * @return
	 */
	public int register(Object target, String methodName) {
		// TODO - allow multiple registrations for same message type ?
		int n=0;
		Method[] ms=target.getClass().getMethods();
		for (int i=ms.length-1; i>=0; i--) {
			Method m=ms[i];
			Class[] pts=null;
			if (methodName.equals(m.getName()) && (pts=m.getParameterTypes()).length==2 && pts[0]==ObjectMessage.class) {
				// return type should be void...
				//_log.info("caching method: "+m+" for class: "+pts[1]);
				Dispatcher old;
				Dispatcher nuw=new TargetDispatcher(target, m);
				if ((old=(Dispatcher)_map.put(pts[1], nuw))!=null) {
					_log.warn("later registration replaces earlier - multiple dispatch NYI: "+old+" -> "+nuw);
				}
				n++;
			}
		}
		return n;
	}
	
	class RendezVousDispatcher implements Dispatcher {	
		protected final Map _rvMap;
		protected final long _timeout;
		
		public RendezVousDispatcher(Map rvMap, long timeout) {
			_rvMap=rvMap;
			_timeout=timeout;
		}
		
		public void dispatch(ObjectMessage om, Serializable obj) throws JMSException {
			// rendez-vous with waiting thread...
			String correlationId=om.getJMSCorrelationID();
			synchronized (_rvMap) {
				Rendezvous rv=(Rendezvous)_rvMap.get(correlationId);
				if (rv!=null) {
					do {
						try {
							rv.attemptRendezvous(obj, _timeout);
						} catch (TimeoutException toe) {
							_log.info("rendez-vous timed out: "+correlationId, toe);
						} catch (InterruptedException ignore) {
							_log.info("rendez-vous interruption ignored: "+correlationId);
						}
					} while (Thread.interrupted()); // TODO - should really subtract from timeout each time...
				}
			}
		}

		public String toString() {
			return "<RendezVousDispatcher>";
		}
	}

	/**
	 * @param type - the message content type to match this dispatcher
	 * @param rvMap - the Map to be used for thread rendez-vous
	 * @param timeout - the rendez-vous timeout on the dispatcher-side
	 */
	public void register(Class type, Map rvMap, long timeout) {
		_map.put(type, new RendezVousDispatcher(rvMap, timeout));
	}
	
	public void onMessage(Message message) {
		//TODO -  Threads should be pooled and reusable
		// any allocs should be cached as ThreadLocals and reused 
		// we need a way of indicating which messages should be threaded and which not...
		// how about a producer/consumer arrangement...
		new DispatchThread(message).start();
	}
	
	class DispatchThread extends Thread {
		protected final Message _message;

		public DispatchThread(Message message) {
			_message=message;
		}
		
		public void run() {
			ObjectMessage om=null;
			Serializable obj=null;
			Dispatcher d;
			
			try {
				if (_message instanceof ObjectMessage && (om=(ObjectMessage)_message)!=null && (obj=om.getObject())!=null) {
					if ((d=(Dispatcher)_map.get(obj.getClass()))!=null) {
						d.dispatch(om, obj);
						// if a message is of unrecognised type, we should recurse up its class hierarchy, memoizing the result
						// if we find a class that matches - TODO - This would enable message subtyping...
					} else {
						_log.debug("no dispatcher registered for message: "+obj);
					}
				}
			} catch (Exception e) {
				_log.error("problem processing incoming message", e);
			}
		}
	}

	// send a message and then wait a given amount of time for the first response - return it...
	// for use with RendezVousDispatcher... - need to register type beforehand...
	public Serializable exchangeMessages(String id, Map rvMap, Serializable request, Destination dest, long timeout) {
		String correlationId=_cluster.getLocalNode().getDestination().toString()+"-"+id;
		Rendezvous rv=new Rendezvous(2); // TODO - can these be reused ?
		
		// set up a rendez-vous...
		synchronized (rvMap) {
			rvMap.put(correlationId, rv);
		}
		
		Serializable response=null;
		try {
			// construct and send message...
			ObjectMessage message=_cluster.createObjectMessage();
			message.setJMSReplyTo(_cluster.getDestination());
			message.setJMSCorrelationID(correlationId);
			message.setObject(request);
			_cluster.send(dest, message);
			
			// rendez-vous with response/timeout...
			do {
				try {
					response=(Serializable)rv.attemptRendezvous(null, timeout);
				} catch (TimeoutException toe) {
					_log.info("no response to location query within timeout: "+id); // session does not exist
				} catch (InterruptedException ignore) {
					_log.info("waiting for location response - interruption ignored: "+id);
				}
			} while (Thread.interrupted());
		} catch (JMSException e) {
			_log.warn("problem sending session location query: "+id, e);
		} finally {
			// tidy up rendez-vous
			synchronized (rvMap) {
				rvMap.remove(correlationId);
			}
		}
		
		return response;
	}
}