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
package org.codehaus.wadi.core.session;

import java.io.Externalizable;
import java.util.Map;

import org.codehaus.wadi.core.motable.Motable;



/**
 * A Session is an object with Time-To-Live (See Motable) and Locking semantics.
 * In a distributable situation its content is Serializable. Different subtypes
 * of Session may choose to implement their payload differently - i.e. a Stateful
 * Session Bean (EJB) looks very different from an HttpSession (Web), but both could
 * inherit or be wrapped by this interface.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Session extends Motable, Externalizable {
    /**
     * Map like contract to manipulate state information.
     */
    Object addState(Object key, Object value);

    /**
     * Map like contract to manipulate state information.
     */
    Object getState(Object key);

    /**
     * Map like contract to manipulate state information.
     */
    Object removeState(Object key);

    Map getState();
    
    void onEndProcessing();
    
    Map<Object, Object> getLocalStateMap();
}
