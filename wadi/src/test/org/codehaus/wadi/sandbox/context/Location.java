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
package org.codehaus.wadi.sandbox.context;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Location extends Motable, Serializable {
	/**
	 * @param req - the request object from the webcontainer
	 * @param res - the response object from the webcontainer
	 * @param promotionLock - used to collapse concurrent threads seeking same context into one stack descent
	 * @return - null if no migration has occurred (in which case the promotionLock has been released), or an immigrating Context (in which case the promotionLock should be released as soon as it has been promoted).
	 * @throws IOException TODO
	 */
	public boolean proxy(HttpServletRequest req, HttpServletResponse res, String id, Sync promotionLock) throws IOException;
}
