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
    protected final static String _nodeNameKey = "nodeName";
	
	protected final String _nodeName;
	protected final String _clusterName;
	protected final long _inactiveTime;
	protected final ThreadPool _executor;
	protected final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.MESSAGES");
    protected Log _log = LogFactory.getLog(getClass());
    protected DispatcherConfig _config;
    protected final Map _rvMap = new ConcurrentHashMap();

    private final MessageDispatcherManager inboundMessageDispatcher; 

    public AbstractDispatcher(String clusterName, String nodeName, long inactiveTime, ThreadPool executor) {
        _nodeName=nodeName;
        _clusterName=clusterName;
        _inactiveTime=inactiveTime;
        _executor = executor;
        
        inboundMessageDispatcher = new BasicMessageDispatcherManager(this, _executor);
    }

	public AbstractDispatcher(String clusterName, String nodeName, long inactiveTime) {
        this(clusterName, nodeName, inactiveTime, new PooledExecutorAdapter(10));
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
	
	public Message attemptRendezVous(String correlationId, Quipu rv, long timeout) {
		// rendez-vous with response/timeout...
		Message response=null;
		try {
			do {
				try {
					long startTime=System.currentTimeMillis();
					if (rv.waitFor(timeout)) {
						response=(Message)rv.getResults().toArray()[0]; // TODO - Aargh!
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
	
    public Message exchangeSend(Address to, Object body, long timeout) throws MessageExchangeException {
        return exchangeSend(getLocalAddress(), to, (Serializable) body, timeout);
    }
	
	public Message exchangeSend(Address from, Address to, Serializable body, long timeout) throws MessageExchangeException {
		return exchangeSend(from, to, body, timeout, null);
	}
	
	public void reply(Message message, Serializable body) throws MessageExchangeException {
        Message msg=createMessage();
        Address from=getLocalAddress();
        msg.setReplyTo(from);
        Address to=message.getReplyTo();
        msg.setAddress(to);
        String incomingCorrelationId=message.getOutgoingCorrelationId();
        msg.setIncomingCorrelationId(incomingCorrelationId);
        msg.setPayload(body);
        if (_log.isTraceEnabled()) _log.trace("reply: " + getPeerName(from) + " -> " + getPeerName(to) + " {" + incomingCorrelationId + "} : " + body);
        send(to, msg);
	}

    public void send(Address to, Serializable body) throws MessageExchangeException {
        try {
            Message om = createMessage();
            om.setPayload(body);
            send(to, om);
        } catch (Exception e) {
            if (_log.isErrorEnabled()) _log.error("problem sending " + body, e);
        }
    }
	
	public void send(Address from, Address to, String outgoingCorrelationId, Serializable body) throws MessageExchangeException {
        Message om=createMessage();
        om.setReplyTo(from);
        om.setAddress(to);
        om.setOutgoingCorrelationId(outgoingCorrelationId);
        om.setPayload(body);
        if (_log.isTraceEnabled()) _log.trace("send {" + outgoingCorrelationId + "}: " + getPeerName(from) + " -> " + getPeerName(to) + " : " + body);
        send(to, om);
	}
	
	public Message exchangeSend(Address from, Address to, Serializable body, long timeout, String targetCorrelationId) throws MessageExchangeException {
        Message om=createMessage();
        om.setReplyTo(from);
        om.setAddress(to);
        om.setPayload(body);
        String correlationId=nextCorrelationId();
        om.setOutgoingCorrelationId(correlationId);
        if (targetCorrelationId!=null)
            om.setIncomingCorrelationId(targetCorrelationId);
        Quipu rv=setRendezVous(correlationId, 1);
        if (_log.isTraceEnabled()) _log.trace("exchangeSend {" + correlationId + "}: " + getPeerName(from) + " -> " + getPeerName(to) + " : " + body);
        send(to, om);
        return attemptRendezVous(correlationId, rv, timeout);
	}
	
	public Message exchangeSend(Address from, Address to, String outgoingCorrelationId, Serializable body, long timeout) {
		Quipu rv=null;
		// set up a rendez-vous...
		rv=setRendezVous(outgoingCorrelationId, 1);
		// send the message...
        try {
            send(from, to, outgoingCorrelationId, body);
            return attemptRendezVous(outgoingCorrelationId, rv, timeout);
        } catch (MessageExchangeException e) {
            return null;
        }
	}
	
	public void reply(Address from, Address to, String incomingCorrelationId, Serializable body) throws MessageExchangeException {
        Message om=createMessage();
        om.setReplyTo(from);
        om.setAddress(to);
        om.setIncomingCorrelationId(incomingCorrelationId);
        om.setPayload(body);
        if (_log.isTraceEnabled()) _log.trace("reply: " + getPeerName(from) + " -> " + getPeerName(to) + " {" + incomingCorrelationId + "} : " + body);
        send(to, om);
	}
	
	public void forward(Message message, Address destination) throws MessageExchangeException {
        forward(message, destination, message.getPayload());
	}
	
	public void forward(Message message, Address destination, Serializable body) throws MessageExchangeException {
        send(message.getReplyTo(), destination, message.getOutgoingCorrelationId(), body);
	}

	public String getPeerName() {
		return _nodeName;
	}
	
	public long getInactiveTime() {
		return _inactiveTime;
	}
	
	protected void hook() {
	}
}
