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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ActiveClusterDispatcherConfig;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.dindex.impl.DIndex;

/**
 * A Class responsible for the sending of outgoing and dispatching of incoming messages,
 * along with other functionality, like synchronous message exchange etcetera
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ActiveClusterDispatcher extends AbstractDispatcher implements MessageListener {

	public ActiveClusterDispatcher() {
		super();
    }

	public ActiveClusterDispatcher(String name) {
		this();
		_log=LogFactory.getLog(getClass()+"#"+name);
	}

    protected Cluster _cluster;
    protected MessageConsumer _clusterConsumer;
    protected MessageConsumer _nodeConsumer;

    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#init(org.codehaus.wadi.DispatcherConfig)
	 */
    public void init(DispatcherConfig config) throws Exception {
        super.init(config);
        _cluster=((ActiveClusterDispatcherConfig)_config).getCluster();
        boolean excludeSelf;
        excludeSelf=false;
        _clusterConsumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
        _clusterConsumer.setMessageListener(this);
        excludeSelf=false;
        _nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, excludeSelf);
        _nodeConsumer.setMessageListener(this);
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
                _log.trace("receive {"+getIncomingCorrelationId(objectMessage)+"}: "+getNodeName(message.getJMSReplyTo())+" -> "+getNodeName(message.getJMSDestination())+" : "+body);
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
    			_log.warn("spurious message received: "+message);
    		}
    	} catch (Exception e) {
    		_log.warn("bad message", e);
    	}
    }

    //-----------------------------------------------------------------------------------------------

    protected String getNodeName(Destination destination) {
        Node localNode=_cluster.getLocalNode();
        Destination localDestination=localNode.getDestination();

        if (destination.equals(localDestination))
            return DIndex.getNodeName(localNode);

        Destination clusterDestination=_cluster.getDestination();
        if (destination.equals(clusterDestination))
            return "cluster";

        Node node=null;
        if ((node=(Node)_cluster.getNodes().get(destination))!=null)
            return DIndex.getNodeName(node);

        return "<unknown>";
    }

    //-----------------------------------------------------------------------------------------------

    protected static String _incomingCorrelationIdKey="incomingCorrelationId";

    public String getIncomingCorrelationId(ObjectMessage message) throws Exception {
    	return message.getStringProperty(_incomingCorrelationIdKey);
    }

    public static void setIncomingCorrelationId(ObjectMessage message, String id) throws JMSException {
    	message.setStringProperty(_incomingCorrelationIdKey, id);
    }

    protected static String _outgoingCorrelationIdKey="outgoingCorrelationId";

    public static String getOutgoingCorrelationId(ObjectMessage message) throws JMSException {
    	return message.getStringProperty(_outgoingCorrelationIdKey);
    }

    public static void setOutgoingCorrelationId(ObjectMessage message, String id) throws JMSException {
    	message.setStringProperty(_outgoingCorrelationIdKey, id);
    }


    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#send(javax.jms.Destination, javax.jms.Destination, java.lang.String, java.io.Serializable)
	 */
    public boolean send(Destination from, Destination to, String outgoingCorrelationId, Serializable body) {
        try {
            ObjectMessage om=_cluster.createObjectMessage();
            om.setJMSReplyTo(from);
            om.setJMSDestination(to);
            setOutgoingCorrelationId(om, outgoingCorrelationId);
            om.setObject(body);
            _log.trace("send {"+outgoingCorrelationId+"}: "+getNodeName(from)+" -> "+getNodeName(to)+" : "+body);
            _cluster.send(to, om);
            return true;
        } catch (JMSException e) {
            _log.error("problem sending "+body, e);
            return false;
        }
    }

    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSend(javax.jms.Destination, javax.jms.Destination, java.io.Serializable, long)
	 */
    public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout) {
    	return exchangeSend(from, to, body, timeout, null);
    }

    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSend(javax.jms.Destination, javax.jms.Destination, java.io.Serializable, long, java.lang.String)
	 */
    public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout, String targetCorrelationId) {
    	try {
    		ObjectMessage om=_cluster.createObjectMessage();
    		om.setJMSReplyTo(from);
    		om.setJMSDestination(to);
    		om.setObject(body);
    		String correlationId=nextCorrelationId();
    		setOutgoingCorrelationId(om, correlationId);
    		if (targetCorrelationId!=null)
    			setIncomingCorrelationId(om, targetCorrelationId);
    		Quipu rv=setRendezVous(correlationId, 1);
    		_log.trace("exchangeSend {"+correlationId+"}: "+getNodeName(from)+" -> "+getNodeName(to)+" : "+body);
    		_cluster.send(to, om);
    		return attemptRendezVous(correlationId, rv, timeout);
    	} catch (JMSException e) {
    		_log.error("problem sending "+body, e);
    		return null;
    	}
    }

    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSendLoop(javax.jms.Destination, javax.jms.Destination, java.io.Serializable, long, int)
	 */
    public ObjectMessage exchangeSendLoop(Destination from, Destination to, Serializable body, long timeout, int iterations) {
    	ObjectMessage response=null;
    	for (int i=0; response==null && i<iterations; i++) {
    		response=exchangeSend(from, to, body, timeout);
    		if (response==null)
    			_log.warn("null response - retrying: "+(i+1)+"/"+iterations);
    	}
    	return response;
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
            ObjectMessage om=_cluster.createObjectMessage();
            om.setJMSReplyTo(from);
            om.setJMSDestination(to);
            setIncomingCorrelationId(om, incomingCorrelationId);
            om.setObject(body);
            _log.trace("reply: "+getNodeName(from)+" -> "+getNodeName(to)+" {"+incomingCorrelationId+"} : "+body);
            _cluster.send(to, om);
            return true;
        } catch (JMSException e) {
            _log.error("problem sending "+body, e);
            return false;
        }
    }

    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#reply(javax.jms.ObjectMessage, java.io.Serializable)
	 */
    public boolean reply(ObjectMessage message, Serializable body) {
        try {
            ObjectMessage om=_cluster.createObjectMessage();
        	Destination from=_cluster.getLocalNode().getDestination();
            om.setJMSReplyTo(from);
        	Destination to=message.getJMSReplyTo();
            om.setJMSDestination(to);
            String incomingCorrelationId=getOutgoingCorrelationId(message);
            setIncomingCorrelationId(om, incomingCorrelationId);
            om.setObject(body);
            _log.trace("reply: "+getNodeName(from)+" -> "+getNodeName(to)+" {"+incomingCorrelationId+"} : "+body);
            _cluster.send(to, om);
            return true;
        } catch (JMSException e) {
            _log.error("problem replying to message", e);
            return false;
        }
    }

    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeReply(javax.jms.ObjectMessage, java.io.Serializable, long)
	 */
    public ObjectMessage exchangeReply(ObjectMessage message, Serializable body, long timeout) {
        try {
            ObjectMessage om=_cluster.createObjectMessage();
        	Destination from=_cluster.getLocalNode().getDestination();
            om.setJMSReplyTo(from);
        	Destination to=message.getJMSReplyTo();
            om.setJMSDestination(to);
            String incomingCorrelationId=getOutgoingCorrelationId(message);
            setIncomingCorrelationId(om, incomingCorrelationId);
            String outgoingCorrelationId=nextCorrelationId();
            setOutgoingCorrelationId(om, outgoingCorrelationId);
            om.setObject(body);
            Quipu rv=setRendezVous(outgoingCorrelationId, 1);
            _log.trace("exchangeSend {"+outgoingCorrelationId+"}: "+getNodeName(from)+" -> "+getNodeName(to)+" {"+incomingCorrelationId+"} : "+body);
            _cluster.send(to, om);
            return attemptRendezVous(outgoingCorrelationId, rv, timeout);
        } catch (JMSException e) {
            _log.error("problem sending "+body, e);
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
        } catch (JMSException e) {
            _log.error("problem forwarding message", e);
            return false;
        }
    }
    public Cluster getCluster() {
    	return _cluster;
    }

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#addDestination(javax.jms.Destination)
	 */
	public MessageConsumer addDestination(Destination destination) throws JMSException {
	    boolean excludeSelf=true;
	    MessageConsumer consumer=_cluster.createConsumer(destination, null, excludeSelf);
	    consumer.setMessageListener(this);
	    return consumer;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#removeDestination(javax.jms.MessageConsumer)
	 */
	public void removeDestination(MessageConsumer consumer) throws JMSException {
	  consumer.close();
	}

}
