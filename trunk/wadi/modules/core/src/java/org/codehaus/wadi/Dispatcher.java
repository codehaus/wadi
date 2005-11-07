package org.codehaus.wadi;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;

import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public interface Dispatcher {

    interface InternalDispatcher {
        void dispatch(ObjectMessage om, Serializable obj) throws Exception;
        void incCount();
        void decCount();
        int getCount();
    }


    void init(DispatcherConfig config) throws JMSException;

	InternalDispatcher register(Object target, String methodName, Class type);

	InternalDispatcher newRegister(Object target, String methodName, Class type);

	boolean deregister(String methodName, Class type, int timeout);

	boolean newDeregister(String methodName, Class type, int timeout);

	void register(Class type, long timeout);

	boolean send(Destination from, Destination to, String outgoingCorrelationId, Serializable body);

	ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout);

	ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout, String targetCorrelationId);

	ObjectMessage exchangeSendLoop(Destination from, Destination to, Serializable body, long timeout, int iterations);

	ObjectMessage exchangeSend(Destination from, Destination to, String outgoingCorrelationId, Serializable body, long timeout);

	boolean reply(Destination from, Destination to, String incomingCorrelationId, Serializable body);

	boolean reply(ObjectMessage message, Serializable body);

	ObjectMessage exchangeReply(ObjectMessage message, Serializable body, long timeout);

	ObjectMessage exchangeReplyLoop(ObjectMessage message, Serializable body, long timeout);

	boolean forward(ObjectMessage message, Destination destination);

	boolean forward(ObjectMessage message, Destination destination, Serializable body);

	Map getRendezVousMap();

	String nextCorrelationId();

	Quipu setRendezVous(String correlationId, int numLlamas);

	ObjectMessage attemptRendezVous(String correlationId, Quipu rv, long timeout);

	MessageConsumer addDestination(Destination destination) throws JMSException;

	void removeDestination(MessageConsumer consumer) throws JMSException;

	// TODO - rather than owning this, we should be given a pointer to it at init()
	// time, and this accessor should be removed...
	PooledExecutor getExecutor();

}