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

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1563 $
 */
public interface Dispatcher extends MessageListener {
    
    void init(DispatcherConfig config) throws Exception;

    void register(ServiceEndpoint internalDispatcher);

    void unregister(ServiceEndpoint internalDispatcher, int nbAttemp, long delayMillis);

    void send(Address to, Message message) throws MessageExchangeException;

    /**
     * send a Serializable pojo to an Address - async - no reply expected
     * 
     * @param target The Address to which we are sending the Message
     * @param pojo The Object that we want to send inside it
     * @throws MessageExchangeException
     */
    void send(Address target, Serializable pojo) throws MessageExchangeException;

	void send(Address from, Address to, String outgoingCorrelationId, Serializable body) throws MessageExchangeException;

	/**
     * send a Serializable 'pojo' to 'target' Address - sync - and wait for a reply, but not necessarily from the 'target' Address.
     * 
	 * @param target The address to which we are sending the Message
	 * @param pojo The object that we want to send inside it
	 * @param timeout The length of time that we are willing to wait for a reply
	 * @return a response in the form of a Message
	 * @throws MessageExchangeException
	 */
	Message exchangeSend(Address target, Serializable pojo, long timeout) throws MessageExchangeException;

	Message exchangeSend(Address from, Address to, Serializable body, long timeout, String targetCorrelationId) throws MessageExchangeException;

	Message exchangeSend(Address from, Address to, String outgoingCorrelationId, Serializable body, long timeout) throws MessageExchangeException;

	void reply(Address from, Address to, String incomingCorrelationId, Serializable body) throws MessageExchangeException;

	void reply(Message message, Serializable body) throws MessageExchangeException;

	void forward(Message message, Address destination) throws MessageExchangeException;

    // can we lose this ?
	void forward(Message message, Address destination, Serializable body) throws MessageExchangeException;

	String nextCorrelationId();

    Map getRendezVousMap();

    Quipu setRendezVous(String correlationId, int numLlamas);

	Message attemptRendezVous(String correlationId, Quipu rv, long timeout);

	void start() throws MessageExchangeException;

    void stop() throws MessageExchangeException;

	Message createMessage();

    Cluster getCluster();
    
    // lose these - clients should use equiv method on Cluster
    
    //Peer getPeer(Address address); // should be Address.getPeer()
    
    // should be implemented as getPeer(Address address).getName() - but then we will need to introduce a ClusterPeer ?
    String getPeerName(Address address);
    
    long getInactiveTime();

    
    // needed by VM impl - should be equiv to calling _localPeer.setState(map)
    void setDistributedState(Map state) throws MessageExchangeException;

    // needed by BasicReplicaStorage stuff - can we lose it ?
    Address getAddress(String name);

    }
