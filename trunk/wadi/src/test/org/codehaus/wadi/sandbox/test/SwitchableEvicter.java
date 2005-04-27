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
package org.codehaus.wadi.sandbox.test;

import org.codehaus.wadi.sandbox.Evictable;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.EvicterConfig;

// FIXME - this needs redoing ...

/**
 * An Evicter which can be externally switched between Always and Never style behaviour
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SwitchableEvicter implements Evicter {

	protected boolean _switch=true;
	public boolean getSwitch(){return _switch;}
	public void setSwitch(boolean shwitch){_switch=shwitch;}

	public boolean evict(String id, Evictable evictable) {return evict(id, evictable, 0);}
	public boolean evict(String id, Evictable evictable, long time) {return _switch;}
    
    public void setLastAccessedTime(Evictable evictable, long time) {/* do nothing */}
    public void setMaxInactiveInterval(Evictable evictable, int interval)  {/* do nothing */}

    public void init(EvicterConfig config) {/* do nothing */}
    public void destroy() {/* do nothing */}
    
    // Lifecycle
    
    public void start() throws Exception {/* do nothing */}
    public void stop() throws Exception {/* do nothing */}
}
