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
package org.codehaus.wadi.sandbox.impl;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

// IDEA - implement later

// With an event like this, even if an HttpSessionAttributeListener is registered with the Context
// we should be able to avoid deserialisation of a LazyValue, even on session invalidation, unless
// the LazyValue's value is actually referenced through the event. This would be a substantial saving.

// The same should be possible for LazyAttributes - the first direct reference through the event would
// deserialise all attribute Values. This will involve changing the way that lazyAttribute serialises.
// it should probably write out numAttributes, then foreach attribute: listener? and key then foreach
// attribute: value... etc..

// Ultimately, if session metadata encodes whether a session is carrying binding or activation listeners
// it should be possible to e.g. expire sessions without listeners directly on disc without reloading (unless
// listeners are registered with the Context). This would also be a big win.

// I think we will have to change the Value.setValue() sig to include the attribute Name, so that events can be
// raised from this point. We will also probably have to collapse the BindingNotification aspect into the Value class
// so that we can keep taks on which attributes are listeners and which are not.

public class LazyHttpSessionBindingEvent extends HttpSessionBindingEvent {

    public LazyHttpSessionBindingEvent(HttpSession session, String name) {
        super(session, name);
    }

    public LazyHttpSessionBindingEvent(HttpSession session, String name, Object value) {
        super(session, name, value);
    }
    
    public Object getValue() {
        LazyValue value=(LazyValue)super.getValue();
        return value==null?null:value.getValue();
    }

}
