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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Emoter;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.Motable;

/**
 * A collection of useful static functions
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Utils {
	protected static final Log _log=LogFactory.getLog(Utils.class);
	
	public static Motable mote(Emoter emoter, Immoter immoter, Motable emotable, String id) {
		Motable immotable=immoter.nextMotable(id, emotable);
		if (immoter.prepare(id, emotable, immotable) && emoter.prepare(id, emotable, immotable)) {
			immoter.commit(id, immotable);
			emoter.commit(id, emotable);
			_log.info("sucessful motion: "+id+" : "+emoter.getInfo()+" -> "+immoter.getInfo());
			return immotable;
		} else {
			emoter.rollback(id, emotable);
			immoter.rollback(id, immotable);
			_log.warn("unsucessful motion: "+id+" : "+emoter.getInfo()+" -> "+immoter.getInfo());
			return null;
		}
	}
}
