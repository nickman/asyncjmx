/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.asyncjmx.shared.serialization;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: CompositeDataSupportSerializer</p>
 * <p>Description: Serializer for {@link CompositeDataSupport} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.CompositeDataSupportSerializer</code></p>
 */

public class CompositeDataSupportSerializer extends BaseSerializer<CompositeDataSupport> {

	@Override
	protected void doWrite(Kryo kryo, Output output, CompositeDataSupport cds) {
		kryo.writeObject(output, cds.getCompositeType());
		Set<String> keys = cds.getCompositeType().keySet();
		for(String key: keys) {			
			kryo.writeClassAndObject(output, cds.get(key));
		}
	}

	@Override
	protected CompositeDataSupport doRead(Kryo kryo, Input input, Class<CompositeDataSupport> type) {
		CompositeType ct = kryo.readObject(input, CompositeType.class);
		Set<String> keys = ct.keySet();
		Map<String, Object> keyValues = new LinkedHashMap<String, Object>(keys.size());
		for(String key: keys) {
			keyValues.put(key, kryo.readClassAndObject(input));
		}
		try {
			return new CompositeDataSupport(ct, keyValues);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create CompositeDataSupport", ex);
		}
	}

}
