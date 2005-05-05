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
import org.aspectj.lang.Signature;

/**
 * Tracing utility - development use ONLY
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1.8 $
 */
public aspect
  Tracing
{
  protected Log _log=LogFactory.getLog(Tracing.class);

  pointcut traceMethods(Object lock)
    :
  (
   call(* EDU.oswego.cs.dl.util.concurrent.Sync+.*(..)) &&
   //   call(* RWLock.startRead(..)) ||
   //      call(* RoutingStrategy+.*(..))
   (target(org.codehaus.wadi.RWLock$ReaderLock) ||
    target(org.codehaus.wadi.RWLock$WriterLock)) &&
   within(Tracing)		// this will never be true
   )
    && target(lock)
    && !within(Tracing);

  Object
    around(Object lock) : traceMethods(lock)
  {
    Signature sig = thisJoinPointStaticPart.getSignature();
    Object[] args = thisJoinPoint.getArgs();
//    String pkg=sig.getDeclaringType().getName();
    String name=sig.getName();
    String a="";
    for (int i=0;i<args.length;i++)
      a+=((a.length()==0)?"":",")+args[i];
    //    _log.warn("Entering "+this.hashCode()+"."+pkg+"."+name+"("+a+")", new Exception());
    Thread t=Thread.currentThread();
    //    _log.warn(t+": Entering "+this.hashCode()+"."+pkg+"."+name+"("+a+") "+this);
    _log.warn(t+": Entering "+lock.toString()+"."+name+"("+a+")", new Exception());

    Object tmp=proceed(lock);
    //    _log.warn(t+": Leaving "+this.hashCode()+"."+pkg+"."+name+"() "+this+" --> "+tmp);
    _log.warn(t+": Leaving "+lock.toString()+"."+name+"("+a+")"+" --> "+tmp);

    return tmp;
  }
}
