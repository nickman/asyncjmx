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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.JdkLoggerFactory;

/**
 * <p>Title: AsyncJMXClient</p>
 * <p>Description: The netty async JMX client</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.AsyncJMXClient</code></p>
 */

public class AsyncJMXClient implements ChannelUpstreamHandler {
	/** The netty channel factory */
	protected ChannelFactory channelFactory = null;
	/** The netty pipeline factory */
	protected ChannelPipelineFactory pipelineFactory = null;
	/** The netty client bootstrap */
	protected ClientBootstrap clientBootstrap = null;
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
	
	public AsyncJMXClient() {
		bossPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		workerPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		
		clientBootstrap = new ClientBootstrap(channelFactory);
		clientBootstrap.setPipelineFactory(new JMXClientPipelineFactory(this));		
	}
	
	public MBeanServerConnection connectMBeanServerConnection(String host, int port) {
		Channel channel = clientBootstrap.connect(new InetSocketAddress(host, port)).awaitUninterruptibly().getChannel();
		return new SyncMBeanServerConnection(channel, 5000);
	}
	
	public static void main(String[] args) {
		AsyncJMXClient client = new AsyncJMXClient();
		try {
			MBeanServerConnection conn = client.connectMBeanServerConnection("localhost", 9061);
			System.out.println("DefaultDomain:" + conn.getDefaultDomain());
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			ChannelStateEvent cse = (ChannelStateEvent)e;
			if (cse.getChannel().isOpen()) {
            	connections.add(cse.getChannel());
            	log.warning("Adding Channel [" + cse.getChannel().getClass().getName() + "]:" + cse.getChannel());
            	ctx.getPipeline().remove(this);				
			}
		}

	}

}
