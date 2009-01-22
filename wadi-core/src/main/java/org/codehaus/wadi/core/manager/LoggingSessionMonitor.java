/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.core.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.session.Session;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class LoggingSessionMonitor implements SessionMonitor {
    private static final Log LOG = LogFactory.getLog(LoggingSessionMonitor.class);
    
    private final SessionMonitor delegate;

    public LoggingSessionMonitor(SessionMonitor delegate) {
        if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        }
        this.delegate = delegate;
    }


    public void addSessionListener(SessionListener sessionListener) {
        delegate.addSessionListener(sessionListener);
    }
    
    public void removeSessionListener(SessionListener sessionListener) {
        delegate.removeSessionListener(sessionListener);
    }

    public void notifyInboundSessionMigration(Session session) {
        LOG.debug(delegate + " - notifyInboundSessionMigration [" + session.getName() + "]");
        delegate.notifyInboundSessionMigration(session);
    }

    public void notifyOutboundSessionMigration(Session session) {
        LOG.debug(delegate + " - notifyOutboundSessionMigration [" + session.getName() + "]");
        delegate.notifyOutboundSessionMigration(session);
    }

    public void notifySessionCreation(Session session) {
        LOG.debug(delegate + " - notifySessionCreation [" + session.getName() + "]");
        delegate.notifySessionCreation(session);
    }

    public void notifySessionDestruction(Session session) {
        LOG.debug(delegate + " - notifySessionDestruction [" + session.getName() + "]");
        delegate.notifySessionDestruction(session);
    }
}
