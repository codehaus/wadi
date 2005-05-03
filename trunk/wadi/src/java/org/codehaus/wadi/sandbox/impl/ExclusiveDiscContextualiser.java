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
public class ExclusiveDiscContextualiser extends AbstractExclusiveContextualiser {

	protected final StreamingStrategy _streamer;
	protected final File _dir;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public ExclusiveDiscContextualiser(Contextualiser next, Collapser collapser, Evicter evicter, Map map, StreamingStrategy streamer, File dir) {
	    super(next, new CollapsingLocker(collapser), evicter, map);
	    _streamer=streamer;
	    assert dir.exists();
	    assert dir.isDirectory();
	    assert dir.canRead();
	    assert dir.canWrite();
	    _dir=dir;

	    _immoter=new ExclusiveDiscImmoter(_map);
	    _emoter=new ExclusiveDiscEmoter(_map);
	}

	public boolean isExclusive(){return true;}

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	/**
	 * An Immoter that deals in terms of ExclusiveDiscMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class ExclusiveDiscImmoter extends AbstractMappedImmoter {

	    public ExclusiveDiscImmoter(Map map) {
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
	class ExclusiveDiscEmoter extends AbstractMappedEmoter {

		public ExclusiveDiscEmoter(Map map) {super(map);}

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
	}

    protected void load() {
        // if our last incarnation suffered a catastrophic failure there may be some sessions
        // in our directory - FIXME - if replicating, we may not want to reload these...
        boolean accessOnLoad=_config.getAccessOnLoad();
        long time=System.currentTimeMillis();
        String[] list=_dir.list();
        int l=list.length;
        int suffixLength=".".length()+_streamer.getSuffix().length();
        for (int i=0; i<l; i++) {
            String name=list[i];
            String id=name.substring(0, name.length()-suffixLength);
            ExclusiveDiscMotable motable=new ExclusiveDiscMotable();
            motable.setFile(new File(name));
            if (accessOnLoad)
                motable.setLastAccessedTime(time);
            else {
                if (motable.getTimedOut()) {
                    _log.warn("LOADED DEAD SESSION: "+motable.getId());
                    // TODO - something cleverer...
                }
            }
            _map.put(id, motable);
        }
	_log.info("loaded sessions: "+list.length);
    }

    public void start() throws Exception {
        load();
        super.start(); // continue down chain...
    }

    // this should move up.....
    public void expire(Motable motable) {
        // decide whether session needs promotion
        boolean needsLoading=true; // FIXME
        // if so promote to top and expire there
        String id=motable.getId();
        _log.trace("expiring from disc: "+id);
        if (needsLoading) {
            _map.remove(id);
            Motable loaded=_config.getSessionPool().take();
            try {
                loaded.copy(motable);
                motable=null;
                _config.expire(loaded);
            } catch (Exception e) {
                _log.error("unexpected problem expiring from disc", e);
            }
            loaded=null;
        } else {
            // else, just drop it off the disc here...
            throw new UnsupportedOperationException(); // FIXME
        }
    }

}
