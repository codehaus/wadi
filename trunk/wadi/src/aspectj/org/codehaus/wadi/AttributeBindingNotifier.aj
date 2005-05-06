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

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public aspect AttributeBindingNotifier {
    
    pointcut setAttribute(Session session, String name, Object newValue) : execution(Object Session+.setAttribute(String, Object)) && args(name, newValue) && target(session);
    
    Object around(Session session, String name, Object newValue) : setAttribute(session, name, newValue) {
        Object oldValue=proceed(session, name, newValue);
        if (null!=oldValue && oldValue instanceof HttpSessionBindingListener)
            ((HttpSessionBindingListener)oldValue).valueUnbound(new HttpSessionBindingEvent(session.getWrapper(), name, oldValue));
        if (newValue instanceof HttpSessionBindingListener)
            ((HttpSessionBindingListener)newValue).valueBound(new HttpSessionBindingEvent(session.getWrapper(), name, newValue));
        return oldValue;
    }
    
    pointcut removeAttribute(Session session, String name) : execution(Object Session+.removeAttribute(String)) && args(name) && target(session);
    
    Object around(Session session, String name) : removeAttribute(session, name) {
        Object oldValue=proceed(session, name);
        if (null!=oldValue && oldValue instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener)oldValue).valueUnbound(new HttpSessionBindingEvent(session.getWrapper(), name, oldValue));
        }
        return oldValue;
    }
}
