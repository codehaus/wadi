/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.group.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @version $Revision: 1603 $
 */
public class PooledExecutorAdapter implements ThreadPool {
    private final ThreadPoolExecutor pooledExecutor;
    
    public PooledExecutorAdapter(int minPoolSize) {
        pooledExecutor = new ThreadPoolExecutor(minPoolSize,
            Integer.MAX_VALUE,
            5000,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
            public synchronized Thread newThread(Runnable runnable) {
                return new Thread(runnable);
            }
        });
    }
    
    public void execute(Runnable runnable) throws InterruptedException {
        pooledExecutor.execute(runnable);
    }
}
