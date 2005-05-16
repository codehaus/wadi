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

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;

public class ClusterConnection extends AbstractConnection implements Puttable {

    protected final String _correlationId;
    protected final Channel _inputQueue;
    protected final ByteArrayInputStream _inputStream;
    protected final ByteArrayOutputStream _outputStream;
    
    public ClusterConnection(Notifiable notifiable, String correlationId, Channel inputQueue) {
        super(notifiable);
        _inputQueue=inputQueue;
        _correlationId=correlationId;
        _inputStream=new ByteArrayInputStream(inputQueue);
        _outputStream=new ByteArrayOutputStream();
    }

    public InputStream getInputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public OutputStream getOutputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public void close() {
        // TODO Auto-generated method stub

    }

    public java.nio.channels.Channel getChannel() {
        throw new UnsupportedOperationException(); // this API should just support r/w of ByteBuffer///
    }

    // Puttable - ByteBuffers only please :-)
    
    public void put(Object item) throws InterruptedException {
        _inputStream.put(item);
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputStream.offer(item, msecs);
    }
}
