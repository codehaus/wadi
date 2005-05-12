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
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Consumer implements Runnable {
    
    protected static final Log _log=LogFactory.getLog(Consumer.class);
    protected final Socket _socket;
    protected final Listener _listener;
    protected final Object _token;
    
    public Consumer(Socket socket, Listener listener, Object token) {
        _socket=socket;
        _listener=listener;
        _token=token;
    }
    
    public void run() {
        //_log.info("Consumer started...: "+_socket);
        ObjectInputStream  ois=null;
        ObjectOutputStream oos=null;
       try {
            oos=new ObjectOutputStream(_socket.getOutputStream());
            ois=new ObjectInputStream(_socket.getInputStream());
            Executable executable=(Executable)ois.readObject();
            executable.process(_socket, ois, oos);
       } catch (IOException e) {
            _log.warn("connection broken - aborting", e);
        } catch (ClassNotFoundException e) {
            _log.warn("unknown Processor - version/security problem?", e);
        } finally {
            try{if (ois!=null) ois.close();}catch(IOException e){_log.warn("problem closing socket input",e);}
            try{if (oos!=null) oos.close();}catch(IOException e){_log.warn("problem closing socket output",e);}
            try{_socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
            _listener.notify(_token);
        }
        //_log.info("...Consumer finished: "+Thread.currentThread());
    }
}