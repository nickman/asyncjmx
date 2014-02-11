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

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.serialization.NonSerializable.NonSerializableSerializer;

/**
 * <p>Title: NonSerializable</p>
 * <p>Description: Class to wrap objects that failed serialization</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.NonSerializable</code></p>
 */
@DefaultSerializer(NonSerializableSerializer.class)
public class NonSerializable implements PlaceHolder {
	/** The class name of the failed object */
	protected final String className;
	/** The {@link Object#toString()} of the failed object */
	protected final String strObject;
	
	
	/**
	 * Creates a new NonSerializable
	 * @param className The class name of the failed object
	 * @param strObject The {@link Object#toString()} of the failed object 
	 */
	public NonSerializable(String className, String strObject) {
		super();
		this.className = className;
		this.strObject = strObject;
	}
	
	/**
	 * Creates a new NonSerializable
	 * @param failed The object that failed serialization
	 */
	public NonSerializable(Object failed) {
		this(failed.getClass().getName(), failed.toString());
	}

	/**
	 * Returns the class name of the failed object
	 * @return the class name of the failed object
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns the {@link Object#toString()} of the failed object 
	 * @return the {@link Object#toString()} of the failed object
	 */
	public String getStrObject() {
		return strObject;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuilder("[").append(className).append("]:").append(strObject).toString();
	}
	
	/**
	 * <p>Title: NonSerializableSerializer</p>
	 * <p>Description: Kryo serializer for {@link NonSerializable} instances.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.asyncjmx.shared.serialization.NonSerializable.NonSerializableSerializer</code></p>
	 */
	public static class NonSerializableSerializer extends Serializer<NonSerializable> {

		/**
		 * {@inheritDoc}
		 * @see com.esotericsoftware.kryo.Serializer#write(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Output, java.lang.Object)
		 */
		@Override
		public void write(Kryo kryo, Output output, NonSerializable object) {
			output.writeString(object.className);
			output.writeString(object.strObject);			
		}

		/**
		 * {@inheritDoc}
		 * @see com.esotericsoftware.kryo.Serializer#read(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Input, java.lang.Class)
		 */
		@Override
		public NonSerializable read(Kryo kryo, Input input, Class<NonSerializable> type) {
			return new NonSerializable(input.readString(), input.readString());
		}

		
	}
	
}
