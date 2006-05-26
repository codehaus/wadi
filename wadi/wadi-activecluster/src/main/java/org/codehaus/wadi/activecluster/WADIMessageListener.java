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
package org.codehaus.wadi.activecluster;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @version $Revision: 1603 $
 */
class WADIMessageListener implements MessageListener {

    protected static final Log _log = LogFactory.getLog(WADIMessageListener.class);

    private final org.codehaus.wadi.group.MessageListener adaptee;
    private final ActiveClusterCluster cluster;

    public WADIMessageListener(ActiveClusterCluster cluster, org.codehaus.wadi.group.MessageListener adaptee) {
        this.cluster = cluster;
        this.adaptee = adaptee;
    }

    public void onMessage(Message message) {
        ActiveClusterCluster._cluster.set(cluster); // attach cluster to a ThreadLocal for future use...
        try {
            adaptee.onMessage(new ActiveClusterMessage(cluster, (ObjectMessage)message));
        } catch (JMSException e) {
            _log.error("ActiveCluster issue: could not demarshall incoming message", e);
        }
    }
}
