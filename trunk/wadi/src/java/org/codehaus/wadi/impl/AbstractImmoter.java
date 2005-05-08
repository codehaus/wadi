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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Abstract base for Immoters
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractImmoter implements Immoter {
	protected static final Log _log = LogFactory.getLog(AbstractImmoter.class);

	public boolean prepare(String id, Motable emotable, Motable immotable) {
		try {
			immotable.copy(emotable);
			return true;
		} catch (Exception e) {
			if (_log.isWarnEnabled()) _log.warn("problem during insertion: "+id, e);
			return false;
		}
	}

	public void commit(String id, Motable immotable) {
	    // This method is provided as a placeholder. Subclasses can call super.rollback().
	    // If we want to add anything here later, we can.
	    // It is NOT intended that this form some sort of default behaviour !
	}

	public void rollback(String id, Motable immotable) {
		try {
			immotable.destroy();
		} catch (Exception e) {
			if (_log.isErrorEnabled()) _log.error("problem rolling back insertion: "+id, e);
		}
	}

	// keep the throws clause - we are defining a method signature for our subtypes
	public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable, Sync motionLock) throws IOException, ServletException {
	// most Contextualisers cannot contextualise locally...
        return false;
	}
}
