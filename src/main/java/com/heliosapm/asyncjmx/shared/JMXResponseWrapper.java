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
 * <p>Title: JMXResponseWrapper</p>
 * <p>Description: A local wrapper for passing JMX op responses</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.JMXResponseWrapper</code></p>
 */

public class JMXResponseWrapper {
	/** The id of the request id this response is responding to */
	protected int responseId;
	/** The response value */
	protected Object response = null;
	/** The void indicator */
	protected byte wasVoid;
	
	
	/**
	 * Creates a new JMXResponseWrapper
	 * @param responseId The id of the request id this response is responding to
	 * @param response The response value
	 */
	public JMXResponseWrapper(int responseId, Object response) {
		this.responseId = responseId;
		this.response = response;
		wasVoid = 0;
	}
	
	public JMXResponseWrapper(int responseId) {
		this.responseId = responseId;
		wasVoid = 1;
	}
	
	
}
