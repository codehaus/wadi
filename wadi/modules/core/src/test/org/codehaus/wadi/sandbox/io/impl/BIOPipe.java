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
import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.io.PipeConfig;

public class BIOPipe extends AbstractPipe {
    
    protected static final Log _log=LogFactory.getLog(BIOPipe.class);
    
    protected final Socket _socket;
    
    public BIOPipe(PipeConfig config, long timeout, Socket socket) {
        super(config, timeout);
        _socket=socket;
        try {
            _socket.setSoTimeout((int)_timeout); // TODO - parameterise
        } catch (SocketException e) {
            _log.warn("could not set socket timeout", e);
        }
    }
    
    // Connection
    
    public void run() {
        while (_valid)
            super.run(); // impossible to idle - loop until EOF...
    }
    
    public void close() throws IOException {
        super.close(); // deals with streams...
        try{_socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
        }

    // StreamConnection

    public InputStream getInputStream() throws IOException {return _socket.getInputStream();}
    public OutputStream getOutputStream() throws IOException {return _socket.getOutputStream();}

}
