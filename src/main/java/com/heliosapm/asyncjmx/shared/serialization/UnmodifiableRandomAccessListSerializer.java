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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: UnmodifiableRandomAccessListSerializer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.UnmodifiableRandomAccessListSerializer</code></p>
 */

public class UnmodifiableRandomAccessListSerializer extends Serializer<List<?>> {

	@Override
	public void write(Kryo kryo, Output output, List<?> object) {
		if(object.isEmpty()) {
			output.write(0);
		} else {
			output.write(object.size());
			for(Object o: object) {
				kryo.writeClassAndObject(output, o);
			}
		}
	}

	@Override
	public List<Object> read(Kryo kryo, Input input, Class<List<?>> type) {
		int size = input.readInt();
		List<Object> list = new ArrayList<Object>(size);
		for(int i = 0; i < size; i++) {
			list.add(kryo.readClassAndObject(input));
		}
		try {
			Constructor<?> ctor = type.getDeclaredConstructor(List.class);
			ctor.setAccessible(true);
			return (List<Object>)ctor.newInstance(list);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}

}
