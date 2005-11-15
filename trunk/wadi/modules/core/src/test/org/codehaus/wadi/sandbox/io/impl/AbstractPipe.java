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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.sandbox.io.PeerConfig;
import org.codehaus.wadi.sandbox.io.Pipe;
import org.codehaus.wadi.sandbox.io.PipeConfig;

public abstract class AbstractPipe implements Pipe, PeerConfig  {

    protected static final Log _log=LogFactory.getLog(AbstractPipe.class);

    protected final PipeConfig _config;
    protected final long _timeout;
    
    protected boolean _valid;
    
    public AbstractPipe(PipeConfig config, long timeout) {
        _config=config;
        _timeout=timeout;
        _valid=true;
    }
    
    protected ObjectInputStream _ois;
    public ObjectInputStream getObjectInputStream() throws IOException {
        if (_ois==null)
            _ois=new ObjectInputStream(getInputStream());
        return _ois;
    }
    
    protected ObjectOutputStream _oos;
    public ObjectOutputStream getObjectOutputStream() throws IOException {
        if (_oos==null)
            _oos=new ObjectOutputStream(getOutputStream());
        return _oos;
    }
    
    public void run() {
        try {
            //_log.info("running...");
            //_log.info("starting read...");
            ObjectInputStream ois=getObjectInputStream();
            //_log.info("stream created...");
            Peer peer=(Peer)ois.readObject();
            //_log.info("object read...");
            try {
            run(peer);
            } catch (Exception e) {
                if ( _log.isErrorEnabled() ) {

                    _log.error("problem running Peer", e);
                }
            }
            //_log.info("...ran");
        } catch (EOFException e) {
            // end of the line - fall through...
            if (_log.isTraceEnabled()) _log.trace("Connection reached end of input - quitting...: "+this);
            _valid=false;
        } catch (IOException e) {
            _log.warn("problem reading object off wire", e);
            _valid=false; // this socket is trashed...
        } catch (ClassNotFoundException e) {
            _log.warn("unknown Peer type - version/security problem?", e);
            _valid=false; // this stream is unfixable ?
        } finally {
            _ois=null;
            _oos=null;
            if (_valid)
                _config.notifyIdle(this); // after running, we declare ourselves 'idle' to our Server...
            else
                try {
                    close();
                } catch (IOException e) {
                    if ( _log.isErrorEnabled() ) {

                        _log.error("problem closing server Connection", e);
                    }
                }
        }
        //_log.info("...idle");
    }
    
    public boolean run(Peer peer) throws Exception {
        try {
            return peer.run(this);
        } finally {
            _ois=null;
            _oos=null;
//          if (_valid)
//          _config.notifyIdle(this); // after running, we declare ourselves 'idle' to our Server...
//          else
//          try {
//          close();
//          } catch (IOException e) {
//          _log.error("problem closing server Connection", e);
//          }     
        }
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
    
    // PeerConfig
    
    public Contextualiser getContextualiser() {
        return _config.getContextualiser();
    }

    public String getNodeId() {
        return _config.getNodeId();
    }
}
