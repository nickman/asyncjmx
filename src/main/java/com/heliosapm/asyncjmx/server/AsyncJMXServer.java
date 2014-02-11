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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.JdkLoggerFactory;

/**
 * <p>Title: AsyncJMXServer</p>
 * <p>Description: The netty JMX server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.AsyncJMXServer</code></p>
 */

public class AsyncJMXServer {
	/** The netty channel factory */
	protected ChannelFactory channelFactory = null;
	/** The netty pipeline factory */
	protected ChannelPipelineFactory pipelineFactory = null;
	/** The netty server bootstrap */
	protected ServerBootstrap serverBootstrap = null;
	/** The netty channel factory boss thread pool */
	protected ThreadPoolExecutor bossPool = null;
	/** The netty channel factory worker thread pool */
	protected ThreadPoolExecutor workerPool = null;
	/** A channel group for created connections */
	protected ChannelGroup connections = new DefaultChannelGroup(getClass().getSimpleName());
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass().getName());
	
	static {
		InternalLoggerFactory.setDefaultFactory(new JdkLoggerFactory());
	}
}
