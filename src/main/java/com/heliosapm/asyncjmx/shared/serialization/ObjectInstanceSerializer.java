/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>Title: ObjectInstanceSerializer</p>
 * <p>Description: Kryo serializer for JMX {@link ObjectInstance}s</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.ObjectInstanceSerializer</code></p>
 */

public class ObjectInstanceSerializer extends BaseSerializer<ObjectInstance> {

	@Override
	protected void doWrite(Kryo kryo, Output output, ObjectInstance object) {
		kryo.writeObject(output, object.getObjectName());
		output.writeString(object.getClassName());
	}

	@Override
	protected ObjectInstance doRead(Kryo kryo, Input input, Class<ObjectInstance> type) {
		ObjectName on = kryo.readObject(input, ObjectName.class);
		return new ObjectInstance(on, input.readString());
	}

}
