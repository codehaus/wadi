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

import javax.jms.BytesMessage;
import javax.jms.JMSException;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;


public class BytesMessageInputStream extends AbstractAsyncInputStream implements Puttable {

    public BytesMessageInputStream(Channel inputQueue, long timeout) {
        super(inputQueue, timeout);
    }

    protected BytesMessage _buffer;
    protected long _remaining;
    
    protected void setBuffer(Object object) {
        _buffer=(BytesMessage)object;
        try {
            _remaining=_buffer==null?0:_buffer.getBodyLength();
        } catch (JMSException e) {
            _log.error("could not ascertain input length", e);
        }
    }
    
    protected Object getBuffer() {
        return _buffer;
    }
    
    protected int readByte() throws IOException {
        try {
            int b=_buffer.readUnsignedByte();
            _remaining--;
            return b;
        } catch (JMSException e) {
            _log.error("could not read next byte", e);
            throw new IOException();
        }
    }
    
    protected void readBytes(byte b[], int off, int len) throws IOException {
        try {
            if (off==0)
                _buffer.readBytes(b, len);
            else {
                // inefficient - but we are not helped by JMS API...
                for (int i=0; i<len; i++)
                    b[off++]=_buffer.readByte();
            }
            _remaining-=len;
        } catch (JMSException e) {
            _log.error("problem reading bytes", e);
            throw new IOException();
        }
    }
    
    protected long getRemaining() {
        return _remaining;
    }
    
    protected void recycle(Object object) {
        // I don't see how we can recycle BytesMessages... ?
    }
    
}
