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

package org.codehaus.wadi.old;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Guards against insertion of a null key into session
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
privileged public aspect
  NameChecking
{
  private static final Log _log=LogFactory.getLog(NameChecking.class);

  pointcut checkName(String name) :
    execution(* HttpSessionImpl.*Attribute(..)) &&
    args(name,..);

  before(String name)
    : checkName(name)
    {
      if (name==null)
      {
	_log.debug("HttpSession attribute name was null");
	throw new IllegalArgumentException("HttpSession attribute name was null");
      }
    }
}
