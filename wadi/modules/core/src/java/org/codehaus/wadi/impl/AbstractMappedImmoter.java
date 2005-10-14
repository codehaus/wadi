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

import java.util.Map;

import org.codehaus.wadi.Motable;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractMappedImmoter extends AbstractImmoter {
    
    protected final Map _map;
    
    public AbstractMappedImmoter(Map map) {
        super();
        _map=map;    
    }
    
	public boolean prepare(String name, Motable emotable, Motable immotable) {
		if (!super.prepare(name, emotable, immotable))
			return false;
		
		_map.put(name, immotable); // TODO - map locking ?
		return true;
	}

	public void rollback(String name, Motable immotable) {
		_map.remove(name);
		super.rollback(name, immotable);
	}
}
