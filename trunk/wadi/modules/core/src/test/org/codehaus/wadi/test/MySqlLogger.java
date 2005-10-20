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

package org.codehaus.wadi.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MySqlLogger implements com.mysql.jdbc.log.Log {
	
	protected final Log _log;
	
	public MySqlLogger(String name) {
		_log=LogFactory.getLog(name);
	}
	
	public boolean isDebugEnabled() {
		return _log.isDebugEnabled();
	}

	public boolean isErrorEnabled() {
		return _log.isErrorEnabled();
	}

	public boolean isFatalEnabled() {
		return _log.isFatalEnabled();
	}

	public boolean isInfoEnabled() {
		return _log.isInfoEnabled();
	}

	public boolean isTraceEnabled() {
		return _log.isTraceEnabled();
	}

	public boolean isWarnEnabled() {
		return _log.isWarnEnabled();
	}

	public void logDebug(Object msg) {
		_log.debug(msg);
	}

	public void logDebug(Object msg, Throwable thrown) {
		_log.debug(msg, thrown);
	}

	public void logError(Object msg) {
		_log.error(msg);
	}

	public void logError(Object msg, Throwable thrown) {
		_log.error(msg, thrown);
	}

	public void logFatal(Object msg) {
		_log.fatal(msg);
	}

	public void logFatal(Object msg, Throwable thrown) {
		_log.fatal(msg, thrown);
	}

	public void logInfo(Object msg) {
		_log.info(msg);
	}

	public void logInfo(Object msg, Throwable thrown) {
		_log.info(msg, thrown);
	}

	public void logTrace(Object msg) {
		_log.trace(msg);
	}

	public void logTrace(Object msg, Throwable thrown) {
		_log.trace(msg, thrown);
	}

	public void logWarn(Object msg) {
		_log.warn(msg);		
	}

	public void logWarn(Object msg, Throwable thrown) {
		_log.warn(msg, thrown);
	}
	
}