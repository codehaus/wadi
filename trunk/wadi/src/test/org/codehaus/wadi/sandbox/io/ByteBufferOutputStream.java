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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ByteBufferOutputStream extends OutputStream {

    protected final static Log _log=LogFactory.getLog(ByteBufferOutputStream.class);
    
    protected final SocketChannel _channel;
    protected final ByteBuffer _buffer;
    
    public ByteBufferOutputStream(SocketChannel channel, int bufferSize) {
        super();
        _channel=channel;
        _buffer=ByteBuffer.allocateDirect(bufferSize);
    }

    // impl
    
    protected void send() throws IOException {
        _buffer.flip();
        while (_buffer.hasRemaining())
            _channel.write(_buffer);
        _buffer.clear();
    }
    
    // OutputStream
    
    public void write(int b) throws IOException {
        //_log.info("writing: "+(char)b);
        _buffer.put((byte)b);
        if (!_buffer.hasRemaining())
            send();
    }
    
    public void write(byte b[], int off, int len) throws IOException {
        //_log.info("writing: "+len+" bytes");
        int written=0;
        while (written<len) {
            int tranche=Math.min(_buffer.remaining(), len);
            _buffer.put(b, off+written, tranche);
            written+=tranche;
            
            if (!_buffer.hasRemaining())
                send();
        }
    }
    
    public void flush() throws IOException {
        super.flush();
        send();
    }
    
    public void close() throws IOException {
        super.close();
        send();
    }
    
    // ByteBufferOutputStream
    
    public void write(ByteBuffer buffer, int offset, int length) throws IOException {
        _channel.write(null, offset, length);
    }
}
