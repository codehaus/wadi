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
package org.codehaus.wadi.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;


/**
 * A collection of useful static functions
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Utils {
	private static final Log LOG = LogFactory.getLog(Utils.class);

	/**
	 * Mote (in other words - move) the data held in a Motable from one Contextualiser to another, such
	 * that if the two Contextualisers store Motables in a persistant fashion, the data is never
	 * present in less than one of the two.
	 *
	 * @param emoter - delegate for the source Contextualiser
	 * @param immoter - delegate for the target Contextualiser
	 * @param emotable - data to be moved
	 * @return - the resulting immotable - in other words - the data's new representation in the target Contextualiser
	 */
	public static Motable mote(Emoter emoter, Immoter immoter, Motable emotable) {
        Motable immotable = immoter.newMotable(emotable);
        boolean success = emoter.emote(emotable, immotable);
        if (!success) {
            return null;
        }
        success = immoter.immote(emotable, immotable);
        if (!success) {
            return null;
        }
        return immotable;
    }

    public static byte[] getContent(Externalizable object, Streamer streamer) throws IOException {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ObjectOutput oo=streamer.getOutputStream(baos);
        object.writeExternal(oo);
        oo.close();
        return baos.toByteArray();
    }

    public static Externalizable setContent(Externalizable object, byte[] content, Streamer streamer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais=new ByteArrayInputStream(content);
        ObjectInput oi=streamer.getInputStream(bais);
        object.readExternal(oi);
        oi.close();
        return object;
    }

}
