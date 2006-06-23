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
package org.codehaus.wadi.jgroups.messages;

import java.io.Serializable;
import java.util.Map;

import org.codehaus.wadi.Message;
import org.codehaus.wadi.jgroups.Utils;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class StateUpdate implements Message, Serializable {

    protected static final String _prefix="<"+Utils.basename(StateUpdate.class)+": ";
    protected static final String _suffix=">";

    protected final Map _state;

    public StateUpdate(Map state) {
        _state=state;
    }

    public Map getState() {
        return _state;
    }

    // TODO - custom serialiser

    public String toString() {
        return _prefix+_state+_suffix; 
    }

}
