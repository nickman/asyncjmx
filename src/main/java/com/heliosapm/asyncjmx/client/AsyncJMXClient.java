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

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
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

import com.heliosapm.asyncjmx.shared.logging.JMXLogger;

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
		InputStream is = null;
		try {
			is = AsyncJMXClient.class.getClassLoader().getResourceAsStream("client-logging.properties");
			LogManager.getLogManager().readConfiguration(is);
			String tsFormat = LogManager.getLogManager().getProperty(JMXLogger.TS_FORMAT_KEY);
			JMXLogger.setTimestampFormat(tsFormat!=null ? tsFormat : JMXLogger.DEFAULT_DATE_FORMAT);
		} catch (Exception ex) {
			System.err.println("Failed to load client logging configuration:" + ex);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception x) { /* No Op */ } 
		}
		
		InternalLoggerFactory.setDefaultFactory(new JdkLoggerFactory());
	}
	
	public AsyncJMXClient() {
		bossPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		workerPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		
		clientBootstrap = new ClientBootstrap(channelFactory);
		clientBootstrap.setPipelineFactory(new JMXClientPipelineFactory(this));		
		clientBootstrap.setOption("tcpNoDelay", true);
		clientBootstrap.setOption("receiveBufferSize", 1048576);
		clientBootstrap.setOption("sendBufferSize", 1048576);
		clientBootstrap.setOption("bufferFactory", new DirectChannelBufferFactory(2048));
	}
	
	public MBeanServerConnection connectMBeanServerConnection(String host, int port, boolean async) {
		Channel channel = clientBootstrap.connect(new InetSocketAddress(host, port)).awaitUninterruptibly().getChannel();
		if(!async) {
			return new SyncMBeanServerConnection(channel, 300000);
		}
		throw new RuntimeException("No async yet");
		
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}
	
	public static void main(String[] args) {
		AsyncJMXClient client = new AsyncJMXClient();
		try {
			MBeanServerConnection conn = client.connectMBeanServerConnection("localhost", 9061, false);
			log("Connected [%s]", conn);
			ObjectName on = new ObjectName("java.lang:type=Threading");
			boolean reg = conn.isRegistered(on);
			log("isRegistered(%s):%s", on, reg);
			on = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
			reg = conn.isRegistered(on);
			log("isRegistered(%s):%s", on, reg);
			on = new ObjectName("java.lang:type=Threading");
			MBeanInfo minfo = conn.getMBeanInfo(on);
			log("MBeanInfo for [%s]:\n%s", on, minfo);
			
			NotificationListener listener = new NotificationListener() {
				/**
				 * {@inheritDoc}
				 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
				 */
				@Override
				public void handleNotification(Notification notification, Object handback) {
					System.out.println("NOTIF: ------>" + notification);					
				}
			};
			
			for(ObjectName mb: conn.queryNames(new ObjectName("java.lang:type=GarbageCollector,name=*"), null)) {
				conn.addNotificationListener(mb, listener, null, null);
			}
			
			System.out.println("\n\n\t************\n\tListening...\n\t************");
			Thread.sleep(120000);
//			
////			System.out.println("DefaultDomain:" + conn.getDefaultDomain());
//			Set<ObjectInstance> objectInstances = conn.queryMBeans(null, null);
//			if(objectInstances!=null) {
//				System.out.println("ObjectInstances:" + objectInstances.size());
//				for(Object on: objectInstances) {
//					System.out.println("\t{" + on + "}");
//				}
//			} else {
//				System.out.println("QueryMBeans Failed");
//			}
//			
//			Set<ObjectName> objectNames = conn.queryNames(null, null);
//			if(objectNames!=null) {
//				System.out.println("ObjectNames:" + objectNames.size());
//				for(Object on: objectNames) {
//					System.out.println("\t" + on);
//				}
//			} else {
//				System.out.println("QueryNames Failed");
//			}
//
//			
//			
//			ObjectName obn = null;			
//			obn = new ObjectName("java.lang:name=PS Eden Space,type=MemoryPool");
//			System.out.println("REQUESTING Minfo:[" + obn + "]");
//			MBeanInfo minfo = conn.getMBeanInfo(obn);
//			System.out.println("Minfo:[" + obn + "]\n" + minfo);
//			obn = new ObjectName("java.lang:type=Memory");
//			System.out.println("REQUESTING Minfo:[" + obn + "]");
//			minfo = conn.getMBeanInfo(obn);
//			System.out.println("Minfo:[" + obn + "]\n" + minfo);			
//			obn = new ObjectName("java.lang:name=PS MarkSweep,type=GarbageCollector");
//			System.out.println("REQUESTING Minfo:[" + obn + "]");
//			minfo = conn.getMBeanInfo(obn);
//			System.out.println("Minfo:[" + obn + "]\n" + minfo);
//			
//			if(minfo==null) return;
//			for(ObjectName on: conn.queryNames(null, null)) {
//				System.out.println("Sending GETMBEANINFO for [" + on + "]");
//				minfo = conn.getMBeanInfo(on);
//				System.out.println("Got GETMBEANINFO for [" + on + "]--->" + minfo);
//				Set<String> attrNames = new HashSet<String>();
//				try {
//					
//					for(MBeanAttributeInfo ma: minfo.getAttributes()) {
//						attrNames.add(ma.getName());
//						Descriptor d = ma.getDescriptor();
//						if(d!=null) {
//							if(d instanceof ImmutableDescriptor) {
//								System.out.println("ImmutableDescriptor:" + d);
//							} else {
//								System.out.println("Descriptor:" + d);
//							}
//							
//						}
//					}
//				} catch (Exception ex) {
//					UnsafeAdapter.throwException(ex);
//				}
//				AttributeList attrs = conn.getAttributes(on, attrNames.toArray(new String[attrNames.size()]));
//			}
			System.out.println("\n\t========================================\n\tCOMPLETE\n\t========================================\n");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			client.connections.close().awaitUninterruptibly();
			client.channelFactory.releaseExternalResources();
			client.channelFactory.shutdown();
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
