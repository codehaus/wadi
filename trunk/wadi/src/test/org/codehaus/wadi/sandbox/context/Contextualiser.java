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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Contextualising a request is realising/processing it within the correct Context, i.e. in the presence of the required HttpSession, if any.
 *
 * A Contextualiser can choose to either process the request within itself, or promote a Context to its caller, within which the request may be processed.
 * It should indicate to its caller, via return code, whether said processing has already been carried out or not.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Contextualiser {

	// I'd like to add Manager to param list, but it bloats dependency tree - can we get along without it ?
	// FilterChain.doFilter() throws IOException, ServletException...
	boolean contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Promoter promoter, Sync promotionMutex, boolean localOnly) throws IOException, ServletException;

	void evict();
	void demote(String key, Motable val);

	boolean isLocal();
}
