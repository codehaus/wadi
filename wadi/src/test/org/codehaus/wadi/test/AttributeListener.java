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

package org.codehaus.wadi.test;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionAttributeListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AttributeListener
  implements
    HttpSessionAttributeListener
{
  protected Log _log=LogFactory.getLog(AttributeListener.class);

  public
    AttributeListener()
  {
    _log.trace("ctor");
  }

  public void
    attributeAdded(HttpSessionBindingEvent e)
  {
    _log.trace("attributeAdded()");
  }

  public void
    attributeRemoved(HttpSessionBindingEvent e)
  {
    _log.trace("attributeRemoved()");
  }

  public void
    attributeReplaced(HttpSessionBindingEvent e)
  {
    _log.trace("attributeReplaced()");
  }
}
