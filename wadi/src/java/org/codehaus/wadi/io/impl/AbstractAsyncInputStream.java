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
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

public abstract class AbstractAsyncInputStream extends InputStream implements Puttable {

    protected static final Object _endOfQueue=new Object();

    protected final Log _log=LogFactory.getLog(getClass());
    protected final Channel _inputQueue;
    protected final long _timeout;

    public AbstractAsyncInputStream(Channel inputQueue, long timeout) {
        super();
        _inputQueue=inputQueue;
        _timeout=timeout;
    }

    protected abstract void setBuffer(Object object);
    protected abstract Object getBuffer();
    protected abstract int readByte() throws IOException;
    protected abstract void readBytes(byte b[], int off, int len) throws IOException;
    protected abstract long getRemaining();
    protected abstract void recycle(Object object);
    
    protected boolean ensureBuffer() throws IOException {
        if (getBuffer()!=null)
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
        
        setBuffer(tmp);
        return true;
    }

    // InputStream
    
    public int read() throws IOException {
        if (!ensureBuffer())
            return -1;
        
        int b=readByte();
        
        if (getRemaining()<=0) {
            Object object=getBuffer();
            setBuffer(null);
            recycle(object);
        }
        
        //_log.info("reading: "+(char)b);
        
        return b;
    }

    // InputStream
    
    public int read(byte b[], int off, int len) throws IOException {
        int red=0; // read (pres.) and read (perf.) are homographs...
        while (red<len && ensureBuffer()) {
            int tranche=Math.min(len, (int)getRemaining());
            readBytes(b, off+red, tranche);
            red+=tranche;
            
            if (getRemaining()<=0) {
                Object object=getBuffer();
                setBuffer(null);
                recycle(object);
            }
        }
        //_log.info("read: "+red+" bytes");
        return red==0?-1:red;
    }

    // InputStream
    
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
