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
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.core.util.Utils;

/**
 * 
 * @version $Revision: 2340 $
 */
public class SessionStateHandler implements ObjectStateHandler {
    protected final Streamer streamer;
    protected SessionFactory sessionFactory;
    
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
    
    public byte[] extractFullState(Object key, Motable target) {
        Externalizable externalizable = newExtractFullStateExternalizable(key, target);
        try {
            return Utils.getContent(externalizable, streamer);
        } catch (IOException e) {
            throw new WADIRuntimeException(e);
        }
    }

    public byte[] extractUpdatedState(Object key, Motable target) {
        Externalizable externalizable = newExtractUpdatedStateExternalizable(key, target);
        try {
            return Utils.getContent(externalizable, streamer);
        } catch (IOException e) {
            throw new WADIRuntimeException(e);
        }
    }

    public void resetObjectState(Motable target) {
    }
    
    public Motable restoreFromFullState(Object key, Motable motable) {
        Session session = newSession(key);
        try {
            session.restore(motable.getCreationTime(), 
                    motable.getLastAccessedTime(),
                    motable.getMaxInactiveInterval(),
                    motable.getId(),
                    motable.getBodyAsByteArray());
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        return session;
    }
    
    public Motable restoreFromFullState(Object key, byte[] state) {
        Session session = newSession(key);
        try {
            Utils.setContent(session, state, streamer);
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        return session;
    }

    public Motable restoreFromFullStateTransient(Object key, byte[] state) {
        return restoreFromFullState(key, state);
    }
    
    public Motable restoreFromUpdatedState(Object key, byte[] state) {
        Session session = newSession(key);
        try {
            Utils.setContent(session, state, streamer);
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        return session;
    }
    
    public void discardState(Object key, Motable payload) {
    }
    
    public void initState(Object key, Motable payload) {
    }
    
    protected Externalizable newExtractFullStateExternalizable(Object key, Object target) {
        if (false == target instanceof Session) {
            throw new IllegalArgumentException(target.getClass().getName() + " is not a Session");
        }
        return (Session) target;
    }

    protected Externalizable newExtractUpdatedStateExternalizable(Object key, Object target) {
        if (false == target instanceof Session) {
            throw new IllegalArgumentException(target.getClass().getName() + " is not a Session");
        }
        return (Session) target;
    }
    
    protected Session newSession(Object key) {
        return sessionFactory.create();
    }

}