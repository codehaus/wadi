/* Copyright 2003-2005 Core Developers Network Ltd.
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

package org.codehaus.wadi.jboss4;

import org.apache.commons.logging.Log;
import org.jboss.logging.Logger;

/**
 * Commons Logging -> JBoss Logging bridge. I wrote it to ensure that commons-logging:trace went to
 * jboss-logging:trace.
 *
 * It took ages to figure out :
 *
 * In your $JBOSS_HOME/server/<config>/conf/log4j.xml:
 *
 *  1) remove the "Threshold" param to your target Appender
 *
 *  2) try e.g. <category name="org.codehaus.wadi"><priority value="TRACE" class="org.jboss.logging.XLevel"/></category>
 *
 * and you should see lots of TRACE statements when WADI runs (slowly)
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 */
public class JBossLog implements Log {

	protected final Logger _log;

	public JBossLog(String name) {
		_log=Logger.getLogger(name);
	}

	public boolean isDebugEnabled() {
		return _log.isDebugEnabled();
	}

	public boolean isErrorEnabled() {
		return true;
	}

	public boolean isFatalEnabled() {
		return true;
	}

	public boolean isInfoEnabled() {
		return _log.isInfoEnabled();
	}

	public boolean isTraceEnabled() {
		return _log.isTraceEnabled();
	}

	public boolean isWarnEnabled() {
		return true;
	}

	public void trace(Object arg0) {
		_log.trace(arg0);
	}

	public void trace(Object arg0, Throwable arg1) {
		_log.trace(arg0, arg1);
	}

	public void debug(Object arg0) {
		_log.debug(arg0);
	}

	public void debug(Object arg0, Throwable arg1) {
		_log.debug(arg0, arg1);
	}

	public void info(Object arg0) {
		_log.info(arg0);
	}

	public void info(Object arg0, Throwable arg1) {
		_log.info(arg0, arg1);
	}

	public void warn(Object arg0) {
		_log.warn(arg0);
	}

	public void warn(Object arg0, Throwable arg1) {
		_log.warn(arg0, arg1);
	}

	public void error(Object arg0) {
		_log.error(arg0);
	}

	public void error(Object arg0, Throwable arg1) {
		_log.error(arg0, arg1);
	}

	public void fatal(Object arg0) {
		_log.fatal(arg0);
	}

	public void fatal(Object arg0, Throwable arg1) {
		_log.fatal(arg0, arg1);
	}

}
