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
package org.codehaus.wadi.sandbox.dindex;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BucketFacade extends AbstractBucket /* implements Excludable,*/ {
    
    protected static final Log _log = LogFactory.getLog(BucketFacade.class);
    
    protected long _timeStamp;
    protected Bucket _content;
    
    public BucketFacade(int key, long timeStamp, Bucket content) {
        super(key);
        _timeStamp=timeStamp;
        _content=content;
        _log.info("["+_key+"] initialising location to: "+_content);
    }
    
    public boolean isLocal() {
        return _content.isLocal();
    }
    
    // TODO - locking...
    public void setContent(long timeStamp, Bucket content) {
        if (timeStamp>_timeStamp) {
            _log.info("["+_key+"] changing location from: "+_content+" to: "+content);
            _timeStamp=timeStamp;
            _content=content;
        }
    }
    
    public Bucket getContent() {
        return _content;
    }
    
    public void setContentRemote(long timeStamp, Destination location) {
        if (timeStamp>_timeStamp) {
            _timeStamp=timeStamp;
            if (_content instanceof RemoteBucket) {
                ((RemoteBucket)_content).setLocation(location);
            } else {
                _log.info("["+_key+"] changing location from: local to: "+location);
                _content=new RemoteBucket(_key, location);
            }
        }
    }
    
}
