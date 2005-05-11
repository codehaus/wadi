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
package org.codehaus.wadi.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ExclusiveDiscMotableConfig;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.StoreMotableConfig;

public class ExclusiveDiscMotable extends AbstractMotable implements StoreMotable {
    
    protected static final Log _log=LogFactory.getLog(ExclusiveDiscMotable.class);
    
    protected ExclusiveDiscMotableConfig _config;
    protected File _file;
    protected int _bodyLength;
    protected long _offset;
    
    public void init(StoreMotableConfig config) { // used when we are going to store something...
        _config=(ExclusiveDiscMotableConfig)config;
    }
    
    public void init(StoreMotableConfig config, String name) throws Exception { // used when we are going to load something...
        _config=(ExclusiveDiscMotableConfig)config;
        _file=new File(_config.getDirectory(), name+_config.getSuffix());
        _offset=loadHeader();

        assert(name.equals(_name));
    }
    
    public byte[] getBodyAsByteArray() throws Exception {
        ensureFile();
        // we already know the body length...
        return (byte[])loadBody(false);
    }
    
    public void setBodyAsByteArray(byte[] body) throws Exception {
        ensureFile();
        _bodyLength=body.length;
        store(false, body);
    }
    
    public ByteBuffer getBodyAsByteBuffer() throws Exception {
        ensureFile();
        // we already know the body length...
        return (ByteBuffer)loadBody(true);
    }
    
    public void setBodyAsByteBuffer(ByteBuffer body) throws Exception {
        ensureFile();
        _bodyLength=body.capacity(); // FIXME - this will work until we start reusing different sized ByteBuffers
        store(true, body);
    }
    
    public void destroy() {
        super.destroy();
        if (_file!=null && _file.exists()) {
            _file.delete();
            if (_log.isTraceEnabled()) _log.trace("removed (exclusive disc): "+_file+": "+_bodyLength+" bytes");
        }
    }
    
    protected void ensureFile() {
        if (_file==null)
            _file=new File(_config.getDirectory(), _name+_config.getSuffix());
    }

    protected long loadHeader() {
        ensureFile();
        FileInputStream fis=null;
        ObjectInputStream ois=null;
        try {
            fis=new FileInputStream(_file);
            ois=new ObjectInputStream(fis);
            _creationTime=ois.readLong();
            _lastAccessedTime=ois.readLong();
            _maxInactiveInterval=ois.readInt();
            _name=(String)ois.readObject();
            _bodyLength=ois.readInt();
            
            if (!checkTimeframe(System.currentTimeMillis()))
                if (_log.isWarnEnabled()) _log.warn("loaded (exclusive disc) from the future!: "+_name);
            
            return fis.getChannel().position();
        } catch (Exception e) {
            if (_log.isErrorEnabled()) _log.warn("load (exclusive disc) failed: "+_file, e);
            return -1;
        }
        finally {
            if (ois!=null)
                try {
                    ois.close();
                } catch (IOException e) {
                    if (_log.isWarnEnabled()) _log.warn("load (exclusive disc) problem: "+_file, e); 
                }
        }
    }
    
    public Object loadBody(boolean useNIO) throws Exception {
        ensureFile();
        FileInputStream fis=null;
        try {
            Object body;
            fis=new FileInputStream(_file);
            
            FileChannel channel=fis.getChannel();
            channel.position(_offset);
            if (useNIO) {
                ByteBuffer buffer=_config.take(_bodyLength);
                channel.read(buffer);
                buffer.flip();
                body=buffer;
            } else {
                byte[] array=new byte[_bodyLength];
                fis.read(array);
                body=array;
            }
            long position=channel.position();
            long bytesLoaded=position-_offset;
            assert (bytesLoaded==_bodyLength);
            if (_log.isTraceEnabled()) _log.trace("loaded ("+(useNIO?"NIO ":"")+"exclusive disc): "+_file+": "+bytesLoaded+" bytes");
            return body;
        } catch (Exception e) {
            if (_log.isErrorEnabled()) _log.error("load ("+(useNIO?"NIO ":"")+"exclusive disc) failed: "+_file);
            throw e;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                if (_log.isWarnEnabled()) _log.warn("load ("+(useNIO?"NIO ":"")+"exclusive disc) problem: "+_file, e);
            }
        }
    }
    
    protected void store(boolean useNIO, Object body) throws Exception {
        ensureFile();
        ObjectOutputStream oos=null;
        FileOutputStream fos=null;
        try {
            fos=new FileOutputStream(_file);
            oos=new ObjectOutputStream(fos);
            
            oos.writeLong(_creationTime);
            oos.writeLong(_lastAccessedTime);
            oos.writeInt(_maxInactiveInterval);
            oos.writeObject(_name);
            oos.writeInt(_bodyLength);
            oos.flush();
            FileChannel channel=fos.getChannel();
            _offset=channel.position();
            if (_bodyLength>0) {
                if (useNIO) {
                    ByteBuffer buffer=(ByteBuffer)body;
                    channel.write(buffer);
                    _config.put(buffer);
                } else {
                    fos.write((byte[])body);
                }
            }
            long bytesStored=channel.position()-_offset;
            assert(_bodyLength==bytesStored);
            if (_log.isTraceEnabled()) _log.trace("stored ("+(useNIO?"NIO ":"")+"exclusive disc): "+_file+": "+bytesStored+" bytes");
        } catch (Exception e) {
            if (_log.isWarnEnabled()) _log.warn("store ("+(useNIO?"NIO ":"")+"exclusive disc) failed: "+_file);
            throw e;
        } finally {
            try {
                if (oos!=null)
                    oos.close();
            } catch (IOException e) {
                if (_log.isWarnEnabled()) _log.warn("store ("+(useNIO?"NIO ":"")+"exclusive disc) problem: "+_file, e);
            }
        }
    }

}

