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
package org.codehaus.wadi.sandbox.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.sandbox.io.Pipe;
import org.codehaus.wadi.sandbox.io.PipeConfig;

public class SocketClientPipe extends AbstractPipe {

    public static class DummyPipeConfig implements PipeConfig {
        public void notifyIdle(Pipe pipe) {/* do nothing */}
        public void notifyClosed(Pipe pipe) {/* do nothing */}
        public Contextualiser getContextualiser() {return null;}        
        public String getNodeId() {return null;}
    }
    
    
    protected final SocketChannel _channel;
    protected final Socket _socket;
    
    public SocketClientPipe(InetSocketAddress address, long timeout) throws IOException {
        super(new DummyPipeConfig(), timeout);
        _channel=SocketChannel.open(address);
        _channel.configureBlocking(true);
        _socket=_channel.socket();
        _socket.setKeepAlive(true);
        _socket.setSoTimeout((int)_timeout);
    }
   
    
    // Pipe
    
    public void close() throws IOException {
        super.close(); // deals with streams...
        try{_socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
        try{_channel.close();}catch(Exception e){_log.warn("problem closing socket",e);}
        }

    // StreamPipe

    public InputStream getInputStream() throws IOException {return _socket.getInputStream();}
    public OutputStream getOutputStream() throws IOException {return _socket.getOutputStream();}

    // WritableByteChannel - supported

    public int write(ByteBuffer src) throws IOException {
        return _channel.write(src);
    }

    public boolean isOpen() {
        return _channel.isOpen();
    }
}
