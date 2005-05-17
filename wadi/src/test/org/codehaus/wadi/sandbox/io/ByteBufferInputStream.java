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
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

// N.B. It is unfortunate that EDU.oswego.cs.dl.util.concurrent.Channel and java.nio.channels.Channel are homonyms.
// All mentions of Channel in this file refer to the EDU.oswego.cs.dl.util.concurrent variety.

// two threads will be using this object - a producer (the server) and a consumer (the stream's reader).

public class ByteBufferInputStream extends InputStream implements Puttable {
    
    protected final static Log _log=LogFactory.getLog(ByteBufferInputStream.class);
    protected static final Object _endOfQueue=new Object();
    
    protected final Channel _inputQueue; // ByteBuffers are pushed onto here by producer, taken off by the consumer
    protected final Puttable _outputQueue; // and then placed onto here...
    protected final long _timeout;
    
    protected ByteBuffer _buffer=null; // only ever read by consumer
    
    public ByteBufferInputStream(Channel inputQueue, Puttable outputQueue, long timeout) {
        super();
        _inputQueue=inputQueue;
        _outputQueue=outputQueue;
        _timeout=timeout;
    }

    // impl
    
    protected boolean ensureBuffer() throws IOException {
        if (_buffer!=null)
            return true; // we still have input...
        
        Object tmp=null;
        do {
            try {
                tmp=_inputQueue.poll(_timeout); // we need a fresh buffer...
            } catch (TimeoutException e) {
                _log.error("timed out", e);
                throw new IOException();
            } catch (InterruptedException e) {
                _log.error("interrupted", e);
            }
        } while (Thread.interrupted());
        
        if (tmp==_endOfQueue) {// no more input - our producer has committed his end of the queue...
            Utils.safePut(_endOfQueue, _inputQueue); // leave it there - clumsy
            return false; 
        }
        
        _buffer=(ByteBuffer)tmp;
        return true;
    }

    public void recycle() {
        ByteBuffer buffer=_buffer;
        _buffer=null;
        buffer.clear();
        Utils.safePut(buffer, _outputQueue);  
    }
    
    // InputStream
    
    public int read() throws IOException {
        if (!ensureBuffer())
            return -1;
        
        byte b=_buffer.get();
        
        if (!_buffer.hasRemaining())
            recycle();
        
        //_log.info("reading: "+(char)b);
        
        return (int)b&0xFF; // convert byte to unsigned int - otherwise 255==-1 i.e. EOF etc..
    }
    
    public int read(byte b[], int off, int len) throws IOException {
        int red=0; // read (pres.) and read (perf.) are homographs...
        while (red<len && ensureBuffer()) {
            int tranche=Math.min(len, _buffer.remaining());
            _buffer.get(b, off+red, tranche);
            red+=tranche;
            
            if (!_buffer.hasRemaining())
                recycle();
        }
        //_log.info("read: "+red+" bytes");
        return red==0?-1:red;
    }
    
    // ISSUE - if someone puts a BB on our input then calls close() to indicate that there is no more input coming
    // we find ourselves in a race. If the consumer thread wins, and tries to rollover to the next buffer before close()
    // gets called, it will sleep on the inputQueue...
    
    // SOLUTION - interrupt it, when close is called, it checks closed flag and either aborts or goes round again - messy
    // but probably necessary...
    
    public void commit() {
        Utils.safePut(_endOfQueue, _inputQueue);
    }
    
    // ByteBufferInputStream
    
    public void read(ByteBuffer buffer, int from, int to) {
        throw new UnsupportedOperationException(); // NYI
    }

    // Puttable
    
    public void put(Object item) throws InterruptedException {
        //_log.info("putting buffer on input queue: "+item);
        _inputQueue.put(item);        
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputQueue.offer(item, msecs);
    }
    
}
