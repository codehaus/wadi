/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.group.impl;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.Quipu;

/**
 * 
 * @version $Revision: 1603 $
 */
class RendezVousMsgDispatcher extends AbstractMsgDispatcher {
    private static final Log log = LogFactory.getLog(RendezVousMsgDispatcher.class);
    
    public RendezVousMsgDispatcher(Dispatcher dispatcher, Class type) {
        super(dispatcher, type);
    }

    public void dispatch(Message om) throws Exception {
        String correlationId = om.getIncomingCorrelationId();
        Map rendezVousMap = _dispatcher.getRendezVousMap();
        synchronized (rendezVousMap) {
            Quipu rv= (Quipu) rendezVousMap.get(correlationId);
            if (null == rv) {
                log.warn("no one waiting for: " + correlationId);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("successful correlation: " + correlationId);
                }
                rv.putResult(om);
            }
        }
    }

    public String toString() {
        return "<RendezVousMsgDispatcher>";
    }
}