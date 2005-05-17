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
import java.io.OutputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BytesMessageOutputStream extends OutputStream {

    protected final static Log _log=LogFactory.getLog(BytesMessageOutputStream.class);

    protected final BytesMessageOutputStreamConfig _config;
    protected BytesMessage _buffer;
    
    public BytesMessageOutputStream(BytesMessageOutputStreamConfig config) {
        super();
        _config=config;
        try {
            allocate();
        } catch (IOException e) {
            _log.error(e); // should we let this go further ?
        }
    }

    public void allocate() throws IOException {
        try {
            _buffer=_config.createBytesMessage();
        } catch (JMSException e) {
            _log.error(e);
            throw new IOException();
        }   
    }
    
    public void flush() throws IOException {
        send(_buffer);
        allocate();
    }
    
    public void close() throws IOException {
        send(_buffer);
        allocate();
    }
    
    public void send(BytesMessage message) throws IOException {
        try {
            message.reset(); // switch to read-only mode
            if (message.getBodyLength()>0) {
                _config.send(message);
            }
        } catch (Exception e) {
            _log.error(e);
            throw new IOException("problem sending bytes");
        }
    }

    public void write(int b) throws IOException {
        try {
        _buffer.writeByte((byte)b);
        } catch (JMSException e) {
            _log.error(e);
            throw new IOException();
        }
    }
    
}
