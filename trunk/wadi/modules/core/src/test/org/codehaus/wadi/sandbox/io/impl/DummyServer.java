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
package org.codehaus.wadi.sandbox.io.impl;

import org.codehaus.wadi.sandbox.io.Pipe;
import org.codehaus.wadi.sandbox.io.Server;
import org.codehaus.wadi.sandbox.io.ServerConfig;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyServer implements Server {

    public DummyServer() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void init(ServerConfig config) {
        // do nothing
    }

    public void start() throws Exception {
        // TODO Auto-generated method stub

    }

    public void stop() throws Exception {
        // TODO Auto-generated method stub

    }

    public void waitForExistingPipes() {
        // TODO Auto-generated method stub

    }

    public void stopAcceptingPipes() {
        // TODO Auto-generated method stub

    }

    public void run(Pipe pipe) {
        // TODO Auto-generated method stub

    }

}
