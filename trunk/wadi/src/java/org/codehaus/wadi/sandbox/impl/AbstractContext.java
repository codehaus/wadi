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
package org.codehaus.wadi.sandbox.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.Context;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractContext extends AbstractMotable implements Context {

    protected static Log _log = LogFactory.getLog(AbstractContext.class);

    protected RWLock _lock;
	public Sync getSharedLock(){return _lock.readLock();}
	public Sync getExclusiveLock(){return _lock.writeLock();}

	public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, boolean invalidated, String id, RWLock lock) {
	    init(creationTime, lastAccessedTime, maxInactiveInterval, invalidated);
	    _lock=lock;
	}
	
	public void destroy() {
	    super.destroy();
	    _lock=null;
	}

	// Motable
	public byte[] getBytes() throws Exception {
        return Utils.getContent(this, new SimpleStreamingStrategy());
	}

	public void setBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		Utils.setContent(this, bytes, new SimpleStreamingStrategy());
	}
	
}
