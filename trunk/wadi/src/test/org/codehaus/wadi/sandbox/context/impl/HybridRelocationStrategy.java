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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.RelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Combine various RelocationStrategies to produce a cleverer one
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class HybridRelocationStrategy implements RelocationStrategy {

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.RelocationStrategy#relocate(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Promoter, EDU.oswego.cs.dl.util.concurrent.Sync, java.util.Map)
	 */
	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres,
			FilterChain chain, String id, Immoter immoter,
			Sync promotionLock, Map locationMap) throws IOException,
			ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setTop(Contextualiser top){}
	public Contextualiser getTop(){return null;}
}
