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

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test session lifecycle listener
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SessionListener
  implements
    HttpSessionListener
{
  protected Log _log=LogFactory.getLog(SessionListener.class);

  public
    SessionListener()
  {
    _log.trace("ctor");
  }

  public void
    sessionCreated(HttpSessionEvent e)
  {
    _log.trace("sessionCreated()");
  }

  public void
    sessionDestroyed(HttpSessionEvent e)
  {
    _log.trace("sessionDestroyed()");
  }
}
