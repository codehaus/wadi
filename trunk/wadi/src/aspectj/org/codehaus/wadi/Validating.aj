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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests session validity upon every relevant interraction
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1.5 $
 */
privileged public aspect
  Validating
{
  private static final Log _log=LogFactory.getLog(Validating.class);

  pointcut validate(HttpSession session) :
    execution(* HttpSession.*(..)) &&
    !execution(* HttpSession.isValid()) &&
    !execution(* HttpSession.setValid(..)) &&
    this(session);

  before(HttpSession session)
    : validate(session)
    {
      if (!session._valid)
      {
	// this session has been explicitly invalidated by application
	// code...
	_log.debug("method called on explicitly invalidated HttpSession");
	throw new IllegalStateException("explicitly invalidated HttpSession");
      }

      // no need to test for time-outs - if a thread is in the
      // container with this session, the session should not be
      // timed-out - it has only just been touched.
    }
}
