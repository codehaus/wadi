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

public class ByteArrayInputStream extends InputStream implements Puttable {

    protected static final Log _log=LogFactory.getLog(ByteArrayInputStream.class);
    protected static final byte[] _endOfQueue=new byte[0];

    protected final Channel _inputQueue;
    
    public ByteArrayInputStream(Channel inputQueue) {
        _inputQueue=inputQueue;
    }

    protected byte[] _buffer;
    protected int _position;
    protected boolean _committed;
    
    // impl
    
    protected boolean ensureBuffer() {
        if (_buffer==null) {
            // we need a fresh buffer...
            if (!_committed) {
                byte[] buffer=(byte[])Utils.safeTake(_inputQueue);
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
                    _buffer=(byte[])Utils.safeTake(_inputQueue);
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
        
        byte b=_buffer[_position++];
        
        if (_position>=_buffer.length) {
            byte[] buffer=_buffer;
            _buffer=null;
            _position=0;
            // could reuse buffer here... - TODO
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
    
    public synchronized void put(Object item) throws InterruptedException {
        _inputQueue.put(item);
    }

    public synchronized boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputQueue.offer(item, msecs);
    }

}
