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

import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Dispatcher;
import org.codehaus.wadi.DispatcherConfig;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

public abstract class AbstractDispatcher implements Dispatcher {


	protected final Map _map = new HashMap();
	protected final PooledExecutor _executor;

	public AbstractDispatcher() {
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
            _log.error("no method: "+methodName+"("+type.getName()+") on class: "+target.getClass().getName() , e);
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
            _log.error("no method: "+methodName+"("+type.getName()+") on class: "+target.getClass().getName() , e);
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
                        if (_log.isWarnEnabled()) _log.warn("unsuccessful message exchange within timeframe ("+timeout+" millis) {"+correlationId+"}");
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


    public abstract String getIncomingCorrelationId(ObjectMessage message) throws Exception;

}
