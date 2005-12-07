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

package org.codehaus.wadi.impl;

/**
 * This class is NOT for production use. It is useful for debugging
 * race conditions.
 *
 * @author <a href="mailto:jules@coredevelopers.net"></a>
 * @version $Revision$
 */
public class
  SimpleLog
  extends org.apache.commons.logging.impl.SimpleLog
{
  public SimpleLog(String name){super(name);}

  protected void
    log(int type, Object message, Throwable t)
  {
    synchronized (System.err)	// yeugh !
    {
      System.err.print("{"+Thread.currentThread().getName()+"} "); // yeugh !
      super.log(type, message, t);
    }
  }
}
