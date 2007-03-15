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
package org.codehaus.wadi.location.endpoint;

import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.session.MovePMToSM;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * @version $Revision:1815 $
 */
public class RelocationImmoterTest extends RMockTestCase {

    public void testSuccessfulMigration() throws Exception {
        SessionMonitor sessionMonitor = (SessionMonitor) mock(SessionMonitor.class);
        Dispatcher dispatcher = (Dispatcher) mock(Dispatcher.class);
        Envelope envelope = (Envelope) mock(Envelope.class);
        int inactivePeriod = 10;
        Peer im = (Peer) mock(Peer.class);
        MovePMToSM pmToSm = new MovePMToSM("key", im, "corID");
        
        Session sesstionToMigrate = (Session) mock(Session.class);
        sessionMonitor.notifyOutbountSessionMigration(sesstionToMigrate);
        startVerification();
        
        RelocationImmoter immoter = new RelocationImmoter(sessionMonitor, dispatcher, envelope, pmToSm, inactivePeriod);
        
        immoter.newMotable(sesstionToMigrate);
    }
    
}