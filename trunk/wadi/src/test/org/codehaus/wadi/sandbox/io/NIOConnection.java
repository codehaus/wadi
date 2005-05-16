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

public class NIOConnection extends AbstractConnection implements Puttable {
    
    protected final static Log _log=LogFactory.getLog(NIOConnection.class);
    
    protected final SocketChannel _channel;
    protected final SelectionKey _key;
    protected final Channel _inputQueue;
    protected final Puttable _outputQueue;
 
    public NIOConnection(Notifiable notifiable, SocketChannel channel, SelectionKey key, Channel inputQueue, Puttable outputQueue) {
        super(notifiable);
        _channel=channel;
        _key=key;
        _inputQueue=inputQueue;
        _outputQueue=outputQueue;

        // ctor is called by Server thread - find some way to do these allocations on Consumer thread...
        _inputStream=new ByteBufferInputStream(_inputQueue, _outputQueue);
        _outputStream=new ByteBufferOutputStream(_channel);
        }

    protected ByteBufferInputStream _inputStream;
    protected OutputStream _outputStream;
    
//    public void run() {
//        _log.info("running a Connection!");
//        int capacity=4096;
//        byte[] bytesOut=new byte[capacity];
//
//        int n=0;
//        try {
//            while((n=_inputStream.read(bytesOut))!=-1) {
//                _log.info(new String(bytesOut));
//            }
//            if (n==-1) { // last read
//                _log.info(new String(bytesOut));
//            }
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        _log.info("finished!");
//    }

    public InputStream getInputStream() throws IOException {return _inputStream;}
    public OutputStream getOutputStream() throws IOException {return _outputStream;}
    public java.nio.channels.Channel getChannel() {return null;}
 
    public synchronized void commit() throws IOException {
        //_out.commit();
        _inputStream.commit();
        if (!_channel.isOpen())
            return;
        _key.cancel();
        _channel.socket().shutdownOutput();
        _channel.close();
        _channel.socket().close();
        _channel.close();
    }

    // Puttable - ByteBuffers only please :-)
    
    public void put(Object item) throws InterruptedException {
        _inputStream.put(item);
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputStream.offer(item, msecs);
    }
}