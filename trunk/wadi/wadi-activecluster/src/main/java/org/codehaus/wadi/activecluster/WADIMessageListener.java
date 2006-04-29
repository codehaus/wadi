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

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * 
 * @version $Revision: 1603 $
 */
class WADIMessageListener implements MessageListener {
    private final org.codehaus.wadi.group.MessageListener adaptee;
    
    public WADIMessageListener(org.codehaus.wadi.group.MessageListener adaptee) {
        this.adaptee = adaptee;
    }

    public void onMessage(Message message) {
        adaptee.onMessage(new ACObjectMessageAdapter(message));
    }
}
