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
package org.codehaus.wadi.group;

import java.io.Serializable;
import java.util.Map;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1563 $
 */
public interface Dispatcher extends MessageListener {
    void init(DispatcherConfig config) throws Exception;

    void register(ServiceEndpoint internalDispatcher);

    void unregister(ServiceEndpoint internalDispatcher, int nbAttemp, long delayMillis);

    void send(Address to, Message message) throws MessageExchangeException;

    void send(Address destination, Serializable request) throws MessageExchangeException;

	void send(Address from, Address to, String outgoingCorrelationId, Serializable body) throws MessageExchangeException;

	Message exchangeSend(Address from, Address to, Serializable body, long timeout) throws MessageExchangeException;

	Message exchangeSend(Address from, Address to, Serializable body, long timeout, String targetCorrelationId) throws MessageExchangeException;

	Message exchangeSend(Address from, Address to, String outgoingCorrelationId, Serializable body, long timeout) throws MessageExchangeException;

	void reply(Address from, Address to, String incomingCorrelationId, Serializable body) throws MessageExchangeException;

	void reply(Message message, Serializable body) throws MessageExchangeException;

	void forward(Message message, Address destination) throws MessageExchangeException;

	void forward(Message message, Address destination, Serializable body) throws MessageExchangeException;

	String nextCorrelationId();

    Map getRendezVousMap();

    Quipu setRendezVous(String correlationId, int numLlamas);

	Message attemptRendezVous(String correlationId, Quipu rv, long timeout);

	// TODO - rather than owning this, we should be given a pointer to it at init()
	// time, and this accessor should be removed...
	PooledExecutor getExecutor();

    LocalPeer getLocalPeer();

    Address getLocalAddress();
    
    Cluster getCluster();

    Address getClusterAddress();

	Map getDistributedState();
    
	void setDistributedState(Map state) throws MessageExchangeException;

	void start() throws MessageExchangeException;

    void stop() throws MessageExchangeException;

	String getPeerName(Address address);

	Message createMessage();

	String getPeerName();
	
    long getInactiveTime();

    int getNumNodes();

    Address getAddress(String name);
}
