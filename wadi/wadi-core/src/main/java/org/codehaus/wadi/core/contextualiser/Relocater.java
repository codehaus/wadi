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
package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.motable.Immoter;


/**
 * Abstracts out a strategy for either request or state relocation. This is necessary to
 * ensure that a request is processed in the same node as its state.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Relocater {
    /**
	 * Either relocate the request to the session by proxying/redirection, or the session to the request, by migration...
     * @param invocation
	 * @param id
	 * @param immoter
	 * @param b 
	 * @return - whether, or not, the request was contextualised
	 */
    boolean relocate(Invocation invocation, Object id, Immoter immoter, boolean shuttingDown) throws InvocationException;
}
