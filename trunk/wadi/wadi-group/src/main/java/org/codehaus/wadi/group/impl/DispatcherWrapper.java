/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.group.ServiceEndpoint;

public class DispatcherWrapper implements Dispatcher {

    protected Dispatcher _delegate;

    public DispatcherWrapper(Dispatcher delegate) throws MessageExchangeException {
        _delegate=delegate;
        _delegate.start();
    }

    public Collection attemptMultiRendezVous(String correlationId, Quipu rv, long timeout) throws MessageExchangeException {
        return _delegate.attemptMultiRendezVous(correlationId, rv, timeout);
    }

    public Message attemptRendezVous(String correlationId, Quipu rv, long timeout) throws MessageExchangeException {
        return _delegate.attemptRendezVous(correlationId, rv, timeout);
    }

    public Message createMessage() {
        return _delegate.createMessage();
    }

    public Message exchangeSend(Address target, Serializable pojo, long timeout) throws MessageExchangeException {
        return _delegate.exchangeSend(target, pojo, timeout);
    }

    public Message exchangeSend(Address target, Serializable body, long timeout, String targetCorrelationId) throws MessageExchangeException {
        return _delegate.exchangeSend(target, body, timeout, targetCorrelationId);
    }

    public Message exchangeSend(Address target, String sourceCorrelationId, Serializable pojo, long timeout) throws MessageExchangeException {
        return _delegate.exchangeSend(target, sourceCorrelationId, pojo, timeout);
    }

    public void forward(Message message, Address destination) throws MessageExchangeException {
        _delegate.forward(message, destination);
    }

    public void forward(Message message, Address destination, Serializable body) throws MessageExchangeException {
        _delegate.forward(message, destination, body);
    }

    public Address getAddress(String name) {
        return _delegate.getAddress(name);
    }

    public Cluster getCluster() {
        return _delegate.getCluster();
    }

    public String getPeerName(Address address) {
        return _delegate.getPeerName(address);
    }

    public Map getRendezVousMap() {
        return _delegate.getRendezVousMap();
    }

    public void init(DispatcherConfig config) throws Exception {
        _delegate.init(config);
    }

    public String nextCorrelationId() {
        return _delegate.nextCorrelationId();
    }

    public void register(ServiceEndpoint internalDispatcher) {
        _delegate.register(internalDispatcher);
    }

    public void reply(Address from, Address to, String sourceCorrelationId, Serializable body) throws MessageExchangeException {
        _delegate.reply(from, to, sourceCorrelationId, body);
    }

    public void reply(Message message, Serializable body) throws MessageExchangeException {
        _delegate.reply(message, body);
    }

    public void send(Address target, Message message) throws MessageExchangeException {
        _delegate.send(target, message);
    }

    public void send(Address target, Serializable pojo) throws MessageExchangeException {
        _delegate.send(target, pojo);
    }

    public void send(Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException {
        _delegate.send(target, sourceCorrelationId, pojo);
    }

    public void send(Address source, Address target, String sourceCorrelationId, Serializable pojo) throws MessageExchangeException {
        _delegate.send(source, target, sourceCorrelationId, pojo);
    }

    public Quipu setRendezVous(String correlationId, int numLlamas) {
        return _delegate.setRendezVous(correlationId, numLlamas);
    }

    public void start() throws MessageExchangeException {
        // already started
    }

    public void stop() throws MessageExchangeException {
        _delegate.stop();
    }

    public void unregister(ServiceEndpoint internalDispatcher, int nbAttemp, long delayMillis) {
        _delegate.unregister(internalDispatcher, nbAttemp, delayMillis);
    }

    public void onMessage(Message message) {
        _delegate.onMessage(message);
    }

}
