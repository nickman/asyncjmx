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

import java.util.Map;

import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: TabularDataSupportSerializer</p>
 * <p>Description: Serializer for {@link TabularDataSupport} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.TabularDataSupportSerializer</code></p>
 */

public class TabularDataSupportSerializer extends Serializer<TabularDataSupport> {

	@Override
	public void write(Kryo kryo, Output output, TabularDataSupport tds) {
		OpenTypeSerializer.otSerializer.write(kryo, output, tds.getTabularType());
		int size = tds.size();
		output.write(size);
		for(Map.Entry<Object, Object> entry: tds.entrySet()) {
			kryo.writeClassAndObject(output, entry.getKey());
			kryo.writeClassAndObject(output, entry.getValue());
		}		
	}

	@Override
	public TabularDataSupport read(Kryo kryo, Input input, Class<TabularDataSupport> type) {
		try {
			kryo.setReferences(true);
			input.readByte();
			TabularType ttype = OpenTypeSerializer.tabSerializer.read(kryo, input, TabularType.class);
			int size = input.readInt();
			TabularDataSupport tds = new TabularDataSupport(ttype, size, 0.75f);
			for(int i = 0; i < size; i++) {
				Object key = kryo.readClassAndObject(input);
				Object value = kryo.readClassAndObject(input);
				tds.put(key, value);
			}
			return tds;
		} finally {
			kryo.setReferences(false);
		}
	}

}
