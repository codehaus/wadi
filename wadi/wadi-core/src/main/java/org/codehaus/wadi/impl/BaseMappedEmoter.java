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

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.motable.Motable;


/**
 * A basic Emoter for MappedContextualisers
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class BaseMappedEmoter extends AbstractChainedEmoter {
	private final ConcurrentMotableMap map;

	public BaseMappedEmoter(ConcurrentMotableMap map) {
        if (null == map) {
            throw new IllegalArgumentException("map is required");
        }
        this.map = map;
    }

    public boolean emote(Motable emotable, Motable immotable) {
        if (super.emote(emotable, immotable)) {
            map.remove(immotable.getName());
            return true;
        } else {
            return false;
        }
    }
    
}
