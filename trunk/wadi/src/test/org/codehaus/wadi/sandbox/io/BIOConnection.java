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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.Channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BIOConnection extends AbstractConnection {
    
    protected static final Log _log=LogFactory.getLog(BIOConnection.class);
    
    protected final Socket _socket;
    public BIOConnection(Notifiable notifiable, Socket socket) {
        super(notifiable);
        _socket=socket;
    }
    
    public InputStream getInputStream() throws IOException {return _socket.getInputStream();}
    public OutputStream getOutputStream() throws IOException {return _socket.getOutputStream();}
    public void close() {try{_socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}}
    public Channel getChannel(){return _socket.getChannel();}

}
