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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.SimpleType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: SimpleTypeSerializer</p>
 * <p>Description: Serializer for {@link SimpleType} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.SimpleTypeSerializer</code></p>
 */

public class SimpleTypeSerializer extends Serializer<SimpleType> {
	public static final Map<Byte, SimpleType> TYPES;
	public static final Map<SimpleType, Byte> INDEXES;
	
	static {
		try {
			Map<Byte, SimpleType> tmp = new HashMap<Byte, SimpleType>(SimpleType.ALLOWED_CLASSNAMES_LIST.size());
			Map<SimpleType, Byte> tmp2 = new HashMap<SimpleType, Byte>(SimpleType.ALLOWED_CLASSNAMES_LIST.size());
			byte cnt = 0;
			for(Field f: SimpleType.class.getDeclaredFields()) {
				if(f.getType().equals(SimpleType.class)) {
					SimpleType st = (SimpleType)f.get(null);
					tmp.put(cnt, st);
					tmp2.put(st, cnt);
					cnt++;
				}
			}
			TYPES = Collections.unmodifiableMap(tmp);
			INDEXES = Collections.unmodifiableMap(tmp2);
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
	}
	
	
	@Override
	public void write(Kryo kryo, Output output, SimpleType st) {
		output.writeByte(INDEXES.get(st));		
	}

	@Override
	public SimpleType read(Kryo kryo, Input input, Class<SimpleType> type) {
		return TYPES.get(input.readByte());
	}

}
