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

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractDelegatingContextualiser extends AbstractChainedContextualiser {

    public AbstractDelegatingContextualiser(Contextualiser next) {
        super(next);
    }
    
    public boolean isExclusive() {return _next.isExclusive();}

    public Immoter getDemoter(String name, Motable motable) {return _next.getDemoter(name, motable);}
    
    public Immoter getSharedDemoter() {return _next.getSharedDemoter();}

    public void promoteToExclusive(Immoter immoter) {_next.promoteToExclusive(immoter);}
    
    public void load(Emoter emoter, Immoter immoter) {_next.load(emoter, immoter);}
    
}
