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
package org.codehaus.wadi.sandbox.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;

import org.activecluster.Cluster;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;

public class ClusterConnection extends AbstractServerConnection implements Puttable, BytesMessageOutputStreamConfig {

    protected final Cluster _cluster;
    protected final Destination _us;
    protected final Destination _them;
    protected final String _correlationId;
    protected final Channel _inputQueue;
    protected final BytesMessageInputStream _inputStream;
    protected final BytesMessageOutputStream _outputStream;
    
    public ClusterConnection(ConnectionConfig config, Cluster cluster, Destination us, Destination them, String correlationId, Channel inputQueue, long timeout) {
        super(config);
        _cluster=cluster;
        _us=us;
        _them=them;
        _correlationId=correlationId;
        _inputQueue=inputQueue;
        _inputStream=new BytesMessageInputStream(inputQueue, timeout);
        _outputStream=new BytesMessageOutputStream(this);
    }

    // Connection
    
    public void close() throws IOException {
        _inputStream.commit();
        super.close();
    }

    // StreamConnection
    
    public InputStream getInputStream() throws IOException {
        return _inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
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
        bytesMessage.setJMSCorrelationID(_correlationId);
        bytesMessage.setJMSReplyTo(_us);
        //bytesMessage.setIntProperty("content-length", bytesMessage.getBodyLength());
        _cluster.send(_them, bytesMessage);
    }
    
    public BytesMessage createBytesMessage() throws JMSException {
        return _cluster.createBytesMessage();
    }
}
