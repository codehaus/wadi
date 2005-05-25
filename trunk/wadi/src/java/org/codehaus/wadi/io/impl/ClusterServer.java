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
package org.codehaus.wadi.io.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.io.Pipe;
import org.codehaus.wadi.io.PipeConfig;
import org.codehaus.wadi.io.ServerConfig;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public class ClusterServer extends AbstractServer implements PipeConfig, MessageListener {

    protected final boolean _excludeSelf;
    protected final Map _pipes;
        
    public ClusterServer(PooledExecutor executor, long pipeTimeout, boolean excludeSelf) {
        super(executor, pipeTimeout);
        _excludeSelf=excludeSelf;
        _pipes=new HashMap();
    }
    
    protected ExtendedCluster _cluster;
    protected MessageConsumer _nodeConsumer;
    protected MessageConsumer _clusterConsumer;
    
    public void init(ServerConfig config) {
        super.init(config);
        _cluster=_config.getCluster();
    }

    public void start() throws Exception {
        super.start();
        _clusterConsumer=_cluster.createConsumer(_cluster.getDestination(), null, _excludeSelf);
        _clusterConsumer.setMessageListener(this);
        _nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, _excludeSelf);
        _nodeConsumer.setMessageListener(this);
    }

    public void stop() throws Exception {
        stopAcceptingPipes();
        waitForExistingPipes();
        super.stop();
    }
    
    public void stopAcceptingPipes() {
        try {
        _clusterConsumer.setMessageListener(null);
        _nodeConsumer.setMessageListener(null);
        } catch (JMSException e) {
            _log.warn("could not remove Listeners", e);
        }
    }
    
    public void onMessage(Message message) {
        try {
            if (message instanceof BytesMessage) {
                BytesMessage bm=(BytesMessage)message;
                String correlationId=bm.getJMSCorrelationID();
                Destination replyTo=bm.getJMSReplyTo();
                //_log.info("receiving message");
                synchronized (_pipes) {
                    ClusterPipe pipe=(ClusterPipe)_pipes.get(correlationId);
                    if (pipe==null) {
                        // initialising a new Pipe...
                        String name=correlationId.substring(0, correlationId.length()-7);
                        Destination us=_cluster.getLocalNode().getDestination();
                        pipe=new ClusterPipe(this, _pipeTimeout, _cluster, us, replyTo, name, new LinkedQueue(), true);
                        //_log.info("created Pipe: '"+pipe.getCorrelationId()+"'");
                        _pipes.put(pipe.getCorrelationId(), pipe);
                        run(pipe);
                    }
                    // servicing existing pipe...
                    if (bm.getBodyLength()>0) {
                        //_log.info("servicing Pipe: '"+correlationId+"' - "+bm.getBodyLength()+" bytes");
                        Utils.safePut(bm, pipe);
                    }
                    if (bm.getBooleanProperty("closing-stream")) {
                        //_log.info("SERVER CLOSING STREAM: "+pipe);
//                        pipe.commit();
                    }
                }
            }
        } catch (JMSException e) {
            _log.error("unexpected problem", e);
        }
    }
    
    // needs more thought...
    
    public Pipe makeClientPipe(String correlationId, Destination target) {
        Destination source=_cluster.getLocalNode().getDestination();
        LinkedQueue queue=new LinkedQueue();
        ClusterPipe pipe=new ClusterPipe(this, _pipeTimeout, _cluster, source, target, correlationId, queue, false);
        _pipes.put(pipe.getCorrelationId(), pipe);
        return pipe;
    }
    
    public void waitForExistingPipes() {
        int numPipes;
        while ((numPipes=_pipes.size())>0) {
            _log.info("waiting for: "+numPipes+" Pipe[s]");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                _log.trace("unexpected interruption - ignoring", e);
            }
        }
        _log.info("existing Pipes have finished running");
    }
    
    // PipeConfig
    
    public void notifyClosed(Pipe pipe) {
        _pipes.remove(((ClusterPipe)pipe)._ourCorrelationId); // TODO - encapsulate properly
    }

    public void notifyIdle(Pipe pipe) {
        // Cluster Connections idle automatically after running...
        _pipes.remove(((ClusterPipe)pipe)._ourCorrelationId); // TODO - encapsulate properly
    }

}
