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
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.codehaus.wadi.Streamer;

/**
 * Don't do anything extra, just connect streams up directly.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SimpleStreamer implements Streamer {
    
    private final ClassLoader classLoader;

    public SimpleStreamer() {
        this(Thread.currentThread().getContextClassLoader());
    }

	public SimpleStreamer(ClassLoader classLoader) {
        if (null == classLoader) {
            throw new IllegalArgumentException("classLoader is required");
        }
        this.classLoader = classLoader;
	}
	
	public ObjectInput getInputStream(InputStream is) throws IOException {
        return new ObjectInputStream(is, classLoader);
    }
    
    public ObjectOutput getOutputStream(OutputStream os) throws IOException {
        return new ObjectOutputStream(os);
    }
    
    public String getSuffix() {
        return "ser";
    }
    
    public String getSuffixWithDot() {
        return ".ser";
    }
    
}

