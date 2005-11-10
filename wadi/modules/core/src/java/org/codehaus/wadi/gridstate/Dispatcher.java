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
package org.codehaus.wadi.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;
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


    void init(DispatcherConfig config) throws Exception;

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

	// TODO - rather than owning this, we should be given a pointer to it at init()
	// time, and this accessor should be removed...
	PooledExecutor getExecutor();
	
	Destination getLocalDestination();

	void setDistributedState(Map state) throws Exception;
	
	void start() throws Exception;
	void stop() throws Exception;
	
	String getNodeName(Destination destination);
	
    String getIncomingCorrelationId(ObjectMessage message) throws Exception;
    void setIncomingCorrelationId(ObjectMessage message, String correlationId) throws Exception;
    String getOutgoingCorrelationId(ObjectMessage message) throws Exception;
    void setOutgoingCorrelationId(ObjectMessage message, String correlationId) throws Exception;
	void send(Destination to, ObjectMessage message) throws Exception;
	ObjectMessage createObjectMessage() throws Exception;

}
