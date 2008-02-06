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
package org.codehaus.wadi.group.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherContext;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.EnvelopeInterceptor;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.group.QuipuException;
import org.codehaus.wadi.group.ServiceEndpoint;

/**
 * The portable aspects of a Dispatcher implementation
 *
 * @version $Revision: 1595 $
 */
public abstract class AbstractDispatcher implements Dispatcher {
	
	protected final ThreadPool _executor;
    protected Log _log = LogFactory.getLog(getClass());
    protected final Map _rvMap = new ConcurrentHashMap();
    protected final List interceptors;
    private final DispatcherContext context;
    
    private final EnvelopeDispatcherManager inboundEnvelopeDispatcher; 

    public AbstractDispatcher(ThreadPool executor) {
        _executor = executor;
        
        interceptors = new CopyOnWriteArrayList();
        inboundEnvelopeDispatcher = new BasicEnvelopeDispatcherManager(this, _executor);
        context = new BasicDispatcherContext();
    }

	public AbstractDispatcher() {
        this(new PooledExecutorAdapter(10));
	}
    
	public DispatcherContext getContext() {
	    return context;
	}
    
    public void addInterceptor(EnvelopeInterceptor interceptor) {
        interceptor.registerLoopbackEnvelopeListener(this);
        interceptors.add(interceptor);
    }
	
    public void removeInterceptor(EnvelopeInterceptor interceptor) {
        interceptors.remove(interceptor);
        interceptor.unregisterLoopbackEnvelopeListener(this);
    }
    
	public void register(ServiceEndpoint msgDispatcher) {
        inboundEnvelopeDispatcher.register(msgDispatcher);
    }
    
    public void unregister(ServiceEndpoint msgDispatcher, int nbAttemp, long delayMillis) {
        inboundEnvelopeDispatcher.unregister(msgDispatcher, nbAttemp, delayMillis);
    }

    public final void onEnvelope(Envelope envelope) {
        envelope = onInboundEnvelope(envelope);
        if (null == envelope) {
            return;
        }
        
        doOnEnvelope(envelope);
    }

    protected void doOnEnvelope(Envelope envelope) {
        inboundEnvelopeDispatcher.onEnvelope(envelope);
    }
	
	class SimpleCorrelationIDFactory {

        protected int count;

        public synchronized String create() {
            return Integer.toString(count++);
        }

    }

    protected final SimpleCorrelationIDFactory _factory = new SimpleCorrelationIDFactory();

    public void addRendezVousEnvelope(Envelope envelope) {
        String targetCorrelationId = envelope.getTargetCorrelationId();
        if (null == targetCorrelationId) {
            throw new IllegalStateException("No targetCorrelationId");
        }
        Quipu rv= (Quipu) _rvMap.get(targetCorrelationId);
        if (null == rv) {
            if (_log.isTraceEnabled()) {
                _log.trace("no one waiting for [" + targetCorrelationId + "]");
            }            
        } else {
            if (_log.isTraceEnabled()) {
                _log.trace("successful correlation [" + targetCorrelationId + "]");
            }
            rv.putResult(envelope);
        }
    }
    
    public Quipu newRendezVous(int numLlamas) {
        return setRendezVous(_factory.create(), numLlamas);
    }

	public Envelope attemptRendezVous(Quipu rv, long timeout) throws MessageExchangeException {
        Collection messages = attemptMultiRendezVous(rv, timeout);
        if (messages.size() > 1) {
            throw new MessageExchangeException("[" + messages.size() + "] result messages. Expected only one.");
        }
        return (Envelope) messages.iterator().next();
	}
	
    public Collection attemptMultiRendezVous(Quipu rv, long timeout) throws MessageExchangeException {
        Collection response = null;
        try {
            do {
                try {
                    long startTime = System.currentTimeMillis();
                    if (rv.waitFor(timeout)) {
                        response = rv.getResults();
                        long elapsedTime = System.currentTimeMillis()-startTime;
                        if (_log.isTraceEnabled()) {
                            _log.trace("successful message exchange within timeframe (" + elapsedTime + "<" + timeout + " millis) {" + rv + "}");
                        }
                    } else {
                        _log.debug("unsuccessful message exchange within timeframe (" + timeout +" millis) {" + rv + "}", new Exception());
                    }
                } catch (InterruptedException e) {
                    _log.debug("waiting for response - interruption ignored");
                } catch (QuipuException e) {
                    throw new MessageExchangeException(e);
                }
            } while (Thread.interrupted());
        } finally {
            _rvMap.remove(rv.getCorrelationId());
        }
        if (null == response) {
            throw new MessageExchangeException("No correlated messages received within [" + timeout + "]ms");
        }
        return response;
    }
    
	public Envelope exchangeSend(Address to, Serializable body, long timeout) throws MessageExchangeException {
		return exchangeSend(to, body, timeout, null);
	}
	
