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
package com.heliosapm.asyncjmx.shared;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.JMXCallback.JMXCallbackSerializer;
import com.heliosapm.asyncjmx.shared.serialization.BaseSerializer;
import com.heliosapm.asyncjmx.shared.serialization.HistogramKeyProvider;


/**
 * <p>Title: JMXCallback</p>
 * <p>Description: Encapsulates a callback from a JMX server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXCallback</code></p>
 */
@DefaultSerializer(JMXCallbackSerializer.class)
public class JMXCallback implements HistogramKeyProvider<Class<?>> {
	/** The JMX callback type */
	protected final JMXResponseType typeCode;	
	/** The JMX callback payload */
	protected final Object callback;
	/** The target identifier */
	protected final int target;
	
	/**
	 * Creates a new JMXCallback
	 * @param typeCode The JMX callback type
	 * @param callback The JMX callback payload
	 * @param target The target identifier
	 */
	public JMXCallback(JMXResponseType typeCode, Object callback, int target) {
		this.typeCode = typeCode;
		this.callback = callback;
		this.target = target;
	}
	
	/**
	 * Returns the target identifier
	 * @return the target identifier
	 */
	public int getTarget() {
		return target;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.asyncjmx.shared.serialization.HistogramKeyProvider#getHistogramKey()
	 */
	@Override
	public Class<?> getHistogramKey() {
		return callback != null ? callback.getClass() : null;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.asyncjmx.shared.serialization.HistogramKeyProvider#getVoidHistogramSize()
	 */
	@Override
	public int getVoidHistogramSize() {
		return 9;
	}
	

	/**
	 * Returns the JMX callback type
	 * @return the JMX callback type
	 */
	public JMXResponseType getTypeCode() {
		return typeCode;
	}

	/**
	 * Returns the JMX callback payload
	 * @return the JMX callback payload
	 */
	public Object getCallback() {
		return callback;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuilder("JMXCallback [ type:")
			.append(typeCode.name())
			.append(", callback type:").append(callback==null ? "<null>" : callback.getClass().getName())
			.append(", callback:").append(callback)
		.toString();
	}
	
	/**
	 * <p>Title: JMXCallbackSerializer</p>
	 * <p>Description: Kryo serializer for {@link JMXCallback} instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.asyncjmx.shared.JMXCallback.JMXCallbackSerializer</code></p>
	 */
	public class JMXCallbackSerializer extends BaseSerializer<JMXCallback> {

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.asyncjmx.shared.serialization.BaseSerializer#doWrite(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Output, java.lang.Object)
		 */
		@Override
		protected void doWrite(Kryo kryo, Output output, JMXCallback jc) {
			output.writeByte(jc.typeCode.opCode);
			output.writeInt(jc.target);
			kryo.writeClassAndObject(output, jc.callback);
			
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.asyncjmx.shared.serialization.BaseSerializer#doRead(com.esotericsoftware.kryo.Kryo, com.esotericsoftware.kryo.io.Input, java.lang.Class)
		 */
		@Override
		protected JMXCallback doRead(Kryo kryo, Input input, Class<JMXCallback> type) {
			byte opCode = input.readByte();
			int target = input.readInt();
			Object callback = kryo.readClassAndObject(input);			
			return new JMXCallback(JMXResponseType.decode(opCode), callback, target);
		}
		
	}


}
