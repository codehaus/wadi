/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi;


public interface Manager {
    void init(ManagerConfig config);

    void start() throws Exception;

    void stop() throws Exception;

    void destroy();

    Session create();

    void destroy(Session session);

    Manager getManager();

    SessionWrapperFactory getSessionWrapperFactory();

    SessionIdFactory getSessionIdFactory();

    int getMaxInactiveInterval();

    void setMaxInactiveInterval(int interval);

    void setLastAccessedTime(Evictable evictable, long oldTime, long newTime);

    void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval);

    SessionPool getSessionPool();

    boolean getErrorIfSessionNotAcquired();

    void around(Invocation invocation) throws InvocationException;
}