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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class Peer implements Runnable, Serializable {
    
    protected static final Log _log=LogFactory.getLog(Peer.class);

    protected transient Socket _socket;
    protected transient ObjectInputStream _ois;
    protected transient ObjectOutputStream _oos;

    public Peer(InetSocketAddress address) throws IOException {
        this(new Socket(address.getAddress(), address.getPort()));
    }
    
    public Peer(Socket socket) throws IOException {
        _socket=socket;
        _ois=new ObjectInputStream(_socket.getInputStream());
        _oos=new ObjectOutputStream(_socket.getOutputStream());
    }
    
    Peer() {
        // used for deserialisation
    }
    
    public void run() {
        try {
            process(_socket, _ois, _oos);
        } finally {
            try{_ois.close();}catch(IOException e){_log.warn("problem closing socket input", e);}
            _ois=null;
            try{_oos.flush();}catch(IOException e){_log.warn("problem flushing socket output", e);}
            try{_oos.close();}catch(IOException e){_log.warn("problem closing socket output", e);}
            _oos=null;
            try{_socket.close();}catch(IOException e){_log.warn("problem closing socket", e);}     
            _socket=null;
        }
    }
    
    public abstract void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos);
    
}