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
package org.codehaus.wadi.core.session;

import org.codehaus.wadi.Session;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.web.AttributesFactory;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1885 $
 */
public class DistributableSessionFactory extends StandardSessionFactory {

    protected final Streamer streamer;

    public DistributableSessionFactory(AttributesFactory attributesFactory, Streamer streamer) {
        super(attributesFactory);
        if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        }
        this.streamer = streamer;
    }

    public Session create() {
        return new DistributableSession(newAttributes(), getManager(), streamer);
    }
    
}
