/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.replication.storage.remoting;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Quipu;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BaseMockDispatcher implements Dispatcher {

    public void init(DispatcherConfig config) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void send(Address source, Address target, String sourceCorrelationId, Serializable pojo) {
        throw new UnsupportedOperationException();
    }

    public void send(Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException {
        throw new UnsupportedOperationException();
    }
    
    public Message exchangeSend(Address to, Serializable body, long timeout) {
        throw new UnsupportedOperationException();
    }

    public Message exchangeSend(Address target, Serializable body, long timeout, String targetCorrelationId) {
        throw new UnsupportedOperationException();
    }

    public Message exchangeSend(Address target, String targetCorrelationId, Serializable pojo, long timeout) {
        throw new UnsupportedOperationException();
    }

    public void reply(Address from, Address to, String incomingCorrelationId, Serializable body) {
        throw new UnsupportedOperationException();
    }

    public void reply(Message message, Serializable body) {
        throw new UnsupportedOperationException();
    }

    public void forward(Message message, Address destination) {
        throw new UnsupportedOperationException();
    }

    public void forward(Message message, Address destination, Serializable body) {
        throw new UnsupportedOperationException();
    }

    public Map getRendezVousMap() {
        throw new UnsupportedOperationException();
    }

    public String nextCorrelationId() {
        throw new UnsupportedOperationException();
    }

    public Quipu setRendezVous(String correlationId, int numLlamas) {
        throw new UnsupportedOperationException();
    }

    public Message attemptRendezVous(String correlationId, Quipu rv, long timeout) {
        throw new UnsupportedOperationException();
    }

    public Collection attemptMultiRendezVous(String correlationId, Quipu rv, long timeout) throws MessageExchangeException {
        throw new UnsupportedOperationException();
    }
    
    public Cluster getCluster() {
        throw new UnsupportedOperationException();
    }

    public void setDistributedState(Map state) throws MessageExchangeException {
        throw new UnsupportedOperationException();
    }

    public void start() throws MessageExchangeException {
        throw new UnsupportedOperationException();
    }

    public void stop() throws MessageExchangeException {
        throw new UnsupportedOperationException();
    }

    public String getPeerName(Address address) {
        throw new UnsupportedOperationException();
    }

    public String getIncomingCorrelationId(Message message) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void setIncomingCorrelationId(Message message, String correlationId) throws Exception {
        throw new UnsupportedOperationException();
    }

    public String getOutgoingCorrelationId(Message message) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void setOutgoingCorrelationId(Message message, String correlationId) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void send(Address target, Message message) throws MessageExchangeException {
        throw new UnsupportedOperationException();
    }

    public Message createMessage() {
        throw new UnsupportedOperationException();
    }

    public void setClusterListener(ClusterListener listener) {
        throw new UnsupportedOperationException();
    }

    public Address getAddress(String name) {
        throw new UnsupportedOperationException();
    }

    public void send(Address destination, Serializable request) throws MessageExchangeException {
        throw new UnsupportedOperationException();
    }

    public void onMessage(Message arg0) {
        throw new UnsupportedOperationException();
    }

    public void register(ServiceEndpoint internalDispatcher) {
        throw new UnsupportedOperationException();
    }

    public void unregister(ServiceEndpoint internalDispatcher, int nbAttemp, long delayMillis) {
        throw new UnsupportedOperationException();
    }
}