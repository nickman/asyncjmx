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
package com.heliosapm.asyncjmx.shared.logging;

/**
 * <p>Title: IRenderer</p>
 * <p>Description: Defines a class that provides custom string rendering for specific classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.logging.IRenderer</code></p>
 * @param <T> The assumed type of the object being rendered
 */

public interface IRenderer<T> {
	
	/** The default renderer */
	public static final IRenderer<Object> DEFAULT = new IRenderer.DefaultRenderer();
	
	/**
	 * Renders the passed object instance to a loggable string
	 * @param t The object to render
	 * @return The string rendered
	 */
	public String render(T t);
	
	/**
	 * Returns the type this renderer renders for
	 * @return a class 
	 */
	public Class<T> getSupportedType();
	
	/**
	 * <p>Title: DefaultRenderer</p>
	 * <p>Description: The default renderer for classes that do not match to any other renderer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.asyncjmx.shared.logging.IRenderer.DefaultRenderer</code></p>
	 */
	public static class DefaultRenderer implements IRenderer<Object> {

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.asyncjmx.shared.logging.IRenderer#render(java.lang.Object)
		 */
		@Override
		public String render(Object t) {
			if(t==null) return "<null>";
			return t.toString();
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.asyncjmx.shared.logging.IRenderer#getSupportedType()
		 */
		@Override
		public Class<Object> getSupportedType() {
			return Object.class;
		}
		
	}
}
