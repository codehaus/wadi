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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

public class NIOServerConnection extends AbstractServerConnection implements Puttable {
    
    protected final static Log _log=LogFactory.getLog(NIOServerConnection.class);
    
    protected final SocketChannel _channel;
    protected final SelectionKey _key;
    protected final Channel _inputQueue;
    protected final Puttable _outputQueue;
 
    public NIOServerConnection(NIOConnectionConfig config, long timeout, SocketChannel channel, SelectionKey key, Channel inputQueue, Puttable outputQueue, int bufferSize) {
        super(config, timeout);
        _channel=channel;
        _key=key;
        _inputQueue=inputQueue;
        _outputQueue=outputQueue;

        // ctor is called by Server thread - find some way to do these allocations on Consumer thread...
        _inputStream=new ByteBufferInputStream(_inputQueue, _outputQueue, _timeout);
        _outputStream=new ByteBufferOutputStream(_channel, bufferSize);
        }

    protected final SynchronizedBoolean _running=new SynchronizedBoolean(false); 
    public boolean getRunning() {return _running.get();}
    public void setRunning(boolean running) {_running.set(running);}
    
    protected ByteBufferInputStream _inputStream;
    public InputStream getInputStream() throws IOException {return _inputStream;}

    protected OutputStream _outputStream;
    public OutputStream getOutputStream() throws IOException {return _outputStream;}
    
    public void close() throws IOException {
        super.close();
        Sync lock=((NIOConnectionConfig)_config).getLock();
        do {
            try {
                lock.acquire(); // sync following actions with Server loop...
            } catch (InterruptedException e) {
                // ignore
            }
        } while (Thread.interrupted());
        
        try {
            //_log.info("cancelling: "+_key);
            _channel.socket().shutdownOutput();
            _channel.socket().close();
            _channel.close();
        } finally {
            lock.release();
        }
    }

    // called by server...
    public synchronized void commit() throws IOException {
        _inputStream.commit();
        _channel.socket().shutdownInput();
        _key.cancel();
    }

    // Puttable - ByteBuffers only please :-)
    
    public void put(Object item) throws InterruptedException {
        _inputStream.put(item);
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputStream.offer(item, msecs);
    }
}