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
package org.codehaus.wadi.sandbox.impl;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

// TODO - a JDBC-based equivalent

/**
 * Maps id:File where file contains Context content...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ExclusiveDiscContextualiser extends AbstractCollapsingContextualiser {
	protected static final Log _log = LogFactory.getLog(ExclusiveDiscContextualiser.class);

	protected final StreamingStrategy _streamer;
	protected final File _dir;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public ExclusiveDiscContextualiser(Contextualiser next, Evicter evicter, Map map, Collapser collapser, StreamingStrategy streamer, File dir) {
	    super(next, evicter, map, collapser);
	    _streamer=streamer;
	    assert dir.exists();
	    assert dir.isDirectory();
	    assert dir.canRead();
	    assert dir.canWrite();
	    _dir=dir;
	    
	    _immoter=new LocalDiscImmoter(_map);
	    _emoter=new LocalDiscEmoter(_map);
	}
	
	public boolean isLocal(){return true;}

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	/**
	 * An Immoter that deals in terms of ExclusiveDiscMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class LocalDiscImmoter extends AbstractMappedImmoter {

	    public LocalDiscImmoter(Map map) {
	        super(map);
	    }
	    
		public Motable nextMotable(String id, Motable emotable) {
			ExclusiveDiscMotable ldm=new ExclusiveDiscMotable();
			ldm.setId(id);
			return ldm;
		}

		public boolean prepare(String id, Motable emotable, Motable immotable) {
			((ExclusiveDiscMotable)immotable).setFile(new File(_dir, id+"."+_streamer.getSuffix()));
			return super.prepare(id, emotable, immotable);
		}

		public String getInfo() {
			return "exclusive disc";
		}
	}

	/**
	 * An Emmoter that deals in terms of ExclusiveDiscMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	class LocalDiscEmoter extends AbstractMappedEmoter {

		public LocalDiscEmoter(Map map) {super(map);}

		public boolean prepare(String id, Motable emotable, Motable immotable) {
			if (super.prepare(id, emotable, immotable)) {
				try {
					ExclusiveDiscMotable ldm=(ExclusiveDiscMotable)emotable;
					ldm.setFile(new File(_dir, id+"."+_streamer.getSuffix()));
				if (ExclusiveDiscMotable.load(ldm.getFile(), emotable)==null)
					return false;
				} catch (Exception e) {
					_log.error("could not load item from file", e);
					return false;
				}
			} else
				return false;

			return true;
		}

		public String getInfo(){return "exclusive disc";}
	};
}
