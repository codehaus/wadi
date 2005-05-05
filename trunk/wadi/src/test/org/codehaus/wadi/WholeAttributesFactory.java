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

import org.codehaus.wadi.Dirtier;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class WholeAttributesFactory implements AttributesFactory {

    protected final Dirtier _dirtier;
    protected final Streamer _streamer;
    protected final boolean _evictObjectRepASAP;
    protected final boolean _evictByteRepASAP;
    
    public WholeAttributesFactory(Dirtier dirtier, Streamer streamer, boolean evictObjectRepASAP, boolean evictByteRepASAP) {
        _dirtier=dirtier;
        _streamer=streamer;
        _evictObjectRepASAP=evictObjectRepASAP;
        _evictByteRepASAP=evictByteRepASAP;
    }

    public Attributes create(AttributesConfig config) {
        return new WholeAttributes(_dirtier, _streamer, _evictObjectRepASAP, _evictByteRepASAP); // FIXME
    }

}
