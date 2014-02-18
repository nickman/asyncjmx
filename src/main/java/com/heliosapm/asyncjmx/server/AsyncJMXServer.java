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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.JdkLoggerFactory;

import com.heliosapm.asyncjmx.client.AsyncJMXClient;
import com.heliosapm.asyncjmx.shared.KryoFactory;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;

/**
 * <p>Title: AsyncJMXServer</p>
 * <p>Description: The netty JMX server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.AsyncJMXServer</code></p>
 */

public class AsyncJMXServer implements ChannelUpstreamHandler {
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
	/** The listening port */
	protected int port = 9061;
	/** The listener binding interface */
	protected String bindInterface = "0.0.0.0";
	/** The listener inet socket address */
	protected  InetSocketAddress socketAddress = null;
	/** The server listening channel */
	protected Channel serverChannel = null;
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass().getName());
	
	static {
		InputStream is = null;
		try {
			is = AsyncJMXClient.class.getClassLoader().getResourceAsStream("server-logging.properties");
			LogManager.getLogManager().readConfiguration(is);
			String tsFormat = LogManager.getLogManager().getProperty(JMXLogger.TS_FORMAT_KEY);
			JMXLogger.setTimestampFormat(tsFormat!=null ? tsFormat : JMXLogger.DEFAULT_DATE_FORMAT);
		} catch (Exception ex) {
			System.err.println("Failed to load server logging configuration:" + ex);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) { /* No Op */ } 
		}
		
		InternalLoggerFactory.setDefaultFactory(new JdkLoggerFactory());
	}
	
	public AsyncJMXServer() {
		bossPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		workerPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		
		serverBootstrap = new ServerBootstrap(channelFactory);
		serverBootstrap.setPipelineFactory(new JMXServerPipelineFactory(this));
		serverBootstrap.setOption("child.tcpNoDelay", true);
		serverBootstrap.setOption("child.receiveBufferSize", 1048576);
		serverBootstrap.setOption("child.sendBufferSize", 1048576);
		serverBootstrap.setOption("child.bufferFactory", new DirectChannelBufferFactory(2048));
		 		
		
		socketAddress = new InetSocketAddress(bindInterface, port);
		serverChannel = serverBootstrap.bind(socketAddress);
		
		log.warning("Server Channel [" + serverChannel + "] Started");
		
	}
	
	public static void main(String[] args) {
		final AsyncJMXServer server = new AsyncJMXServer();
		Thread t = new Thread() {
			public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				while(true) {
					try {
						String line = br.readLine();
						try {
							if("list".equals(line)) {
								System.out.println("------- Channels -------");
								for(Channel ch: server.connections) {
									System.out.println("\t" + ch.getId());
								}
								System.out.println("------------------------");
							} else {
								String[] pair = line.split("\\s+");
								int chId = Integer.parseInt(pair[0]);
								Channel ch = server.connections.find(chId);
								System.out.println("Selected Channel:" + ch + "  Arg:" + pair[1]);
								if("dump".equals(pair[1])) {
									int regid = KryoFactory.getInstance().getKryo(ch).getNextRegistrationId();
									for(int i = regid; i > 0; i--) {
										try { System.out.println(KryoFactory.getInstance().getKryo(ch).getRegistration(i)); } catch (Exception x) {}
									}
									continue;
								}
								int classId = Integer.parseInt(pair[1]);
								System.out.println(KryoFactory.getInstance().getKryo(ch).getRegistration(classId).getType().getName());
							}
							
							
						} catch (Exception x) {}
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChildChannelStateEvent) {
            ChildChannelStateEvent evt = (ChildChannelStateEvent) e;
            if (evt.getChildChannel().isOpen()) {
            	connections.add(e.getChannel());
            	log.warning("Adding Channel [" + e.getChannel().getClass().getName() + "]:" + e.getChannel());
            	ctx.getPipeline().remove(this);
            }
		} else if (e instanceof ChannelStateEvent) {
			ChannelStateEvent cse = (ChannelStateEvent)e;
			if (cse.getChannel().isOpen()) {
            	connections.add(cse.getChannel());
            	log.warning("Adding Channel [" + cse.getChannel().getClass().getName() + "]:" + cse.getChannel());
            	ctx.getPipeline().remove(this);				
			}
		}
	}	


}
