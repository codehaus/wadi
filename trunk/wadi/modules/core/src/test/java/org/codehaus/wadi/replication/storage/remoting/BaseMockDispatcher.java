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
import java.util.Map;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterListener;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BaseMockDispatcher implements Dispatcher {

    public void init(DispatcherConfig config) throws Exception {
        throw new UnsupportedOperationException();
    }

    public InternalDispatcher register(Object target, String methodName, Class type) {
        throw new UnsupportedOperationException();
    }

    public InternalDispatcher newRegister(Object target, String methodName, Class type) {
        throw new UnsupportedOperationException();
    }

    public boolean deregister(String methodName, Class type, int timeout) {
        throw new UnsupportedOperationException();
    }

    public boolean newDeregister(String methodName, Class type, int timeout) {
        throw new UnsupportedOperationException();
    }

    public void register(Class type, long timeout) {
        throw new UnsupportedOperationException();
    }

    public boolean send(Destination from, Destination to, String outgoingCorrelationId, Serializable body) {
        throw new UnsupportedOperationException();
    }

    public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout) {
        throw new UnsupportedOperationException();
    }

    public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout, String targetCorrelationId) {
        throw new UnsupportedOperationException();
    }

    public ObjectMessage exchangeSendLoop(Destination from, Destination to, Serializable body, long timeout, int iterations) {
        throw new UnsupportedOperationException();
    }

    public ObjectMessage exchangeSend(Destination from, Destination to, String outgoingCorrelationId, Serializable body, long timeout) {
        throw new UnsupportedOperationException();
    }

    public boolean reply(Destination from, Destination to, String incomingCorrelationId, Serializable body) {
        throw new UnsupportedOperationException();
    }

    public boolean reply(ObjectMessage message, Serializable body) {
        throw new UnsupportedOperationException();
    }

    public ObjectMessage exchangeReply(ObjectMessage message, Serializable body, long timeout) {
        throw new UnsupportedOperationException();
    }

    public ObjectMessage exchangeReplyLoop(ObjectMessage message, Serializable body, long timeout) {
        throw new UnsupportedOperationException();
    }

    public boolean forward(ObjectMessage message, Destination destination) {
        throw new UnsupportedOperationException();
    }

    public boolean forward(ObjectMessage message, Destination destination, Serializable body) {
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

    public ObjectMessage attemptRendezVous(String correlationId, Quipu rv, long timeout) {
        throw new UnsupportedOperationException();
    }

    public PooledExecutor getExecutor() {
        throw new UnsupportedOperationException();
    }

    public Destination getLocalDestination() {
        throw new UnsupportedOperationException();
    }

    public Destination getClusterDestination() {
        throw new UnsupportedOperationException();
    }

    public Cluster getCluster() {
        throw new UnsupportedOperationException();
    }

    public Map getDistributedState() {
        throw new UnsupportedOperationException();
    }

    public void setDistributedState(Map state) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void start() throws Exception {
        throw new UnsupportedOperationException();
    }

    public void stop() throws Exception {
        throw new UnsupportedOperationException();
    }

    public String getNodeName(Destination destination) {
        throw new UnsupportedOperationException();
    }

    public String getIncomingCorrelationId(ObjectMessage message) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void setIncomingCorrelationId(ObjectMessage message, String correlationId) throws Exception {
        throw new UnsupportedOperationException();
    }

    public String getOutgoingCorrelationId(ObjectMessage message) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void setOutgoingCorrelationId(ObjectMessage message, String correlationId) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void send(Destination to, ObjectMessage message) throws Exception {
        throw new UnsupportedOperationException();
    }

    public ObjectMessage createObjectMessage() throws Exception {
        throw new UnsupportedOperationException();
    }

    public String getNodeName() {
        throw new UnsupportedOperationException();
    }

    public long getInactiveTime() {
        throw new UnsupportedOperationException();
    }

    public int getNumNodes() {
        throw new UnsupportedOperationException();
    }

    public void setClusterListener(ClusterListener listener) {
        throw new UnsupportedOperationException();
    }

    public Destination getDestination(String name) {
        throw new UnsupportedOperationException();
    }

    public String getLocalNodeName() {
        throw new UnsupportedOperationException();
    }

    public void send(Destination destination, Serializable request) throws Exception {
        throw new UnsupportedOperationException();
    }
}