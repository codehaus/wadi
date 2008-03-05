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
package org.codehaus.wadi.core.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.util.Streamer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DiscStore implements Store {
    private final Log log = LogFactory.getLog(DiscStore.class);

    public static final String SESSION_STORE_DIR_NAME = "SessionStore";

    protected final Streamer streamer;
    protected final File sessionStoreDir;
    private final boolean accessOnLoad;

    public DiscStore(Streamer streamer, File dir, boolean accessOnLoad) throws Exception {
        if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        } else if (null == dir) {
            throw new IllegalArgumentException("dir is required");
        }
        this.streamer = streamer;
        this.accessOnLoad = accessOnLoad;

        sessionStoreDir = new File(dir, SESSION_STORE_DIR_NAME);
        if (!sessionStoreDir.exists()) {
            log.info("Creating directory: " + sessionStoreDir.getCanonicalPath());
            if (!sessionStoreDir.mkdirs()) {
                throw new IOException("Couldn't create directory " + sessionStoreDir.getAbsolutePath());
            }
        }

        try {
            File.createTempFile("DiscStore_WriteTest", null, sessionStoreDir).delete();
        } catch (IOException e) {
            log.error("bad directory: " + sessionStoreDir, e);
            throw e;
        }
    }

    public void clean() {
        File[] files = sessionStoreDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
        if (log.isInfoEnabled()) {
            log.info("removed (exclusive disc): " + files.length + " files");
        }
    }

    public void load(Putter putter) {
        long time = System.currentTimeMillis();
        String[] list = sessionStoreDir.list();
        int suffixLength = ".".length() + streamer.getSuffix().length();
        for (int i = 0; i < list.length; i++) {
            String name = list[i];
            String id = name.substring(0, name.length() - suffixLength);
            
            Motable motable = new BasicStoreMotable(this);
            File file = new File(sessionStoreDir, id + streamer.getSuffixWithDot());
            FileInputStream fis = null;
            ObjectInputStream ois = null;
            try {
                fis = new FileInputStream(file);
                ois = new ObjectInputStream(fis);
                long creationTime = ois.readLong();
                long lastAccessedTime = ois.readLong();
                int maxInactiveInterval = ois.readInt();
                name = (String) ois.readObject();
                motable.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
                if (accessOnLoad) {
                    motable.setLastAccessedTime(time);
                }
                if (!motable.getTimedOut(time)) {
                    putter.put(id, motable);
                }
            } catch (Exception e) {
                log.warn("load (exclusive disc) failed [" + file + "]", e);
            } finally {
                try {
                    if (null != ois) {
                        ois.close();
                    }
                } catch (IOException e) {
                    log.warn("load (exclusive disc) problem [" + file + "]", e);
                }
            }
        }
        log.info("loaded (exclusive disc): " + list.length);
    }

    public Motable create() {
        return new BasicStoreMotable(this);
    }

    public void delete(Motable motable) {
        File file = new File(sessionStoreDir, motable.getName() + streamer.getSuffixWithDot());
        if (file.exists()) {
            file.delete();
            if (log.isTraceEnabled()) {
                log.trace("removed (exclusive disc) [" + file + "]");
            }
        }
    }

    public void insert(Motable motable) throws Exception {
        File file = new File(sessionStoreDir, motable.getName() + streamer.getSuffixWithDot());

        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);

            oos.writeLong(motable.getCreationTime());
            oos.writeLong(motable.getLastAccessedTime());
            oos.writeInt(motable.getMaxInactiveInterval());
            oos.writeObject(motable.getName());
            byte[] bodyAsByteArray = motable.getBodyAsByteArray();
            oos.writeInt(bodyAsByteArray.length);
            oos.flush();
            if (bodyAsByteArray.length > 0) {
                fos.write(bodyAsByteArray);
            }
            if (log.isTraceEnabled()) {
                log.trace("stored disc motable): " + file + ": " + bodyAsByteArray.length + " bytes");
            }
        } catch (Exception e) {
            log.warn("store exclusive disc failed. File [" + file + "]", e);
            throw e;
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                log.warn("store exclusive disc) problem. File [" + file + "]", e);
            }
        }
    }

    public byte[] loadBody(Motable motable) throws Exception {
        File file = new File(sessionStoreDir, motable.getName() + streamer.getSuffixWithDot());

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ois.readLong();
            ois.readLong();
            ois.readInt();
            ois.readObject();
            int bodyLength = ois.readInt();
            byte[] body = new byte[bodyLength];
            fis.read(body);
            if (log.isTraceEnabled())  {
                log.trace("loaded exclusive disc: " + file + ": " + bodyLength + " bytes");
            }
            return body;
        } catch (Exception e) {
            log.error("load exclusive disc failed: " + file, e);
            throw e;
        } finally {
            try {
                if (null != fis) {
                    fis.close();
                }
            } catch (IOException e) {
                log.warn("load exclusive disc problem: " + file, e);
            }
        }
    }

}
