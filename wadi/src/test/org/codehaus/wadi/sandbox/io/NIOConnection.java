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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Puttable;

public class NIOConnection implements Runnable, Puttable {
    
    protected final static Log _log=LogFactory.getLog(NIOConnection.class);
    
    protected final SocketChannel _channel;
    protected final SelectionKey _key;
    protected final ByteBufferInputStream _input;
    //protected final SocketChannelOutputStream _out;

    public NIOConnection(SocketChannel channel, SelectionKey key, EDU.oswego.cs.dl.util.concurrent.Channel inputQueue, Puttable outputQueue) {
        _channel=channel;
        _key=key;
        _input=new ByteBufferInputStream(inputQueue, outputQueue);
        //_out=new SocketChannelOutputStream(channel, _bufferSize);
        }
    
    public void run() {
        _log.info("running a Connection!");
        int capacity=32;
        byte[] bytesOut=new byte[capacity];
        int bytesRead=0;
        
        try {
            while((bytesRead+=_input.read(bytesOut))<capacity) {
                _log.info(new String(bytesOut));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        _log.info(new String(bytesOut));
        _log.info("finished!");
    }

    public synchronized void close()
    throws IOException
    {
        //_out.close();
        _input.close();
        if (!_channel.isOpen())
            return;
        _key.cancel();
        _channel.socket().shutdownOutput();
        _channel.close();
        _channel.socket().close();
        _channel.close();
    }

    public void put(Object item) throws InterruptedException {
        _input.put(item);
        
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _input.offer(item, msecs);
    }
}