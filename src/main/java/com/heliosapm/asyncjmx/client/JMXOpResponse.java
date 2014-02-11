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

import com.heliosapm.asyncjmx.shared.JMXOpCode;

/**
 * <p>Title: JMXOpResponse</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXOpResponse</code></p>
 */

public class JMXOpResponse {
	/** The JMXOpCode of the original request */
	protected final JMXOpCode opCode;	
	/** The request id of the original request */
	protected final int requestId;
	/** The JMX Op response */
	protected final Object response;
	
	/**
	 * Creates a new JMXOpResponse
	 * @param opCode The JMXOpCode of the original request
	 * @param requestId The request id of the original request
	 * @param response The op response
	 */
	public JMXOpResponse(JMXOpCode opCode, int requestId, Object response) {
		super();
		this.opCode = opCode;
		this.requestId = requestId;
		this.response = response;
	}

	/**
	 * Returns the JMXOpCode of the original request
	 * @return the opCode
	 */
	public JMXOpCode getOpCode() {
		return opCode;
	}

	/**
	 * Returns the request id of the original request
	 * @return the requestId
	 */
	public int getRequestId() {
		return requestId;
	}

	/**
	 * Returns the JMX Op response 
	 * @return the response
	 */
	public Object getResponse() {
		return response;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((opCode == null) ? 0 : opCode.hashCode());
		result = prime * result + requestId;
		result = prime * result
				+ ((response == null) ? 0 : response.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JMXOpResponse other = (JMXOpResponse) obj;
		if (opCode != other.opCode)
			return false;
		if (requestId != other.requestId)
			return false;
		if (response == null) {
			if (other.response != null)
				return false;
		} else if (!response.equals(other.response))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JMXOpResponse [");
		if (opCode != null) {
			builder.append("opCode=");
			builder.append(opCode);
			builder.append(", ");
		}
		builder.append("requestId=");
		builder.append(requestId);
		builder.append(", ");
		if (response != null) {
			builder.append("response=");
			builder.append(response);
		}
		builder.append("]");
		return builder.toString();
	}
	
	
	
	
}
