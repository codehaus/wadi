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
package org.codehaus.wadi.sandbox.io.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.codehaus.wadi.gridstate.ExtendedCluster;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.sandbox.io.Pipe;
import org.codehaus.wadi.sandbox.io.PipeConfig;
import org.codehaus.wadi.sandbox.io.ServerConfig;

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
                String ourId=bm.getJMSCorrelationID();
                Destination replyTo=bm.getJMSReplyTo();
                //_log.info("receiving message");
                synchronized (_pipes) {
                    //_log.info("looking up Pipe: "+ourId);
                    AbstractClusterPipe pipe=(AbstractClusterPipe)_pipes.get(ourId);
                    if (pipe==null) {
                        // initialising a new Pipe...
                        String theirId=ourId.substring(0, ourId.indexOf("-server"))+"-client";
                        Destination us=_cluster.getLocalNode().getDestination();
                        pipe=new ServerClusterPipe(this, _pipeTimeout, _cluster, us, ourId, replyTo, theirId, new LinkedQueue());
                        ourId=pipe.getCorrelationId();
                        //_log.info("adding Pipe: '"+ourId+"'");
                        synchronized (_pipes) {
                            _pipes.put(ourId, pipe);
                        }
                        run(pipe);
                    } else {
                        //_log.info("found Pipe: '"+ourId+"'");
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
            if ( _log.isErrorEnabled() ) {

                _log.error("unexpected problem", e);
            }
        }
    }
    
    // needs more thought...
    
    public Pipe makeClientPipe(String correlationId, Destination target) {
        Destination source=_cluster.getLocalNode().getDestination();
        LinkedQueue queue=new LinkedQueue();
        AbstractClusterPipe pipe=new ClientClusterPipe(this, _pipeTimeout, _cluster, source, target, correlationId, queue);
        String id=pipe.getCorrelationId();
        //_log.info("adding Pipe: "+id);
        synchronized (_pipes) {
            _pipes.put(id, pipe);
        }
        return pipe;
    }
    
    protected int getNumPipes() {
        synchronized (_pipes) {
            return _pipes.size();
        }
    }
    
    public void waitForExistingPipes() {
        int numPipes;
        while ((numPipes=getNumPipes())>0) {
            if ( _log.isInfoEnabled() ) {

                _log.info("waiting for: " + numPipes + " Pipe[s]");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if ( _log.isTraceEnabled() ) {

                    _log.trace("unexpected interruption - ignoring", e);
                }
            }
        }
        if ( _log.isInfoEnabled() ) {

            _log.info("existing Pipes have finished running");
        }
    }
    
    // PipeConfig
    
    public void notifyClosed(Pipe pipe) {
        String correlationId=((AbstractClusterPipe)pipe)._ourCorrelationId;
        //_log.info("removing Pipe: "+correlationId);
        synchronized (_pipes) {
            _pipes.remove(correlationId); // TODO - encapsulate properly
        }
    }

    public void notifyIdle(Pipe pipe) {
        // Cluster Connections idle automatically after running...
        //super.notifyIdle(pipe);
        notifyClosed(pipe);
    }
    
}
