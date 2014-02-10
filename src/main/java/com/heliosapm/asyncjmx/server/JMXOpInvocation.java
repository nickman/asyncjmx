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
package com.heliosapm.asyncjmx.server;

import com.heliosapm.asyncjmx.shared.JMXOpCode;

/**
 * <p>Title: JMXOpInvocation</p>
 * <p>Description: Represents a deserialized JMX Op Invocation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.JMXOpInvocation</code></p>
 */

public class JMXOpInvocation {
	/** The decoded op code */
	protected final JMXOpCode opCode;
	/** The request ID */
	protected Integer requestId = null;
	/** The arguments to the JMX Op */
	protected final Object[] args;
	
	/**
	 * Creates a new JMXOpInvocation
	 * @param opCode The byte op code
	 */
	public JMXOpInvocation(byte opCode) {		
		this.opCode = JMXOpCode.decode(opCode);
		args = new Object[this.opCode.signature().length];
	}
	
	/**
	 * Sets the request ID
	 * @param rId The request id of this jmx op
	 */
	public void setRequestId(int rId) {
		requestId = rId;
	}
	
	/**
	 * Returns the request ID
	 * @return The request id of this jmx op, or null if it has not been set
	 */
	public Integer getRequestId() {
		return requestId;
	}
	
	
	
}
