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

public class ByteBufferInputStream extends InputStream {
    
    protected final static Log _log=LogFactory.getLog(ByteBufferInputStream.class);
    
    protected final Takable _inputQueue; // ByteBuffers are pushed onto here by producer, taken off by the consumer
    protected final Puttable _outputQueue; // and then placed onto here...
    
    protected ByteBuffer _buffer=null; // only ever read by consumer
    protected volatile boolean _closed=false; // written by producer, read by consumer
    
    public ByteBufferInputStream(Takable inputQueue, Puttable outputQueue) {
        super();
        _inputQueue=inputQueue;
        _outputQueue=outputQueue;
    }

    // impl
    
    protected void safePut(ByteBuffer buffer, Puttable puttable) {
        do {
            try {
                puttable.put(buffer);
            } catch (InterruptedException e) {
                if (_log.isTraceEnabled()) _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());
    }
    
    protected ByteBuffer safeTake(Takable takable) {
        do {
            try {
                return (ByteBuffer)takable.take();
            } catch (InterruptedException e) {
                if (_log.isTraceEnabled()) _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());
        
        throw new IllegalStateException();
    }
    
    protected boolean ensureBuffer() {
        if (_buffer==null) {
            // we need a fresh buffer...
            if (!_closed) {
                _buffer=safeTake(_inputQueue);
                return true; // there is further input
            } else {
                // producer has closed his end, we will
                // just use up our existing content...
                if (((Channel)_inputQueue).peek()!=null) { // why, oh why, do I have to cast to find out ?
                    _buffer=safeTake(_inputQueue);
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
        
        if (_buffer.position()==_buffer.limit()) {
            ByteBuffer buffer=_buffer;
            _buffer=null;
            buffer.clear();
            safePut(buffer, _outputQueue);
        }
        
        return (int)b&0xFF; // convert byte to unsigned int - otherwise 255==-1 i.e. EOF etc..
    }
    
    // ISSUE - if someone puts a BB on our input then calls close() to indicate that there is no more input coming
    // we find ourselves in a race. If the consumer thread wins, and tries to rollover to the next buffer before close()
    // gets called, it will sleep on the inputQueue...
    
    // SOLUTION - interrupt it, when close is called, it checks closed flag and either aborts or goes round again - messy
    // but probably necessary...
    
    public void close() {
        _closed=true;
    }

    // ByteBufferInputStream
    
    public void read(ByteBuffer buffer, int from, int to) {
        throw new UnsupportedOperationException(); // NYI
    }
    
}
