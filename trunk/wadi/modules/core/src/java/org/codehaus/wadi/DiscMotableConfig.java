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
package org.codehaus.wadi;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface DiscMotableConfig extends StoreMotableConfig {

    File getDirectory();
    String getSuffix();

    ByteBuffer take(int size);
    void put(ByteBuffer buffer);

    boolean getReusingStore();

//    void insert(File dir, Motable motable, Object body) throws Exception;
//    void delete(File dir, Motable motable); // TODO - why no Exception ?
//    void update(File dir, Motable motable, Object body) throws Exception;
//	long loadHeader(File dir, Motable motable); // TODO - why no Exception ?
//	Object loadBody(File dir, Motable motable) throws Exception;

}
