/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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

package org.codehaus.wadi.tomcat;

import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import org.apache.catalina.Container;
import org.apache.catalina.DefaultContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


// used by:

// Session: creation, destruction

// corresponds to calls to fireSessionEvent in
// org.apache.catalina.session.StandardSession


public aspect
  SessionNotification
{
  protected static final Log _log=LogFactory.getLog(SessionNotification.class);

  pointcut notify(HttpSession session)
    : execution(void HttpSession.nowhere(HttpSession)) && target(session);

  void
    around(HttpSession session)
    : notify(session)
  {}
}
