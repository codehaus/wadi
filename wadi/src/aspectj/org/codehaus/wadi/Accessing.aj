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

package org.codehaus.wadi;

// TODO - probably only distributable sessions..? depends on session
// container impl..

// This aspect simply reduces the granularity at which
// lastAccessedTime is stored. This, in turn, will have a significant
// effect on how frequently it needs to be updated. Frequency of
// update will impact heavily on e.g.in-vm replication or e.g. session
// storage mechanisms that order by session time to live.

// TODO - if access is not required, go around replicating aspect and
// set _lastAccessedTime directly, otherwise go through
// setLastAccessedTime()... - Think about it...

/**
 * Compresses last accessed time modifications to cut down on deltas
 * generated.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1.3 $
 */
public aspect
  Accessing
{
  //  private static final Log _log=LogFactory.getLog(Accessing.class);

  pointcut access(AbstractHttpSessionImpl ahsi, long time) :
    execution(void HttpSessionImpl.setLastAccessedTime(long)) && args(time) && target(ahsi);

  void
    around(AbstractHttpSessionImpl ahsi, long time)
    : access(ahsi, time)
    {
      // round the lat by some granularity and only continue call if
      // different from existing value... thus reducing number of
      // changes made to lat.

      time-=time%5000;		// TODO - parameterise.. - lat granularity is 5 secs
      if (ahsi.getLastAccessedTime()!=time)
	proceed(ahsi, time);
    }
}
