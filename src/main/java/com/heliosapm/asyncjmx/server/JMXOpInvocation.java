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

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.serialization.PlaceHolder;

/**
 * <p>Title: JMXOpInvocation</p>
 * <p>Description: Represents a deserialized JMX Op Invocation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.JMXOpInvocation</code></p>
 */

public class JMXOpInvocation {
	/** The decoded op code */
	public final JMXOpCode opCode;
	/** The request ID */
	protected Integer requestId = null;
	/** The arguments to the JMX Op */
	protected final Object[] args;
	/** The arg deserialization count */
	protected transient int deserCount = 0;
	/** The default domain of the MBeanServer to invoke against */
	protected String jmxDomain = null;
	
	/** The default domain of the default MBeanServer to invoke against */
	public static final String DEFAULT_MBEANSERVER_DOMAIN = "DefaultDomain";
	
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

	/**
	 * Returns the expected number of args to read
	 * @return the expected number of args to read
	 */
	public int getDeserArgCount() {
		return args.length;
	}

	/**
	 * Returns true if the op is expecting more args for deserialization
	 * @return true if more args are expected, false otherwise
	 */
	public boolean hasMoreArgs() {
		return deserCount<args.length; 
	}
	
	/**
	 * Appends a newly deserialized argument object
	 * @param arg a deserialized argument object
	 */
	public void appendArg(Object arg) {
		args[deserCount] = arg;
		deserCount++;		
	}

	/**
	 * Returns the default domain of the MBeanServer to invoke against
	 * @return the jmxDomain the default domain of the target MBeanServer 
	 */
	public String getJmxDomain() {
		return jmxDomain!=null ? jmxDomain : DEFAULT_MBEANSERVER_DOMAIN;
	}

	/**
	 * Returns the arguments to the JMX Op
	 * @return an array of arguments
	 */
	public Object[] getArgs() {
		return args;
	}
	
	/**
	 * @return
	 */
	public DynamicTypedIterator getArgumentIterator() {
		return new DynamicTypedIterator();
	}
	
	public class DynamicTypedIterator {
		int index = 0;
		final int argCnt = args.length;

		public boolean hasNext() {
			return index < argCnt;
		}
		
		public <T> T next(Class<T> type) throws NoSuchElementException, IllegalStateException {
			if(index >= argCnt) {
				throw new NoSuchElementException();
			}
			try {
				Object o = args[index];
				if(o!=null && o instanceof PlaceHolder) {
					return null;
				}
				return (T)args[index];
			} finally {
				index++;
			}
		}
	}
	
}
