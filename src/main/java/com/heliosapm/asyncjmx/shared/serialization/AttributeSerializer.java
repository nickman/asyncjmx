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

import javax.management.Attribute;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;

/**
 * <p>Title: AttributeSerializer</p>
 * <p>Description: Kryo serializer for JMX Attributes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.AttributeSerializer</code></p>
 */

public class AttributeSerializer extends BaseSerializer<Attribute> {
	@Override
	protected void doWrite(Kryo kryo, Output output, Attribute object) {
		output.writeString(object.getName());
		Object value = object.getValue();
		kryo.writeClassAndObject(output, value);
		log.info("Wrote Attribute [%s][%s]", object.getName(), value);
	}

	@Override
	protected Attribute doRead(Kryo kryo, Input input, Class<Attribute> type) {		
		String name = input.readString();
		log.info("Reading Attribute [%s]", name);
		Object value = kryo.readClassAndObject(input);
		return new Attribute(name, value);
	}

}
