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
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DiscMotableConfig;
import org.codehaus.wadi.Store;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.core.motable.DiscMotable;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DiscStore implements Store, DiscMotableConfig {
    private final Log _log = LogFactory.getLog(DiscStore.class);

    protected final Streamer _streamer;
    protected final File _dir;
    protected final boolean _useNIO;
    protected final DirectByteBufferCache _cache = new DirectByteBufferCache();
    protected final boolean _reusingStore;

    public DiscStore(Streamer streamer, File dir, boolean useNIO, boolean reusingStore) throws Exception {
        _streamer = streamer;
        _dir = dir;
        _useNIO = useNIO;
        _reusingStore = reusingStore;

        if (!dir.exists()) {
            _log.info("Creating directory: " + _dir.getCanonicalPath());
            if (!dir.mkdirs()) {
                throw new IOException("Couldn't create directory " + _dir.getCanonicalPath());
            }
        }

        try {
            File.createTempFile("DiscStore_WriteTest", null, _dir).delete();
        } catch (IOException e) {
            _log.error("bad directory: " + _dir, e);
            throw e;
        }
    }

    public void clean() {
        File[] files = _dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        if (_log.isInfoEnabled()) {
            _log.info("removed (exclusive disc): " + files.length + " files");
        }
    }

    public void load(Putter putter, boolean accessOnLoad) {
        long time = System.currentTimeMillis();
        String[] list = _dir.list();
        int suffixLength = ".".length() + _streamer.getSuffix().length();
        for (int i = 0; i < list.length; i++) {
            String name = list[i];
            String id = name.substring(0, name.length() - suffixLength);
            DiscMotable motable = new DiscMotable();
            try {
                motable.init(this, id);
                if (accessOnLoad) {
                    motable.setLastAccessedTime(time);
                }
                if (!motable.getTimedOut(time)) {
                    putter.put(id, motable);
                }
            } catch (Exception e) {
                _log.error("failed to load [" + name + "]", e);
            }
        }
        _log.info("loaded (exclusive disc): " + list.length);
    }

    public StoreMotable create() {
        return new DiscMotable();
    }

    public File getDirectory() {
        return _dir;
    }

    public String getSuffix() {
        return _streamer.getSuffixWithDot();
    }

    public boolean getUseNIO() {
        return _useNIO;
    }

    public ByteBuffer take(int size) {
        return _cache.take(size);
    }

    public void put(ByteBuffer buffer) {
        _cache.put(buffer);
    }

    public boolean getReusingStore() {
        return _reusingStore;
    }

}
