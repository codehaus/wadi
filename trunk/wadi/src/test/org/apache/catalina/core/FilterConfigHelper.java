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
package org.apache.catalina.core;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Has it really come to this...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class FilterConfigHelper {
	protected static final Log _log=LogFactory.getLog(FilterConfigHelper.class);
	
	public static Filter getFilter(FilterConfig config) {
		Filter filter=null;
		try {
			filter=((ApplicationFilterConfig)config).getFilter();
		} catch (Exception e) {
			_log.error(e);
		}
		return filter;
	}
}
