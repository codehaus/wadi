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

package org.codehaus.wadi.shared;

import java.lang.reflect.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

// TODO - Distributable sessions only...

// TODO - aspect introspection is cool, but expensive. We should be
// able to write a pointcut for each mutator, which will know it's
// method and cache it, saving us allocation of introspection
// components every time we go through this aspect. - the try/catch is
// probably bad news as well..

/**
 * Detects deltas and replicates them to listening peers - NYI
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public aspect
  Replicating
{
  private static final Log _log=LogFactory.getLog(Replicating.class);

  pointcut invocation(AbstractHttpSessionImpl ahsi) :
    execution(Object HttpSessionSetters.foo*(..)) && target(ahsi);

  before(AbstractHttpSessionImpl ahsi)
    : invocation(ahsi)
    {
      String    id          =ahsi.getId();
      Signature signature   =thisJoinPointStaticPart.getSignature();
      String    methodName  =signature.getName();
      Class[]   methodTypes =((MethodSignature)signature).getParameterTypes();
      Method    method      =null;
      try
      {
	method=HttpSessionSetters.class.getMethod(methodName, methodTypes);
      }
      catch (NoSuchMethodException e)
      {
	_log.warn("replication failed - should never happen", e);
      }

      // using thisJoinPoint is apparently expensive...but the only way... :-(
      Object[]  methodArgs  =thisJoinPoint.getArgs();
      ahsi.getWadiManager().replicate(id, method, methodArgs);
    }
}
