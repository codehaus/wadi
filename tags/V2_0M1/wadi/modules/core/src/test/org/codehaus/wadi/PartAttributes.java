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
package org.codehaus.wadi;

import java.util.Set;

import javax.servlet.http.HttpSessionEvent;

import org.codehaus.wadi.Dirtier;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class PartAttributes extends SimpleAttributes implements DistributableValueConfig {

    public PartAttributes(Dirtier dirtier, Streamer streamer, boolean evictObjectRepASAP, boolean evictByteRepASAP) {
        // NYI
    }
    
    public Set getBindingListenerNames() {return null;} //NYI
    public Set getActivationListenerNames() {return null;} //NYI
    
    public HttpSessionEvent getHttpSessionEvent() {return null;} //NYI
    public Streamer getStreamer() {return null;} //NYI
    public Dirtier getDirtier() {return null;} //NYI
    public ValueHelper findHelper(Class type) {return null;} //NYI
    public boolean getHttpSessionAttributeListenersRegistered(){return false;}// NYI

}
