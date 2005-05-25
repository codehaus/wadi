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

import java.nio.ByteBuffer;

import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;


// N.B. It is unfortunate that EDU.oswego.cs.dl.util.concurrent.Channel and java.nio.channels.Channel are homonyms.
// All mentions of Channel in this file refer to the EDU.oswego.cs.dl.util.concurrent variety.

// two threads will be using this object - a producer (the server) and a consumer (the stream's reader).

public class ByteBufferInputStream extends AbstractAsyncInputStream implements Puttable {
    
    protected final Puttable _outputQueue; // and then placed onto here...
    
    public ByteBufferInputStream(Channel inputQueue, Puttable outputQueue, long timeout) {
        super(inputQueue, timeout);
        _outputQueue=outputQueue;
    }

    protected ByteBuffer _buffer=null; // only ever read by consumer

    protected void setBuffer(Object object) {
        _buffer=(ByteBuffer)object;
    }
    
    protected Object getBuffer() {
        return _buffer;
    }
    
    protected int readByte() {
        return (int)_buffer.get()&0xFF; // convert byte to unsigned int - otherwise 255==-1 i.e. EOF etc..
    }
    
    protected void readBytes(byte b[], int off, int len) {
        _buffer.get(b, off, len);
    }
    
    protected long getRemaining() {
        return _buffer.remaining();
    }
    
    public void recycle(Object object) {
        ((ByteBuffer)object).clear();
        Utils.safePut(object, _outputQueue);  
    }
    
}
