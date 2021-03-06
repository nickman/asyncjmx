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
package com.heliosapm.asyncjmx.client;

/**
 * <p>Title: JMXResponseDecodeStep</p>
 * <p>Description: Enumeration of the {@link JMXOpResponse} replaying decoder steps.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXResponseDecodeStep</code></p>
 */

public enum JMXResponseDecodeStep {
	/** The JMX Response Type */
	RESPTYPE(0),
	/** The total byte size of the entire response wrapper */
	BYTESIZE(0),
	/** The one byte op code that a repsonse is responding to */
	OPCODE(0),
	/** The one int originating request id */
	REQUESTID(1),
	/** The op response value */
	RESPONSE(5);
	
	
	private JMXResponseDecodeStep(int offset) {
		this.offset = offset;
	}
	
	/** The logical offset in the ChannelBuffer when this checkpoint is called */
	public final int offset;

}
