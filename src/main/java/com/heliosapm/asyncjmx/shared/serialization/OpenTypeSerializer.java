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

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;

/**
 * <p>Title: OpenTypeSerializer</p>
 * <p>Description: Serializer for {@link OpenType} instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.OpenTypeSerializer</code></p>
 */

public class OpenTypeSerializer extends BaseSerializer<OpenType> {
	
	protected static final ArrayTypeSerializer arrSerializer = new ArrayTypeSerializer();
	protected static final SimpleTypeSerializer stSerializer = new SimpleTypeSerializer();
	protected static final TabularTypeSerializer tabSerializer = new TabularTypeSerializer();
	protected static final CompositeTypeSerializer ctSerializer = new CompositeTypeSerializer();
	protected static final OpenTypeSerializer otSerializer = new OpenTypeSerializer();
	/** Instance logger */
	protected final JMXLogger log = JMXLogger.getLogger(getClass());
	
	@Override
	protected void doWrite(Kryo kryo, Output output, OpenType ot) {
		kryo.getSerializer(ot.getClass()).write(kryo, output, ot);
	}

	@Override
	protected OpenType doRead(Kryo kryo, Input input, Class<OpenType> type) {
		Class<?> clazz = kryo.readClass(input).getType();
		return (OpenType)kryo.getSerializer(clazz).read(kryo, input, clazz);
	}

}
