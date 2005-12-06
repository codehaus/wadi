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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.Signature;

public aspect Tracing
{
  protected Log _log=LogFactory.getLog(Tracing.class);

  pointcut traceMethods()
    : execution(* *.nothing(..)) && !within(Tracing);

  Object
    around() : traceMethods()
  {
    Signature sig = thisJoinPointStaticPart.getSignature();
    Object[] args = thisJoinPoint.getArgs();
    String pkg=sig.getDeclaringType().getName();
    String name=sig.getName();
    String a="";
    for (int i=0;i<args.length;i++)
      a+=((a.length()==0)?"":",")+args[i];
    _log.trace("Entering "+pkg+"."+name+"("+a+")");

    Object tmp=proceed();
    _log.trace("Leaving "+pkg+"."+name+"() --> "+tmp);

    return tmp;
  }
}