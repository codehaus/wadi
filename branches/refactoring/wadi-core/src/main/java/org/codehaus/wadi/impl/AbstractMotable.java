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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.RehydrationException;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Implement all of Motable except for the Bytes field. This is the field most likely to have different representations.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractMotable extends SimpleEvictable implements Motable, Serializable {
    protected static Log _log = LogFactory.getLog(AbstractMotable.class);

    protected String _name;
    protected boolean newSession = true;
    protected transient Sync _lock = new Mutex();

    public Sync getLock() {
        return _lock;
    }

    public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name) {
        init(creationTime, lastAccessedTime, maxInactiveInterval);
        _name = name;
    }

    public void rehydrate(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body)
            throws RehydrationException {
        initExisting(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
    }

    public void restore(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body)
            throws RehydrationException {
        initExisting(creationTime, lastAccessedTime, maxInactiveInterval, name, body);
    }

    public void copy(Motable motable) throws Exception {
        super.copy(motable);
        _name = motable.getName();
        setBodyAsByteArray(motable.getBodyAsByteArray());
    }

    public void mote(Motable recipient) throws Exception {
        recipient.copy(this);
        destroy();
    }

    public String getName() {
        return _name;
    }

    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        super.readContent(oi);
        _name = (String) oi.readObject();
        _lock = new Mutex();
    }

    public void writeContent(ObjectOutput oo) throws IOException {
        super.writeContent(oo);
        oo.writeObject(_name);
    }

    private void initExisting(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name, byte[] body) throws RehydrationException {
        newSession = false;
        init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        try {
            setBodyAsByteArray(body);
        } catch (Exception e) {
            throw new RehydrationException(e);
        }
    }

}


