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
package org.codehaus.wadi.sandbox.context.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;


/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractMappedContextualiser extends
		AbstractChainedContextualiser {

	protected final Log _log = LogFactory.getLog(getClass());
	protected final Map _map;
	protected final Evicter _evicter;

	/**
	 * @param next
	 */
	public AbstractMappedContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter) {
		super(next, collapser);
		_map=map;
		_evicter=evicter;
	}

	protected String _stringPrefix="<"+getClass().getName()+":";
	protected String _stringSuffix=">";
	public String toString() {
		return new StringBuffer(_stringPrefix).append(_map.size()).append(_stringSuffix).toString();
	}
}
