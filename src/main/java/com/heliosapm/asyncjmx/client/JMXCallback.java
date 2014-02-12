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
package com.heliosapm.asyncjmx.client;

import com.heliosapm.asyncjmx.shared.JMXResponseType;

/**
 * <p>Title: JMXCallback</p>
 * <p>Description: Encapsulates a callback from a JMX server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXCallback</code></p>
 */

public class JMXCallback {
	/** The JMX callback type */
	protected final JMXResponseType typeCode;	
	/** The JMX callback payload */
	protected final Object callback;
	
	/**
	 * Creates a new JMXCallback
	 * @param typeCode The JMX callback type
	 * @param callback The JMX callback payload
	 */
	public JMXCallback(JMXResponseType typeCode, Object callback) {
		this.typeCode = typeCode;
		this.callback = callback;
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

}
