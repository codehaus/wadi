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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.group.ServiceEndpoint;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * The portable aspects of a Dispatcher implementation
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1595 $
 */
public abstract class AbstractDispatcher implements Dispatcher {
	
	protected final ThreadPool _executor;
	protected final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.MESSAGES");
    protected Log _log = LogFactory.getLog(getClass());
    protected final Map _rvMap = new ConcurrentHashMap();

    private final MessageDispatcherManager inboundMessageDispatcher; 

    public AbstractDispatcher(ThreadPool executor) {
        _executor = executor;
        
        inboundMessageDispatcher = new BasicMessageDispatcherManager(this, _executor);
    }

	public AbstractDispatcher(long inactiveTime) {
        this(new PooledExecutorAdapter(10));
	}
	
	public void register(ServiceEndpoint msgDispatcher) {
        inboundMessageDispatcher.register(msgDispatcher);
    }
    
    public void unregister(ServiceEndpoint msgDispatcher, int nbAttemp, long delayMillis) {
        inboundMessageDispatcher.unregister(msgDispatcher, nbAttemp, delayMillis);
    }

    public void onMessage(Envelope message) {
        if (_messageLog.isTraceEnabled()) _messageLog.trace("incoming: "+message.getPayload()+" {"+message.getReplyTo()+"->"+message.getAddress()+"} - "+message.getTargetCorrelationId()+"/"+message.getSourceCorrelationId()+" on "+Thread.currentThread().getName());
        inboundMessageDispatcher.onMessage(message);
    }
	
	class SimpleCorrelationIDFactory {
		
		protected final SynchronizedInt _count=new SynchronizedInt(0);
		
		public String create() {
			return Integer.toString(_count.increment());
		}
		
	}
	
	protected final SimpleCorrelationIDFactory _factory=new SimpleCorrelationIDFactory();

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
    
