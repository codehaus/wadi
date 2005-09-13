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
package org.codehaus.wadi.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.WritableByteChannel;

import org.codehaus.wadi.Config;
import org.codehaus.wadi.Contextualiser;

public interface PeerConfig extends Config/*, StreamConnection*/, WritableByteChannel {

    public void close() throws IOException; // inherited from WriteableByteChannel - but overloaded to mean close whole Connection...
    Contextualiser getContextualiser();
    String getNodeId();
    //InputStream getInputStream() throws IOException;
    //OutputStream getOutputStream() throws IOException;
    ObjectInputStream getObjectInputStream() throws IOException;
    ObjectOutputStream getObjectOutputStream() throws IOException;
    
}
