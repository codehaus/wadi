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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;

import org.activecluster.Cluster;
import org.codehaus.wadi.io.BytesMessageOutputStreamConfig;
import org.codehaus.wadi.io.PipeConfig;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;

public class ClusterPipe extends AbstractPipe implements Puttable, BytesMessageOutputStreamConfig {

    protected final Cluster _cluster;
    protected final Destination _us;
    protected final Destination _them;
    protected final String _correlationId;
    protected final boolean _isServer;
    protected final Channel _inputQueue;
    protected final BytesMessageInputStream _inputStream;
    protected final BytesMessageOutputStream _outputStream;
    protected final String _ourCorrelationId;
    protected final String _theirCorrelationId;
    
    public ClusterPipe(PipeConfig config, long timeout, Cluster cluster, Destination us, Destination them, String correlationId, Channel inputQueue, boolean isServer) {
        super(config, timeout);
        _cluster=cluster;
        _us=us;
        _them=them;
        _correlationId=correlationId;
        _inputQueue=inputQueue;
        _inputStream=new BytesMessageInputStream(inputQueue, _timeout);
        _outputStream=new BytesMessageOutputStream(this);
        _isServer=isServer;
        _ourCorrelationId=correlationId+(isServer?".server":".client");
        _theirCorrelationId=correlationId+(isServer?".client":".server");
    }

    String getCorrelationId() {
        return _ourCorrelationId;
    }
    
    // Connection
    
    public void close() throws IOException {
        _inputStream.commit();
        super.close();
    }

    protected boolean _running=false;
    
    public synchronized void run() {
        _running=true;
        super.run();
        _running=false;
    }
    
    // StreamConnection
    
    public InputStream getInputStream() {
        return _inputStream;
    }

    public OutputStream getOutputStream() {
        return _outputStream;
    }

    // Puttable - byte[] only please :-)
    
    public void put(Object item) throws InterruptedException {
        _inputStream.put(item);
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputStream.offer(item, msecs);
    }
    
    // ByteArrayOutputStreamConfig
    
    public void send(BytesMessage bytesMessage) throws JMSException {
        bytesMessage.setJMSCorrelationID(_theirCorrelationId);
        bytesMessage.setJMSReplyTo(_us);
        //_log.info("sending message: "+(_isServer?"[server->client]":"[client->server]"));
        _cluster.send(_them, bytesMessage);
    }
    
    public BytesMessage createBytesMessage() throws JMSException {
        return _cluster.createBytesMessage();
    }

    // called by server...
    public synchronized void commit() {
        _inputStream.commit();
        _valid=false;
        
        if (!_running)
            _config.notifyClosed(this);
    }

}
