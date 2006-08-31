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
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.Message;
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
    protected DispatcherConfig _config;
    protected final Map _rvMap = new ConcurrentHashMap();

    private final MessageDispatcherManager inboundMessageDispatcher; 

    public AbstractDispatcher(ThreadPool executor) {
        _executor = executor;
        
        inboundMessageDispatcher = new BasicMessageDispatcherManager(this, _executor);
    }

	public AbstractDispatcher(long inactiveTime) {
        this(new PooledExecutorAdapter(10));
	}
	
	public void init(DispatcherConfig config) throws Exception {
		_config=config;
	}
	
	public void register(ServiceEndpoint msgDispatcher) {
        inboundMessageDispatcher.register(msgDispatcher);
    }
    
    public void unregister(ServiceEndpoint msgDispatcher, int nbAttemp, long delayMillis) {
        inboundMessageDispatcher.unregister(msgDispatcher, nbAttemp, delayMillis);
    }

    public void onMessage(Message message) {
        if (_messageLog.isTraceEnabled()) _messageLog.trace("incoming: "+message.getPayload()+" {"+message.getReplyTo()+"->"+message.getAddress()+"} - "+message.getTargetCorrelationId()+"/"+message.getSourceCorrelationId()+" on "+Thread.currentThread().getName());
        inboundMessageDispatcher.onMessage(message);
    }
	
	//-----------------------------------------------------------------------------------------------
	class SimpleCorrelationIDFactory {
		
		protected final SynchronizedInt _count=new SynchronizedInt(0);
		
		public String create() {
			return Integer.toString(_count.increment());
		}
		
	}
	
	protected final SimpleCorrelationIDFactory _factory=new SimpleCorrelationIDFactory();

	public Map getRendezVousMap() {
		return _rvMap;
	}
	
	public String nextCorrelationId() {
		return _factory.create();
	}
	
	public Quipu setRendezVous(String correlationId, int numLlamas) {
		Quipu rv=new Quipu(numLlamas);
		_rvMap.put(correlationId, rv);
		return rv;
	}
	
	public Message attemptRendezVous(String correlationId, Quipu rv, long timeout) throws MessageExchangeException {
        Collection messages = attemptMultiRendezVous(correlationId, rv, timeout);
        if (messages.size() > 1) {
            throw new MessageExchangeException("[" + messages.size() + "] result messages. Expected only one.");
        }
        return (Message) messages.iterator().next();
	}
	
    public Collection attemptMultiRendezVous(String correlationId, Quipu rv, long timeout) throws MessageExchangeException {
        Collection response = null;
        try {
            do {
                try {
                    long startTime = System.currentTimeMillis();
                    if (rv.waitFor(timeout)) {
                        response = rv.getResults();
                        long elapsedTime = System.currentTimeMillis()-startTime;
                        if (_log.isTraceEnabled()) {
                            _log.trace("successful message exchange within timeframe (" + elapsedTime + 
                                    "<" + timeout + " millis) {" + correlationId + "}");
                        }
                    } else {
                        if (_log.isWarnEnabled()) {
                            _log.warn("unsuccessful message exchange within timeframe (" + timeout + 
                                    " millis) {" + correlationId + "}", new Exception());
                        }
                    }
                } catch (TimeoutException e) {
                    if (_log.isWarnEnabled()) {
                        _log.warn("no response to request within timeout ("+timeout+" millis)");
                    }
                } catch (InterruptedException e) {
                    if (_log.isWarnEnabled()) {
                        _log.warn("waiting for response - interruption ignored");
                    }
                }
            } while (Thread.interrupted());
        } finally {
            // tidy up rendez-vous
            if (null != correlationId) {
                _rvMap.remove(correlationId);
            }
        }
        if (null == response) {
            throw new MessageExchangeException("No correlated messages received within [" + timeout + "]ms");
        }
        return response;
    }
    
    public Message exchangeSend(Address target, Object pojo, long timeout) throws MessageExchangeException {
        return exchangeSend(target, (Serializable)pojo, timeout);
    }
	
	public Message exchangeSend(Address to, Serializable body, long timeout) throws MessageExchangeException {
		return exchangeSend(to, body, timeout, null);
	}
	
	public void reply(Message message, Serializable body) throws MessageExchangeException {
        Message msg=createMessage();
        Address from=getCluster().getLocalPeer().getAddress();
        msg.setReplyTo(from);
        Address to=message.getReplyTo();
        msg.setAddress(to);
        String incomingCorrelationId=message.getSourceCorrelationId();
        msg.setTargetCorrelationId(incomingCorrelationId);
        msg.setPayload(body);
        if (_log.isTraceEnabled()) _log.trace("reply: " + getPeerName(from) + " -> " + getPeerName(to) + " {" + incomingCorrelationId + "} : " + body);
        send(to, msg);
	}

    public void send(Address to, Serializable body) throws MessageExchangeException {
        try {
            Message om = createMessage();
            om.setReplyTo(getCluster().getLocalPeer().getAddress());
            om.setAddress(to);
            om.setPayload(body);
            send(to, om);
        } catch (Exception e) {
            if (_log.isErrorEnabled()) _log.error("problem sending " + body, e);
        }
    }
	
    public void send(Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException {
        try {
            Message om = createMessage();
            om.setReplyTo(getCluster().getLocalPeer().getAddress());
            om.setAddress(target);
            om.setPayload(pojo);
            om.setSourceCorrelationId(sourceCorrelationId);
            send(target, om);
        } catch (Exception e) {
            if (_log.isErrorEnabled()) _log.error("problem sending " + pojo, e);
        }
    }

    public void send(Address source, Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException {
        Message om=createMessage();
        om.setReplyTo(source);
        om.setAddress(target);
        om.setSourceCorrelationId(sourceCorrelationId);
        om.setPayload(pojo);
        if (_log.isTraceEnabled()) _log.trace("send {" + sourceCorrelationId + "}: " + getPeerName(source) + " -> " + getPeerName(target) + " : " + pojo);
        send(target, om);
	}
	
	public Message exchangeSend(Address target, Serializable body, long timeout, String targetCorrelationId) throws MessageExchangeException {
        Address from=getCluster().getLocalPeer().getAddress();
        Message om=createMessage();
        om.setReplyTo(from);
        om.setAddress(target);
        om.setPayload(body);
        String sourceCorrelationId=nextCorrelationId();
        om.setSourceCorrelationId(sourceCorrelationId);
        if (targetCorrelationId!=null)
            om.setTargetCorrelationId(targetCorrelationId);
        Quipu rv=setRendezVous(sourceCorrelationId, 1);
        if (_log.isTraceEnabled()) _log.trace("exchangeSend {" + sourceCorrelationId + "}: " + getPeerName(from) + " -> " + getPeerName(target) + " : " + body);
        send(target, om);
        return attemptRendezVous(sourceCorrelationId, rv, timeout);
	}
	
	public Message exchangeSend(Address target, String sourceCorrelationId, Serializable pojo, long timeout) {
		Quipu rv=null;
		// set up a rendez-vous...
		rv=setRendezVous(sourceCorrelationId, 1);
		// send the message...
        try {
            send(getCluster().getLocalPeer().getAddress(), target, sourceCorrelationId, pojo);
            return attemptRendezVous(sourceCorrelationId, rv, timeout);
        } catch (MessageExchangeException e) {
            return null;
        }
	}
	
	public void reply(Address from, Address to, String incomingCorrelationId, Serializable body) throws MessageExchangeException {
        Message om=createMessage();
        om.setReplyTo(from);
        om.setAddress(to);
        om.setTargetCorrelationId(incomingCorrelationId);
        om.setPayload(body);
        if (_log.isTraceEnabled()) _log.trace("reply: " + getPeerName(from) + " -> " + getPeerName(to) + " {" + incomingCorrelationId + "} : " + body);
        send(to, om);
	}
	
	public void forward(Message message, Address destination) throws MessageExchangeException {
        forward(message, destination, message.getPayload());
	}
	
	public void forward(Message message, Address destination, Serializable body) throws MessageExchangeException {
        send(message.getReplyTo(), destination, message.getSourceCorrelationId(), body);
	}

	protected void hook() {
        AbstractCluster._cluster.set(getCluster());
	}
}
