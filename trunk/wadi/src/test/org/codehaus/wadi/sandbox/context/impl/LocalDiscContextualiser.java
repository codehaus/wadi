/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Emoter;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.Motable;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Maps id:File where file contains Context content...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LocalDiscContextualiser extends AbstractMappedContextualiser {
	protected static final Log _log = LogFactory.getLog(LocalDiscContextualiser.class);
	
	protected final StreamingStrategy _streamer;
	protected final File _dir;
	protected final Immoter _immoter;
	protected final Emoter _emoter;
	
	public LocalDiscContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, StreamingStrategy streamer, File dir) {
		super(next, collapser, map, evicter);
		_streamer=streamer;
	    assert dir.exists();
	    assert dir.isDirectory();
	    assert dir.canRead();
	    assert dir.canWrite();
		_dir=dir;
		
		_immoter=new LocalDiscImmoter();
		_emoter=new MappedEmoter(_map) {public String getInfo(){return "local disc";}}; // overwrite - yeugh ! - fix when we have a LifeCycle
	}

	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Sync promotionLock, Motable motable) throws IOException, ServletException {
		// TODO - it should be possible to load a Session off disc, use it, then write it back - here...
		throw new RuntimeException("NYI");
	}
	
	public boolean isLocal(){return true;}

	public Immoter getPromoter(Immoter immoter) {
		// do not check to see whether we should retain this Motable
		// just pass straight through to the COntextualiser above us
		return immoter;
	} 
	
	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}
	
	/**
	 * An Immoter that deals in terms of LocalDiscMotables
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	public class LocalDiscImmoter extends AbstractImmoter {	
		
		public Motable nextMotable(String id, Motable emotable) {
			LocalDiscMotable ldm=new LocalDiscMotable();
			ldm.setFile(new File(_dir, id+"."+_streamer.getSuffix()));
			return ldm;
		}
		
		public void commit(String id, Motable immotable) {
			synchronized (_map){_map.put(id, immotable);}
		}
		
		public void contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable) throws IOException, ServletException {
			// TODO - we need to pass through a promotionLock
			contextualiseLocally(hreq, hres, chain, id, new NullSync(), immotable);
		}
		
		public String getInfo() {
			return "local disc";
		}
	}
}