	public void reply(Envelope envelope, Serializable body) throws MessageExchangeException {
        Envelope reply = createEnvelope();
        reply.setPayload(body);
        reply(envelope, reply);
	}

    public void reply(Envelope request, Envelope reply) throws MessageExchangeException {
        Address from = getCluster().getLocalPeer().getAddress();
        reply.setReplyTo(from);
        Address to = request.getReplyTo();
        reply.setAddress(to);
        String incomingCorrelationId = request.getSourceCorrelationId();
        reply.setTargetCorrelationId(incomingCorrelationId);
        EnvelopeHelper.setAsReply(reply);
        send(to, reply);
    }

    public void send(Address to, Serializable body) throws MessageExchangeException {
        try {
            Envelope envelope = createEnvelope();
            envelope.setReplyTo(getCluster().getLocalPeer().getAddress());
            envelope.setAddress(to);
            envelope.setPayload(body);
            send(to, envelope);
        } catch (Exception e) {
            _log.error("problem sending " + body, e);
        }
    }
	
    public void send(Address target, Quipu quipu, Serializable pojo) throws MessageExchangeException {
        try {
            Envelope envelope = createEnvelope();
            envelope.setReplyTo(getCluster().getLocalPeer().getAddress());
            envelope.setAddress(target);
            envelope.setPayload(pojo);
            envelope.setQuipu(quipu);
            send(target, envelope);
        } catch (Exception e) {
            _log.error("problem sending " + pojo, e);
        }
    }

    public void send(Address source, Address target, Quipu quipu, Serializable pojo) throws MessageExchangeException {
        Envelope envelope = createEnvelope();
        envelope.setReplyTo(source);
        envelope.setAddress(target);
        envelope.setQuipu(quipu);
        envelope.setPayload(pojo);
        send(target, envelope);
	}
	
    public final void send(Address target, Envelope envelope) throws MessageExchangeException {
        envelope = onOutboundEnvelope(envelope);
        if (null == envelope) {
            return;
        }
        
        doSend(target, envelope);
    }

	public Envelope exchangeSend(Address target, Serializable pojo, long timeout, String targetCorrelationId) throws MessageExchangeException {
	    Envelope envelope = createEnvelope();
        envelope.setPayload(pojo);
        return exchangeSend(target, envelope, timeout, targetCorrelationId);
	}
    
    public Envelope exchangeSend(Address target, Envelope envelope, long timeout) throws MessageExchangeException {
        return exchangeSend(target, envelope, timeout, null);
    }

	public Envelope exchangeSend(Address target, Envelope envelope, long timeout, String targetCorrelationId) throws MessageExchangeException {
	    Address from=getCluster().getLocalPeer().getAddress();
	    envelope.setReplyTo(from);
	    envelope.setAddress(target);

	    Quipu rv = newRendezVous(1);
	    envelope.setQuipu(rv);
	    
	    if (targetCorrelationId!=null) {
	        envelope.setTargetCorrelationId(targetCorrelationId);
        }

	    send(target, envelope);
	    return attemptRendezVous(rv, timeout);
	}
	
	public void reply(Address from, Address to, String incomingCorrelationId, Serializable body)
            throws MessageExchangeException {
        Envelope envelope = createEnvelope();
        envelope.setReplyTo(from);
        envelope.setAddress(to);
        envelope.setTargetCorrelationId(incomingCorrelationId);
        envelope.setPayload(body);
        EnvelopeHelper.setAsReply(envelope);
        send(to, envelope);
	}

    protected Quipu setRendezVous(String correlationId, int numLlamas) {
        Quipu rv = new Quipu(numLlamas, correlationId);
        _rvMap.put(correlationId, rv);
        return rv;
    }
    
	protected void hook() {
        AbstractCluster._cluster.set(getCluster());
	}

    public ThreadPool getExecutor() {
        return _executor;
    }

    protected abstract void doSend(Address target, Envelope envelope) throws MessageExchangeException;

    protected Envelope onOutboundEnvelope(Envelope envelope) {
        for (Iterator iter = interceptors.iterator(); iter.hasNext();) {
            EnvelopeInterceptor interceptor = (EnvelopeInterceptor) iter.next();
            envelope = interceptor.onOutboundEnvelope(envelope);
            if (null == envelope) {
                return null;
            }
        }
        return envelope;
    }

    protected Envelope onInboundEnvelope(Envelope envelope) {
        for (Iterator iter = interceptors.iterator(); iter.hasNext();) {
            EnvelopeInterceptor interceptor = (EnvelopeInterceptor) iter.next();
            envelope = interceptor.onInboundEnvelope(envelope);
            if (null == envelope) {
                return null;
            }
        }
        return envelope;
    }

}
