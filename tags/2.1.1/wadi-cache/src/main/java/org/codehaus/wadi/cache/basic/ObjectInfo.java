/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.cache.basic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.codehaus.wadi.core.util.Streamer;


/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectInfo implements Serializable {
    private boolean undefined;
    private Object object;
    private int version;
    
    public ObjectInfo() {
        version = 0;
        undefined = true;
    }

    public ObjectInfo(Object object) {
        if (null == object) {
            throw new IllegalArgumentException("object is required");
        }
        this.object = object;
        
        version = 0;
        undefined = false;
    }
    
    public ObjectInfo(int version, Object object) {
        if (0 > version) {
            throw new IllegalArgumentException("version is lower than 0");
        } else if (null == object) {
            throw new IllegalArgumentException("object is required");
        }
        this.version = version;
        this.object = object;
        
        undefined = false;
    }
    
    public ObjectInfo incrementVersion(Streamer streamer) {
        if (undefined) {
            return this;
        }

        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        try {
            ObjectOutput os = streamer.getOutputStream(memOut);
            os.writeObject(object);
            os.close();
            
            ObjectInput is = streamer.getInputStream(new ByteArrayInputStream(memOut.toByteArray()));
            Object object = is.readObject();
            return new ObjectInfo(version + 1, object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getVersion() {
        return version;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object newValue) {
        if (null == newValue) {
            throw new IllegalArgumentException("newValue is required");
        }
        object =  newValue;
        undefined = false;
    }

    public boolean canMerge(ObjectInfo objectInfo) {
        return objectInfo.version == version + 1;
    }

    public void merge(ObjectInfo newObjectInfo) {
        if (!canMerge(newObjectInfo)) {
            throw new IllegalArgumentException("cannot merge");
        }
        version = newObjectInfo.version;
        object = newObjectInfo.object;
    }

    public boolean isUndefined() {
        return undefined;
    }

}