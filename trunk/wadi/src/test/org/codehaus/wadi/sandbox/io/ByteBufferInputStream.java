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
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;
import EDU.oswego.cs.dl.util.concurrent.Takable;

// N.B. It is unfortunate that EDU.oswego.cs.dl.util.concurrent.Channel and java.nio.channels.Channel are homonyms.
// All mentions of Channel in this file refer to the EDU.oswego.cs.dl.util.concurrent variety.

// two threads will be using this object - a producer (the server) and a consumer (the stream's reader).

public class ByteBufferInputStream extends InputStream implements Puttable {
    
    protected final static Log _log=LogFactory.getLog(ByteBufferInputStream.class);
    protected static final ByteBuffer _endOfQueue=ByteBuffer.allocateDirect(0);
    
    protected final Channel _inputQueue; // ByteBuffers are pushed onto here by producer, taken off by the consumer
    protected final Puttable _outputQueue; // and then placed onto here...
    
    protected ByteBuffer _buffer=null; // only ever read by consumer
    protected volatile boolean _committed=false; // written by producer, read by consumer
    
    public ByteBufferInputStream(Channel inputQueue, Puttable outputQueue) {
        super();
        _inputQueue=inputQueue;
        _outputQueue=outputQueue;
    }

    // impl
    
    protected boolean ensureBuffer() {
        if (_buffer==null) {
            // we need a fresh buffer...
            if (!_committed) {
                ByteBuffer buffer=Utils.safeTake(_inputQueue);
                if (buffer==_endOfQueue)
                    return false; // there is no further input - our producer has committed his end of the queue...
                else {
                    _buffer=buffer;
                    return true; // there is further input
                }
            } else {
                // producer has closed his end, we will
                // just use up our existing content...
                if (_inputQueue.peek()!=null) {
                    _buffer=Utils.safeTake(_inputQueue);
                    return true; // there is further input
                } else {
                    // no buffers left
                    return false; // there is no further input...
                }
            }
        } else {
            // we don't need to rollover to the next buffer yet
            return true; // there is further input
        }
    }

    // InputStream
    
    public int read() throws IOException {
        if (!ensureBuffer())
            return -1;
        
        byte b=_buffer.get();
        
        if (!_buffer.hasRemaining()) {
            ByteBuffer buffer=_buffer;
            _buffer=null;
            buffer.clear();
            Utils.safePut(buffer, _outputQueue);
        }
        
        //_log.info("reading: "+(char)b);

        return (int)b&0xFF; // convert byte to unsigned int - otherwise 255==-1 i.e. EOF etc..
    }
    
    // ISSUE - if someone puts a BB on our input then calls close() to indicate that there is no more input coming
    // we find ourselves in a race. If the consumer thread wins, and tries to rollover to the next buffer before close()
    // gets called, it will sleep on the inputQueue...
    
    // SOLUTION - interrupt it, when close is called, it checks closed flag and either aborts or goes round again - messy
    // but probably necessary...
    
    public void commit() {
        _committed=true;
        Utils.safePut(_endOfQueue, _inputQueue);
    }
    
    // ByteBufferInputStream
    
    public void read(ByteBuffer buffer, int from, int to) {
        throw new UnsupportedOperationException(); // NYI
    }

    public void put(Object item) throws InterruptedException {
        _log.info("putting buffer on input queue: "+item);
        _inputQueue.put(item);        
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputQueue.offer(item, msecs);
    }
    
}
