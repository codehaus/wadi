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

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherContext;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.EnvelopeInterceptor;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.group.ServiceEndpoint;

public class SharedDispatcher implements Dispatcher {

    protected Dispatcher _delegate;

    public SharedDispatcher(Dispatcher delegate) throws MessageExchangeException {
        _delegate=delegate;
        _delegate.start();
    }

    public DispatcherContext getContext() {
        return _delegate.getContext();
    }
    
    public void addInterceptor(EnvelopeInterceptor interceptor) {
        _delegate.addInterceptor(interceptor);
    }
    
    public void removeInterceptor(EnvelopeInterceptor interceptor) {
        _delegate.removeInterceptor(interceptor);
    }
    
    public Collection attemptMultiRendezVous(Quipu rv, long timeout) throws MessageExchangeException {
        return _delegate.attemptMultiRendezVous(rv, timeout);
    }

    public Envelope attemptRendezVous(Quipu rv, long timeout) throws MessageExchangeException {
        return _delegate.attemptRendezVous(rv, timeout);
    }

    public Envelope createEnvelope() {
        return _delegate.createEnvelope();
    }

    public Envelope exchangeSend(Address target, Serializable pojo, long timeout) throws MessageExchangeException {
        return _delegate.exchangeSend(target, pojo, timeout);
    }

    public Envelope exchangeSend(Address target, Serializable pojo, long timeout, String targetCorrelationId) throws MessageExchangeException {
        return _delegate.exchangeSend(target, pojo, timeout, targetCorrelationId);
    }

    public Envelope exchangeSend(Address target, String sourceCorrelationId, Serializable pojo, long timeout) throws MessageExchangeException {
        return _delegate.exchangeSend(target, sourceCorrelationId, pojo, timeout);
    }

    public Cluster getCluster() {
        return _delegate.getCluster();
    }

    public String getPeerName(Address address) {
        return _delegate.getPeerName(address);
    }

    public void register(ServiceEndpoint internalDispatcher) {
        _delegate.register(internalDispatcher);
    }

    public void reply(Address from, Address to, String sourceCorrelationId, Serializable body) throws MessageExchangeException {
        _delegate.reply(from, to, sourceCorrelationId, body);
    }

    public void reply(Envelope envelope, Serializable body) throws MessageExchangeException {
        _delegate.reply(envelope, body);
    }

    public void send(Address target, Envelope envelope) throws MessageExchangeException {
        _delegate.send(target, envelope);
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

    public void start() throws MessageExchangeException {
        // already started
    }

    public void stop() throws MessageExchangeException {
        _delegate.stop();
    }

    public void unregister(ServiceEndpoint internalDispatcher, int nbAttemp, long delayMillis) {
        _delegate.unregister(internalDispatcher, nbAttemp, delayMillis);
    }

    public void onEnvelope(Envelope message) {
        _delegate.onEnvelope(message);
    }

    public Envelope exchangeSend(Address target, Envelope envelope, long timeout, String targetCorrelationId) throws MessageExchangeException {
        return _delegate.exchangeSend(target, envelope, timeout, targetCorrelationId);
    }

    public Envelope exchangeSend(Address target, Envelope envelope, long timeout) throws MessageExchangeException {
        return _delegate.exchangeSend(target, envelope, timeout);
    }

    public void reply(Envelope request, Envelope reply) throws MessageExchangeException {
        _delegate.reply(request, reply);
    }

    public void addRendezVousEnvelope(Envelope envelope) {
        _delegate.addRendezVousEnvelope(envelope);
    }
    
    public Quipu newRendezVous(int numLlamas) {
        return _delegate.newRendezVous(numLlamas);
    }

}
