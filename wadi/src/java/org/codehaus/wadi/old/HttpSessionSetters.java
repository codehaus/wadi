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
package org.codehaus.wadi.old;

/**
 * Useful interface to mixin and act as an AspectJ hook.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public interface
HttpSessionSetters
{
    public void               setLastAccessedTime(long l);
    public void               setMaxInactiveInterval(int i);
    public Object             setAttribute(String key, Object val, boolean returnVal);
    public Object             removeAttribute(String key, boolean returnVal);
    //  public void               invalidate();
}
