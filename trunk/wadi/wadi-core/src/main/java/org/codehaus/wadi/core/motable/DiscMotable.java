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
package org.codehaus.wadi.core.motable;

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
import org.codehaus.wadi.DiscMotableConfig;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.StoreMotableConfig;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DiscMotable extends AbstractMotable implements StoreMotable {
    protected static final Log _log = LogFactory.getLog(DiscMotable.class);

    protected DiscMotableConfig _config;
    protected File _file;
    protected int _bodyLength;
    protected long _offset;

    public void init(StoreMotableConfig config) {
        _config = (DiscMotableConfig) config;
    }

    public void init(StoreMotableConfig config, String name) throws Exception {
        _config = (DiscMotableConfig) config;
        this.name = name;
        _offset = loadHeader();
    }

    public byte[] getBodyAsByteArray() throws Exception {
        ensureFile();
        // we already know the body length...
        return (byte[]) loadBody(false);
    }

    public void setBodyAsByteArray(byte[] body) throws Exception {
        ensureFile();
        _bodyLength = body.length;
        store(false, body);
    }

    // normal destruction - should remove corresponding row from db...
    public void destroy() throws Exception {
        super.destroy();
        if (_file != null && _file.exists()) {
            _file.delete();
            if (_log.isTraceEnabled()) {
                _log.trace("removed (exclusive disc): " + _file + ": " + _bodyLength + " bytes");
            }
        }
    }

    // destruction as our data is transferred to another storage medium.
    // if this is the same medium as we are using for replication, we do not
    // want to remove this copy, as it saves making a fresh replicant...
    public void destroy(Motable recipient) throws Exception {
        super.destroy();

        if (!_config.getReusingStore()) {
            if (_file != null && _file.exists()) {
                _file.delete();
                if (_log.isTraceEnabled()) {
                    _log.trace("removed (exclusive disc): " + _file + ": " + _bodyLength + " bytes");
                }
            }
        }
    }

    protected void ensureFile() {
        if (_file == null) {
            _file = new File(_config.getDirectory(), name + _config.getSuffix());
        }
    }

    protected long loadHeader() {
        ensureFile();
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(_file);
            ois = new ObjectInputStream(fis);
            creationTime = ois.readLong();
            lastAccessedTime = ois.readLong();
            maxInactiveInterval = ois.readInt();
            name = (String) ois.readObject();
            _bodyLength = ois.readInt();
            return fis.getChannel().position();
        } catch (Exception e) {
            _log.warn("load (exclusive disc) failed: " + _file, e);
            return -1;
        } finally {
            if (ois != null)
                try {
                    ois.close();
                } catch (IOException e) {
                    _log.warn("load (exclusive disc) problem: " + _file, e);
                }
        }
    }

    public Object loadBody(boolean useNIO) throws Exception {
        ensureFile();
        FileInputStream fis = null;
        try {
            Object body;
            fis = new FileInputStream(_file);

            FileChannel channel = fis.getChannel();
            channel.position(_offset);
            if (useNIO) {
                ByteBuffer buffer = _config.take(_bodyLength);
                channel.read(buffer);
                buffer.flip();
                body = buffer;
            } else {
                byte[] array = new byte[_bodyLength];
                fis.read(array);
                body = array;
            }
            long position = channel.position();
            long bytesLoaded = position - _offset;
            assert (bytesLoaded == _bodyLength);
            if (_log.isTraceEnabled()) {
                _log.trace("loaded (" + (useNIO ? "NIO " : "") + "exclusive disc): " + _file + ": " + bytesLoaded
                        + " bytes");
            }
            return body;
        } catch (Exception e) {
            _log.error("load (" + (useNIO ? "NIO " : "") + "exclusive disc) failed: " + _file);
            throw e;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                _log.warn("load (" + (useNIO ? "NIO " : "") + "exclusive disc) problem: " + _file, e);
            }
        }
    }

    protected void store(boolean useNIO, Object body) throws Exception {
        ensureFile();
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(_file);
            oos = new ObjectOutputStream(fos);

            oos.writeLong(creationTime);
            oos.writeLong(lastAccessedTime);
            oos.writeInt(maxInactiveInterval);
            oos.writeObject(name);
            oos.writeInt(_bodyLength);
            oos.flush();
            FileChannel channel = fos.getChannel();
            _offset = channel.position();
            if (_bodyLength > 0) {
                if (useNIO) {
                    ByteBuffer buffer = (ByteBuffer) body;
                    channel.write(buffer);
                    _config.put(buffer);
                } else {
                    fos.write((byte[]) body);
                }
            }
            long bytesStored = channel.position() - _offset;
            assert (_bodyLength == bytesStored);
            if (_log.isTraceEnabled()) {
                _log.trace("stored (" + (useNIO ? "NIO " : "") + "exclusive disc): " + _file + ": " + bytesStored
                        + " bytes");
            }
        } catch (Exception e) {
            _log.warn("store (" + (useNIO ? "NIO " : "") + "exclusive disc) failed: " + _file);
            throw e;
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                _log.warn("store (" + (useNIO ? "NIO " : "") + "exclusive disc) problem: " + _file, e);
            }
        }
    }

}

