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
import java.util.Collection;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1563 $
 */
public interface Dispatcher extends MessageListener {
    void register(ServiceEndpoint internalDispatcher);

    void unregister(ServiceEndpoint internalDispatcher, int nbAttemp, long delayMillis);

    /**
     * Send a ready-made Message to the Peer at the 'target' Address.
     * 
     * @param target
     *            The Address of the Peer to which the Message should be sent
     * @param message
     *            The Message itself
     * @throws MessageExchangeException
     */
    void send(Address target, Envelope message) throws MessageExchangeException;

    /**
     * send a Serializable pojo to an Address - async - no reply expected
     * 
     * @param target
     *            The Address to which we are sending the Message
     * @param pojo
     *            The Object that we want to send inside it
     * @throws MessageExchangeException
     */
    void send(Address target, Serializable pojo) throws MessageExchangeException;

    /**
     * send a Serializable 'pojo' to a 'target' Address, with the Message's
     * replyTo field containing the local cluster Address - async - although we are
     * expecting a reply, which will be matched with the sourceCorrelationId.
     * The code that calls this method assumes responsibility for coordinating
     * this thread with the expected reply.
     * 
     * @param target
     *            The Address of the Peer to which this message is to be sent
     * @param sourceCorrelationId
     *            The correlationId which will be used to match a response on
     *            the source Peer
     * @param pojo
     *            The object to be sent in the Message
     * @throws MessageExchangeException
     */
    void send(Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException;
    
    /**
     * send a Serializable 'pojo' to a 'target' Address, with the Message's
     * replyTo field containing the 'source' Address - async - although we are
     * expecting a reply, which will be matched with the sourceCorrelationId.
     * The code that calls this method assumes responsibility for coordinating
     * this thread with the expected reply.
     * 
     * @param source
     *            The Address of the Peer to which the reply should be sent
     * @param target
     *            The Address of the Peer to which this message is to be sent
     * @param sourceCorrelationId
     *            The correlationId which will be used to match a response on
     *            the source Peer
     * @param pojo
     *            The object to be sent in the Message
     * @throws MessageExchangeException
     */
    void send(Address source, Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException;

    /**
     * Send a Serializable 'pojo' to 'target' Address - sync - and wait for a
     * reply. The outgoing message will be accompanied by a correlation id. An
     * incoming message, carrying the same correlation id, arriving within the
     * specified timeframe, will be taken as the response.
     * 
     * @param target
     *            The address to which we are sending the Message
     * @param pojo
     *            The object that we want to send inside it
     * @param timeout
     *            The length of time that we are willing to wait for a reply
     * @return a response in the form of a Message
     * @throws MessageExchangeException
     */
    Envelope exchangeSend(Address target, Serializable pojo, long timeout) throws MessageExchangeException;

    /**
     * Send a Serializable 'pojo' to a 'target' Address - sync - and wait for a
     * reply. This message will be accompanied by a 'targetCorrelationId' which
     * will be used to match it at the target end, so that it may itself be
     * interpreted as an incoming response to a previously outgoing request,
     * thus allowing us to create 'message chains'.
     * 
     * @param target
     *            The address to which we are sending the message
     * @param targetCorrelationId
     *            Explicitly identifies the message to which we wish to reply
     * @param pojo
     *            The object that we wish to send
     * @param timeout
     *            The length of time that we are willing to wait for a reply
     * @return a response in the form of a Message
     * @throws MessageExchangeException
     */
    Envelope exchangeSend(Address target, Serializable body, long timeout, String targetCorrelationId) throws MessageExchangeException;

    /**
     * Send a Serializable 'pojo' to a 'target' Address - sync - and wait for a
     * reply. This message will be accompanied by a 'sourceCorrelationId' which
     * will be used to match a reply at the source end. This allows explicit
     * control of the 'sourceCorrelationId', which would normally be dynamically
     * allocated.
     * 
     * @param target
     *            The address to which we are sending the message
     * @param sourceCorrelationId
     *            Explicitly identifies this message so that it may receive a
     *            reply.
     * @param pojo
     *            The object that we wish to send
     * @param timeout
     *            The length of time that we are willing to wait for a reply
     * @return a response in the form of a Message
     * @throws MessageExchangeException
     */
    Envelope exchangeSend(Address target, String sourceCorrelationId, Serializable pojo, long timeout) throws MessageExchangeException;

    Envelope exchangeSend(Address target, Envelope om, long timeout) throws MessageExchangeException;

    Envelope exchangeSend(Address target, Envelope om, long timeout, String targetCorrelationId) throws MessageExchangeException;
    
    void reply(Address from, Address to, String sourceCorrelationId, Serializable body) throws MessageExchangeException;

    void reply(Envelope message, Serializable body) throws MessageExchangeException;

    void reply(Envelope request, Envelope reply) throws MessageExchangeException;

    void forward(Envelope message, Address destination) throws MessageExchangeException;

    // can we lose this ?
    void forward(Envelope message, Address destination, Serializable body) throws MessageExchangeException;

    void addRendezVousEnvelope(Envelope envelope);

    Quipu newRendezVous(int numLlamas);

    Envelope attemptRendezVous(Quipu rv, long timeout) throws MessageExchangeException;

    Collection attemptMultiRendezVous(Quipu rv, long timeout) throws MessageExchangeException;
    
    void start() throws MessageExchangeException;

    void stop() throws MessageExchangeException;

    Envelope createMessage();

    Cluster getCluster();

    // lose these - clients should use equiv method on Cluster
    // Peer getPeer(Address address); // should be Address.getPeer()
    // should be implemented as getPeer(Address address).getName() - but then we
    // will need to introduce a ClusterPeer ?
    String getPeerName(Address address);

    // needed by BasicReplicaStorage stuff - can we lose it ?
    Address getAddress(String name);
}
