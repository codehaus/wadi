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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.EvictionPolicy;
import org.codehaus.wadi.HttpSessionImpl;

/**
 * Evict after an absolute time period. E.g. after 1 minute of inactivity.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class AbsoluteEvictionPolicy
  implements EvictionPolicy
{
  protected final Log _log=LogFactory.getLog(getClass());
  protected int _fencepost;

  public
    AbsoluteEvictionPolicy(int fencepost) // secs
  {
    _fencepost=fencepost;
  }

  public boolean
    evictable(long currentTimeMillis, HttpSessionImpl impl)
  {
    long lat=impl.getLastAccessedTime();
    long age=currentTimeMillis-lat;
    long mii=impl.getMaxInactiveInterval()*1000;
    long fencepost=_fencepost*1000;
    boolean answer=(age>fencepost) && (age<mii);
    if (_log.isTraceEnabled()) _log.trace(impl.getRealId()+" : eviction policy: "+fencepost+"<"+age+"<"+mii+"="+answer);
    return answer;
  }
}
