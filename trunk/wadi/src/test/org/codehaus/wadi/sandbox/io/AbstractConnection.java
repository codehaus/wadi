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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractConnection implements Runnable {

    protected static final Log _log=LogFactory.getLog(AbstractConnection.class);

    public abstract InputStream getInputStream() throws IOException;
    public abstract OutputStream getOutputStream() throws IOException;
    public abstract void close();
    public abstract Socket getSocket();
    
    public void run() {
        //_log.info("Connection started...: "+getSocket());
        ObjectInputStream  ois=null;
        ObjectOutputStream oos=null;
        try {
            oos=new ObjectOutputStream(getOutputStream());
            ois=new ObjectInputStream(getInputStream());
            Peer peer=(Peer)ois.readObject();
            peer.process(getSocket(), ois, oos);
        } catch (IOException e) {
            _log.warn("connection broken - aborting", e);
        } catch (ClassNotFoundException e) {
            _log.warn("unknown Peer type - version/security problem?", e);
        } finally {
            try{if (oos!=null) oos.flush();}catch(IOException e){_log.warn("problem flushing socket output",e);}
            try{if (ois!=null) ois.close();}catch(IOException e){_log.warn("problem closing socket input",e);}
            try{if (oos!=null) oos.close();}catch(IOException e){_log.warn("problem closing socket output",e);}
            close();
        }
        //_log.info("...Connection finished: "+Thread.currentThread());
    }
}
