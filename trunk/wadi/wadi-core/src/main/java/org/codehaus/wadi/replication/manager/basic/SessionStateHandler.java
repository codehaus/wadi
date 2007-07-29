/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.replication.manager.basic;

import java.io.Externalizable;
import java.io.IOException;

import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.core.util.Utils;

/**
 * 
 * @version $Revision: 2340 $
 */
public class SessionStateHandler implements ObjectStateHandler {
    private final Streamer streamer;
    private SessionFactory sessionFactory;
    
    public SessionStateHandler(Streamer streamer) {
        if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        }
        this.streamer = streamer;
    }
    
    public void setObjectFactory(Object factory) {
        if (null == factory) {
            throw new IllegalArgumentException("factory is required");
        } else if (!(factory instanceof SessionFactory)) {
            throw new IllegalArgumentException(factory.getClass().getName() + " is not " + SessionFactory.class + "");
        }
        this.sessionFactory = (SessionFactory) factory;
    }
    
    public byte[] extractFullState(Object key, Object target) {
        Externalizable externalizable = newExternalizable(key, target);
        try {
            return Utils.getContent(externalizable, streamer);
        } catch (IOException e) {
            throw new WADIRuntimeException(e);
        }
    }

    public byte[] extractUpdatedState(Object key, Object target) {
        Externalizable externalizable = newExternalizable(key, target);
        try {
            return Utils.getContent(externalizable, streamer);
        } catch (IOException e) {
            throw new WADIRuntimeException(e);
        }
    }

    public Object restoreFromFullState(Object key, byte[] state) {
        Externalizable externalizable = newExternalizable(key);
        try {
            Utils.setContent(externalizable, state, streamer);
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        return externalizable;
    }

    public Object restoreFromUpdatedState(Object key, byte[] state) {
        Externalizable externalizable = newExternalizable(key);
        try {
            Utils.setContent(externalizable, state, streamer);
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        return externalizable;
    }
    
    protected Externalizable newExternalizable(Object key, Object tmp) {
        if (false == tmp instanceof Session) {
            throw new IllegalArgumentException(tmp.getClass().getName() + " is not a Session");
        }
        return (Session) tmp;
    }

    protected Externalizable newExternalizable(Object key) {
        return sessionFactory.create();
    }

}