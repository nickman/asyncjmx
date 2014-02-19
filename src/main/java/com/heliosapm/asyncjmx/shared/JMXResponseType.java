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
package com.heliosapm.asyncjmx.shared;

/**
 * <p>Title: JMXResponseType</p>
 * <p>Description: Protocol flag to indicate what incoming events are</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.JMXResponseType</code></p>
 */

public enum JMXResponseType {
	/** A repsonse to a JMX Op */
	JMX_RESPONSE("RESPONSE_HANDLER"),
	/** An async JMX Notification emitted from the server */
	JMX_NOTIFICATION("NOTIFICATION_HANDLER"),
	/** A cache update or directive */
	CACHE_OP("CACHEOP_HANDLER");
	
	private JMXResponseType(String handlerName) {
		this.handlerName = handlerName;
	}
	
	/** The opcode byte for this response type */
	public final byte opCode = (byte)this.ordinal();
	
	/** THe name of the handler in the channel pipeline that will handle this response type */
	public final String handlerName;
	
	/**
	 * Decodes the passed byte to a JMXResponseType
	 * @param opCode The byte to decode
	 * @return the decoded JMXResponseType
	 */
	public static JMXResponseType decode(byte opCode) {
		switch(opCode) {
			case 0:
				return JMX_RESPONSE;
			case 1:
				return JMX_NOTIFICATION;
			case 2:
				return CACHE_OP;
			default:
				throw new IllegalArgumentException("Invalid JMXResponseType Op Code [" + opCode + "]");
		}
	}
			
}
