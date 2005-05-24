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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractServerConnection implements Connection, PeerConfig  {

    protected static final Log _log=LogFactory.getLog(AbstractServerConnection.class);

    protected final ConnectionConfig _config;
    protected final long _timeout;
    
    protected boolean _running;
    
    public AbstractServerConnection(ConnectionConfig config, long timeout) {
        _config=config;
        _timeout=timeout;
        _running=true;
    }
    
    public void run() {
        InputStream is=null;
        try {
            //_log.info("running...");
            is=getInputStream();
            //_log.info("starting read...");
            ObjectInputStream ois=new ObjectInputStream(is);
            //_log.info("stream created...");
            Peer peer=(Peer)ois.readObject();
            //_log.info("object read...");
            run(peer);
            //_log.info("...ran");
        } catch (EOFException e) {
            // end of the line - fall through...
            if (_log.isTraceEnabled()) _log.trace("Connection reached end of input - quitting...: "+this);
            _running=false;
        } catch (IOException e) {
            _log.warn("problem reading object off wire", e);
            _running=false; // this socket is trashed...
        } catch (ClassNotFoundException e) {
            _log.warn("unknown Peer type - version/security problem?", e);
            _running=false; // this stream is unfixable ?
        } finally {
            if (_running)
                _config.notifyIdle(this); // after running, we declare ourselves 'idle' to our Server...
            else
                try {
                    close();
                } catch (IOException e) {
                    _log.error("problem closing server Connection", e);
                }
        }
        //_log.info("...idle");
    }
    
    public void run(Peer peer) throws IOException {
        peer.run(this);
    }
    
    public void close() throws IOException {
        //_log.info("closing...");
        InputStream is=getInputStream();
        OutputStream os=getOutputStream();
        try{os.flush();}catch(IOException e){_log.warn("problem flushing socket output",e);}
        try{is.close();}catch(IOException e){_log.warn("problem closing socket input",e);}
        try{os.close();}catch(IOException e){_log.warn("problem closing socket output",e);}
        _config.notifyClosed(this);
        //_log.info("...closed");
    }

    // WritableByteChannel - default behaviour is not to support this...

    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }
}
