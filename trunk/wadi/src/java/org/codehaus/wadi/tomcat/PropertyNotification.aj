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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TC carries so much notification baggage in its code that it seemed
// to be begging to have it Aspected out...

public aspect
  PropertyNotification
{
  protected static final Log _log=LogFactory.getLog(PropertyNotification.class);

  pointcut setter(Manager manager, Object newValue) :
    execution(void Manager.set*(Object)) && args(newValue) && target(manager);

  void
    around(Manager manager, Object newValue)
    : setter(manager, newValue)
    {
      String name=null;		// TODO - figure out attribute name
      Object oldValue=null;	// invoke corresponding getter here...
      proceed(manager, newValue);

      _log.trace(name+" setter called");
      manager._propertyChangeListeners.firePropertyChange(name, oldValue, newValue);
    }
}
