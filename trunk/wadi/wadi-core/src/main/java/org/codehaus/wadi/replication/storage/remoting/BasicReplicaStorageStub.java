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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.StreamerConfig;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;


/**
 * 
 * @version $Revision$
 */
public class BasicReplicaStorageStub implements ReplicaStorage {
    private static final Log log = LogFactory.getLog(BasicReplicaStorageStub.class);
    
    private final Dispatcher dispatcher;
    private final Destination[] destinations;
    
    public BasicReplicaStorageStub(Dispatcher dispatcher, Destination[] destinations) {
        this.dispatcher = dispatcher;
        this.destinations = destinations;

        dispatcher.register(ReplicaStorageResult.class, 1000);
    }

    public void mergeCreate(Object key, ReplicaInfo replicaInfo) {
        replicaInfo = transformForStorage(replicaInfo);
        ReplicaStorageRequest command = newCreateRequest(key, replicaInfo);
        sendCommand(command);
    }

    public void mergeUpdate(Object key, ReplicaInfo replicaInfo) {
        replicaInfo = transformForStorage(replicaInfo);
        ReplicaStorageRequest command = newUpdateRequest(key, replicaInfo);
        sendCommand(command);
    }

    public void mergeDestroy(Object key) {
        DestroyRequest command = new DestroyRequest(key);
        sendCommand(command);
    }

    public ReplicaInfo retrieveReplicaInfo(Object key) {
        RetrieveReplicaInfoRequest command = new RetrieveReplicaInfoRequest(key);
        ObjectMessage message = sendCommand(command, new MinimumReceivedMessageCallback(1));
        if (null == message) {
            return null;
        }
        
        ReplicaStorageResult result = null;
        try {
            result = (ReplicaStorageResult) message.getObject();
        } catch (JMSException e) {
            log.error(e);
        }
        ReplicaInfo info = (ReplicaInfo) result.getPayload();
        info = transformFromStorage(info);
        return info;
    }

    public NodeInfo getHostingNode() {
        throw new UnsupportedOperationException();
    }

    public boolean storeReplicaInfo(Object key) {
        throw new UnsupportedOperationException();
    }

    protected ReplicaStorageRequest newCreateRequest(Object key, ReplicaInfo replicaInfo) {
        return new CreateRequest(key, replicaInfo);
    }

    protected ReplicaStorageRequest newUpdateRequest(Object key, ReplicaInfo replicaInfo) {
        return new UpdateRequest(key, replicaInfo);
    }

    protected ObjectMessage sendCommand(ReplicaStorageRequest command) {
        return sendCommand(command, null);
    }    
    
    protected ObjectMessage sendCommand(ReplicaStorageRequest command, TwoWayMessageCallback callback) {
        ObjectMessage message = null;
        if (command.isOneWay()) {
            sendOneWay(command);
        } else {
            if (null == callback) {
                callback = new MinimumReceivedMessageCallback(destinations.length);
            }
            message = sendTwoWay(command, callback);
        }
        return message;
    }

    protected ObjectMessage sendTwoWay(ReplicaStorageRequest command, TwoWayMessageCallback callback) {
        ObjectMessage message = null;
        for (int i = 0; i < destinations.length && !callback.testStopSend(); i++) {
            Destination destination = destinations[i];
            try {
                Destination from = dispatcher.getLocalDestination();
                long replyTimeout = command.getTwoWayTimeout();
                message = dispatcher.exchangeSend(from, destination, command, replyTimeout);
                callback.receivedMessage(message);
            } catch (Exception e) {
                log.warn("Error when sending command " + command  + 
                        " to destination " + destination, e);
            }
        }
        return message;
    }

    protected void sendOneWay(ReplicaStorageRequest command) {
        for (int i = 0; i < destinations.length; i++) {
            Destination destination = destinations[i];
            try {
                dispatcher.send(destination, command);
            } catch (Exception e) {
                log.warn("Error when sending command " + command  + 
                        " to destination " + destination, e);
            }
        }
    }
    
    protected ReplicaInfo transformForStorage(ReplicaInfo replicaInfo) {
        Object payload = replicaInfo.getReplica();
        if (null != payload) {
            byte[] serializedPayload;
            try {
                Streamer streamer = new SimpleStreamer();
                ByteArrayOutputStream memOut = new ByteArrayOutputStream();
                ObjectOutput os = streamer.getOutputStream(memOut);
                os.writeObject(payload);
                os.close();
                serializedPayload = memOut.toByteArray();
            } catch (IOException e) {
                throw (AssertionError) new AssertionError("Should not happen.").initCause(e);
            }
            replicaInfo = new ReplicaInfo(replicaInfo, serializedPayload);
        }
        return replicaInfo;
    }
    
    protected ReplicaInfo transformFromStorage(ReplicaInfo replicaInfo) {
        Object payload = replicaInfo.getReplica();
        if (false == payload instanceof byte[]) {
            throw new IllegalStateException("Payload is of type " +
                    payload.getClass() + " and should be of the type " +
                    byte[].class);
        }
        byte[] serializedPayload = (byte[]) payload;
        try {
            Streamer streamer = new SimpleStreamer();
            streamer.init(new StreamerConfig() {
                public ClassLoader getClassLoader() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
            ByteArrayInputStream memIn = new ByteArrayInputStream(serializedPayload);
            ObjectInput is = streamer.getInputStream(memIn);
            payload = is.readObject();
            is.close();
        } catch (IOException e) {
            throw (AssertionError) new AssertionError("Should not happen.").initCause(e);
        } catch (ClassNotFoundException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        }
        replicaInfo = new ReplicaInfo(replicaInfo, payload);
        return replicaInfo;
    }

    public void start() throws Exception {
        throw new UnsupportedOperationException();
    }

    public void stop() throws Exception {
        throw new UnsupportedOperationException();
    }
}