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
package org.codehaus.wadi.location.session;

import java.io.Serializable;

import org.codehaus.wadi.core.motable.Motable;

/**
 * A request for the emigration of the enclosed session - The response
 * should be a ReleaseEntryResponse object sent whence this request arrived.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class ReleaseEntryRequest implements SessionRequestMessage, Serializable {
	protected final Motable _motable;
    private int version;
    private int numberOfExpectedMerge;

	public ReleaseEntryRequest(Motable motable) {
		_motable=motable;
	}

	public Motable getMotable() {
		return _motable;
	}

    public Object getKey() {
        return _motable.getName();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getNumberOfExpectedMerge() {
        return numberOfExpectedMerge;
    }

    public void setNumberOfExpectedMerge(int numberOfExpectedMerge) {
        this.numberOfExpectedMerge = numberOfExpectedMerge;
    }

    public SessionResponseMessage newResponseFailure() {
        return new ReleaseEntryResponse(false);
    }
    
    public String toString() {
        return "<ReleaseEntryRequest: " + _motable.getName() + ">";
    }

}
