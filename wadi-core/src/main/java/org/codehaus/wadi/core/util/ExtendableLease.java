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
package org.codehaus.wadi.core.util;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ExtendableLease extends SimpleLease {

    public interface Extender {
    	public boolean extend();
    }

    protected static final Extender _DefaultExtender=new Extender(){public boolean extend(){return false;}};
    
	protected Extender _extender=_DefaultExtender;
	protected long _leasePeriod;
	
    public ExtendableLease(String label, Sync sync) {
    	super(label, sync);
    }
    
    public Handle acquire(long leasePeriod, Extender extender) throws InterruptedException {
    	Handle handle=super.acquire(leasePeriod);
    	_extender=extender; // TODO - needs some form of atomicity
    	return handle;
    }

    public Handle attempt(long timeframe, long leasePeriod, Extender extender) throws InterruptedException {
    	Handle handle=super.attempt(timeframe, leasePeriod);
    	_extender=extender;
    	return handle;
    }

    // Use an ExtendableReleaser here
    protected Handle setAlarm(long leasePeriod) {
    	_leasePeriod=leasePeriod;
        Releaser releaser=new ExtendableReleaser();
        Handle handle=new SimpleHandle(_daemon.executeAfterDelay(leasePeriod, releaser));
        if (_lockLog.isTraceEnabled()) _lockLog.trace(_label+" - acquisition: "+this+"."+handle);
        synchronized (_handles) {_handles.add(handle);}
        releaser.init(handle);
        return handle;
    }
    
    public class ExtendableReleaser extends Releaser {

        public void run() {
        	if (_extender.extend()) {
        		// set up another lease...
        		setAlarm(_leasePeriod);
        	} else {
        		// release lock
        		super.run();
        	}
        }
    }

}
