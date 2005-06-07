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

import org.activecluster.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.MessageDispatcherConfig;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

// TODO - we need a ThreadPool here - to stop a glut of incoming messages from overwhelming a node.

// IDEA - a single thread of this pool could be responsible for e.g., aggregating all immigrations and batching them
// to cut contention...

/**
 * A Class responsible for the sending of outgoing and dispatching of incoming messages,
 * along with other functionality, like synchronous message exchange etcetera
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MessageDispatcher implements MessageListener {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final Map _map=new HashMap();
    protected final PooledExecutor _executor;

	public MessageDispatcher() {
        _executor=new PooledExecutor(new BoundedBuffer(10), 100); // parameterise
        _executor.setMinimumPoolSize(4);
	    }

    protected MessageDispatcherConfig _config;

    protected Cluster _cluster;
    protected MessageConsumer _clusterConsumer;
    protected MessageConsumer _nodeConsumer;

    public void init(MessageDispatcherConfig config) throws JMSException {
        _config=config;
        _cluster=_config.getCluster();
        boolean excludeSelf;
        excludeSelf=true;
        _clusterConsumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
        _clusterConsumer.setMessageListener(this);
        excludeSelf=false;
        _nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, excludeSelf);
        _nodeConsumer.setMessageListener(this);
    }

    interface Dispatcher {
        void dispatch(ObjectMessage om, Serializable obj) throws Exception;
        void incCount();
        void decCount();
        int getCount();
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

        protected int _count;
        public void incCount() {_count++;}
        public void decCount() {_count--;}
        public synchronized int getCount() {return _count;}
    }



    public Dispatcher register(Object target, String methodName, Class type) {
        try {
            int n=0;
            Method method=target.getClass().getMethod(methodName, new Class[] {ObjectMessage.class, type});
            if (method==null) return null;

            Dispatcher nuw=new TargetDispatcher(target, method);

            Dispatcher old=(Dispatcher)_map.put(type, nuw);
            if (old!=null)
                if (_log.isWarnEnabled()) _log.warn("later registration replaces earlier - multiple dispatch NYI: "+old+" -> "+nuw);

            if (_log.isTraceEnabled()) _log.trace("registering: "+type.getName()+"."+methodName+"()");
            return nuw;
        } catch (NoSuchMethodException e) {
            // ignore
            return null;
        }
    }

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
	      if (rv==null) {
		if (_log.isWarnEnabled()) _log.warn("rendez-vous missed - no-one waiting: "+correlationId);
	      } else {
		do { // should we move this pattern into Utils ?
		  try {
		    rv.attemptRendezvous(om, _timeout);
		  } catch (TimeoutException toe) {
		    if (_log.isWarnEnabled()) _log.warn("rendez-vous timed out: "+correlationId, toe);
		  } catch (InterruptedException ignore) {
		    if (_log.isTraceEnabled()) _log.trace("rendez-vous interruption ignored: "+correlationId);
		  }
		} while (Thread.interrupted()); // TODO - should really subtract from timeout each time...
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

	/**
	 * @param type - the message content type to match this dispatcher
	 * @param rvMap - the Map to be used for thread rendez-vous
	 * @param timeout - the rendez-vous timeout on the dispatcher-side
	 */
	public void register(Class type, Map rvMap, long timeout) {
		_map.put(type, new RendezVousDispatcher(rvMap, timeout));
		if (_log.isTraceEnabled()) _log.trace("registering class: "+type.getName());
	}

    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage=null;
            Serializable serializable=null;
            Dispatcher dispatcher;
            if (
                    message instanceof ObjectMessage &&
                    (objectMessage=(ObjectMessage)message)!=null &&
                    (serializable=objectMessage.getObject())!=null &&
                    (dispatcher=(Dispatcher)_map.get(serializable.getClass()))!=null
            ) {
                do {
                    try {
                        synchronized (dispatcher) {
                            _executor.execute(new DispatchRunner(dispatcher, objectMessage, serializable)); // TODO - pool DispatchRunner ?
                            dispatcher.incCount();
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                } while (Thread.interrupted());
            } else {
                _log.warn("spurious message received: "+message);
            }
        } catch (JMSException e) {
            _log.warn("bad message", e);
        }
    }

	class DispatchRunner implements Runnable {
        protected final Dispatcher _dispatcher;
        protected final ObjectMessage _objectMessage;
        protected final Serializable _serializable;

		public DispatchRunner(Dispatcher dispatcher, ObjectMessage objectMessage, Serializable serializable) {
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

	// Pass this around to avoid the risk of exception everytime we access an ObjectMessage
	public static class Settings {
		public Destination from;
		public Destination to;
		public String correlationId;

		public String toString() {
			return "<Settings: to:"+to+" from:"+from+" corrId:"+correlationId+" >";
		}
	}

    public void sendMessage(Serializable s, Settings settingsIn) throws JMSException {
        // construct and send message...
        ObjectMessage message=_cluster.createObjectMessage();
        message.setJMSReplyTo(settingsIn.from);
        message.setJMSCorrelationID(settingsIn.correlationId);
        message.setObject(s);
        _cluster.send(settingsIn.to, message);
    }
    
    public void replyToMessage(ObjectMessage request, Serializable response) throws JMSException {
        // construct and send message...
        ObjectMessage message=_cluster.createObjectMessage();
        message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
        message.setJMSCorrelationID(request.getJMSCorrelationID());
        message.setObject(response);
        _cluster.send(request.getJMSReplyTo(), message);
    }

	// send a message and then wait a given amount of time for the first response - return it...
	// for use with RendezVousDispatcher... - need to register type beforehand...
	public Serializable exchangeMessages(Serializable request, Map rvMap, Settings settingsInOut, long timeout) {
		Rendezvous rv=new Rendezvous(2); // TODO - can these be reused ?

		// set up a rendez-vous...
		synchronized (rvMap) {
			rvMap.put(settingsInOut.correlationId, rv);
		}

		ObjectMessage om=null;
		Serializable response=null;
		try {
			sendMessage(request, settingsInOut);
			// rendez-vous with response/timeout...
			do {
				try {
					long startTime=System.currentTimeMillis();
					om=(ObjectMessage)rv.attemptRendezvous(null, timeout);
					response=om.getObject();
					settingsInOut.to=om.getJMSReplyTo();
					// om.getJMSDestination() might be the whole cluster, not just this node... - TODO
					settingsInOut.from=_cluster.getLocalNode().getDestination();
					assert settingsInOut.correlationId.equals(om.getJMSCorrelationID());
					long elapsedTime=System.currentTimeMillis()-startTime;
					if (_log.isTraceEnabled()) _log.trace("successful message exchange within timeframe ("+elapsedTime+"<"+timeout+" millis)"); // session does not exist
				} catch (TimeoutException toe) {
					if (_log.isWarnEnabled()) _log.warn("no response to request within timeout ("+timeout+" millis)"); // session does not exist
				} catch (InterruptedException ignore) {
					if (_log.isWarnEnabled()) _log.warn("waiting for response - interruption ignored");
				}
			} while (Thread.interrupted());
		} catch (JMSException e) {
			if (_log.isWarnEnabled()) _log.warn("problem sending request message", e);
		} finally {
			// tidy up rendez-vous
			synchronized (rvMap) {
				rvMap.remove(settingsInOut.correlationId);
			}
		}

		return response;
	}

	public Cluster getCluster(){return _cluster;}

	public MessageConsumer addDestination(Destination destination) throws JMSException {
	    boolean excludeSelf=true;
	    MessageConsumer consumer=_cluster.createConsumer(destination, null, excludeSelf);
	    consumer.setMessageListener(this);
	    return consumer;
	}

	public void removeDestination(MessageConsumer consumer) throws JMSException {
	  consumer.close();
	}
}