	public Quipu setRendezVous(String correlationId, int numLlamas) {
        Quipu rv = new Quipu(numLlamas, correlationId);
        _rvMap.put(correlationId, rv);
        return rv;
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
                } catch (TimeoutException e) {
                    _log.debug("no response to request within timeout ("+timeout+" millis)");
                } catch (InterruptedException e) {
                    _log.debug("waiting for response - interruption ignored");
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
    
    public Envelope exchangeSend(Address target, Object pojo, long timeout) throws MessageExchangeException {
        return exchangeSend(target, (Serializable)pojo, timeout);
    }
	
	public Envelope exchangeSend(Address to, Serializable body, long timeout) throws MessageExchangeException {
		return exchangeSend(to, body, timeout, null);
	}
	
	public Envelope exchangeSendLink(Address address, Serializable pojo, long timeout, int retries) throws MessageExchangeException {
		String sourceCorrelationId=_factory.create();
		for (int attempts=0; attempts<retries; attempts++) {
			Envelope response=exchangeSend(address, sourceCorrelationId, pojo, timeout);
			// FIXME:
			// There is a chance that a response will arrive whilst we are resetting the rendez-vous
			// and be missed. At the best this will be less efficient than it might. At the worst we
			// may end up missing out on a response.
			// Consider how we might leave the rendezvous in place and extend its life for the period
			// period of the exchange.
			if (response!=null)
				return response;
		}
		throw new MessageExchangeException("no reply to repeated message");
	}

	public void reply(Envelope message, Serializable body) throws MessageExchangeException {
        Envelope reply = createMessage();
        reply.setPayload(body);
        reply(message, reply);
	}

    public void reply(Envelope request, Envelope reply) throws MessageExchangeException {
        Address from = getCluster().getLocalPeer().getAddress();
        reply.setReplyTo(from);
        Address to = request.getReplyTo();
        reply.setAddress(to);
        String incomingCorrelationId = request.getSourceCorrelationId();
        reply.setTargetCorrelationId(incomingCorrelationId);
        if (_log.isTraceEnabled()) {
            _log.trace("reply [" + reply + "]");
        }
        send(to, reply);
    }

    public void send(Address to, Serializable body) throws MessageExchangeException {
        try {
            Envelope om = createMessage();
            om.setReplyTo(getCluster().getLocalPeer().getAddress());
            om.setAddress(to);
            om.setPayload(body);
            send(to, om);
        } catch (Exception e) {
            _log.error("problem sending " + body, e);
        }
    }
	
    public void send(Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException {
        try {
            Envelope om = createMessage();
            om.setReplyTo(getCluster().getLocalPeer().getAddress());
            om.setAddress(target);
            om.setPayload(pojo);
            om.setSourceCorrelationId(sourceCorrelationId);
            send(target, om);
        } catch (Exception e) {
            _log.error("problem sending " + pojo, e);
        }
    }

    public void send(Address source, Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException {
        Envelope om=createMessage();
        om.setReplyTo(source);
        om.setAddress(target);
        om.setSourceCorrelationId(sourceCorrelationId);
        om.setPayload(pojo);
        if (_log.isTraceEnabled()) {
            _log.trace("send {" + sourceCorrelationId + "}: " + getPeerName(source) + " -> " + getPeerName(target) + " : " + pojo);
        }
        send(target, om);
	}
	
	public Envelope exchangeSend(Address target, Serializable pojo, long timeout, String targetCorrelationId) throws MessageExchangeException {
	    Envelope message = createMessage();
        message.setPayload(pojo);
        return exchangeSend(target, message, timeout, targetCorrelationId);
	}
    
    public Envelope exchangeSend(Address target, Envelope om, long timeout) throws MessageExchangeException {
        return exchangeSend(target, om, timeout, null);
    }

	public Envelope exchangeSend(Address target, Envelope om, long timeout, String targetCorrelationId) throws MessageExchangeException {
	    Address from=getCluster().getLocalPeer().getAddress();
	    om.setReplyTo(from);
	    om.setAddress(target);
	    Quipu rv= newRendezVous(1);
	    om.setSourceCorrelationId(rv.getCorrelationId());
	    if (targetCorrelationId!=null) {
	        om.setTargetCorrelationId(targetCorrelationId);
        }
	    if (_log.isTraceEnabled()) {
            _log.trace("exchangeSend [" + om + "]");
        }
	    send(target, om);
	    return attemptRendezVous(rv, timeout);
	}
	
	public Envelope exchangeSend(Address target, String sourceCorrelationId, Serializable pojo, long timeout) {
		Quipu rv=null;
		// set up a rendez-vous...
		rv=setRendezVous(sourceCorrelationId, 1);
		// send the message...
        try {
            send(getCluster().getLocalPeer().getAddress(), target, sourceCorrelationId, pojo);
            return attemptRendezVous(rv, timeout);
        } catch (MessageExchangeException e) {
            return null;
        }
	}
	
	public void reply(Address from, Address to, String incomingCorrelationId, Serializable body) throws MessageExchangeException {
        Envelope om=createMessage();
        om.setReplyTo(from);
        om.setAddress(to);
        om.setTargetCorrelationId(incomingCorrelationId);
        om.setPayload(body);
        if (_log.isTraceEnabled()) {
            _log.trace("reply: " + getPeerName(from) + " -> " + getPeerName(to) + " {" + incomingCorrelationId + "} : " + body);
        }
        send(to, om);
	}
	
	public void forward(Envelope message, Address destination) throws MessageExchangeException {
        forward(message, destination, message.getPayload());
	}
	
	public void forward(Envelope message, Address destination, Serializable body) throws MessageExchangeException {
        send(message.getReplyTo(), destination, message.getSourceCorrelationId(), body);
	}

	protected void hook() {
        AbstractCluster._cluster.set(getCluster());
	}

    public ThreadPool getExecutor() {
        return _executor;
    }
    
}
