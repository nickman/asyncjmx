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
package com.heliosapm.asyncjmx.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.heliosapm.asyncjmx.server.JMXOpInvocation.DynamicTypedIterator;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.JMXResponseType;
import com.heliosapm.asyncjmx.shared.serialization.VoidResult;

/**
 * <p>Title: JMXMBeanServerInvocationHandler</p>
 * <p>Description: JMX Op invocation handler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.JMXMBeanServerInvocationHandler</code></p>
 */
@ChannelHandler.Sharable
public class JMXMBeanServerInvocationHandler extends SimpleChannelHandler {
	/** The singleton instance */
	private static volatile JMXMBeanServerInvocationHandler instance = null;
	/** The singleton instance ctor lock*/
	private static final Object lock = new Object();
	/** Instance logger */
	private final Logger log = Logger.getLogger(getClass().getName());
	/** A map of the known MBeanServerConnections keyed by either the server's default domain or by the JMXServiceURL */
	protected final Map<String, MBeanServerConnection> knownMBeanServers = new ConcurrentHashMap<String, MBeanServerConnection>();
	
	
	/**
	 * Acquires the JMXMBeanServerInvocationHandler singleton instance
	 * @return the JMXMBeanServerInvocationHandler singleton instance
	 */
	public static JMXMBeanServerInvocationHandler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new JMXMBeanServerInvocationHandler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new JMXMBeanServerInvocationHandler
	 */
	private JMXMBeanServerInvocationHandler() {
		log.info("Initializing JMXMBeanServerInvocationHandler");
		knownMBeanServers.put(JMXOpInvocation.DEFAULT_MBEANSERVER_DOMAIN, ManagementFactory.getPlatformMBeanServer());
		for(MBeanServer mbeanServer: MBeanServerFactory.findMBeanServer(null)) {
			String domain = mbeanServer.getDefaultDomain();
			registerMBeanServer(domain, mbeanServer);
		}
		log.info("JMXMBeanServerInvocationHandler Initialization Complete. Registered MBeanServers: " + knownMBeanServers.size());
	}
	
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		e.getCause().printStackTrace(System.err);
	}
	
	/**
	 * Registers a discovered MBeanServer
	 * @param id The assigned id which can be the MBeanServer's default domain or the connecting JMXServiceURL
	 * @param mbeanServer The MBeanServer to register
	 */
	public void registerMBeanServer(String id, MBeanServerConnection mbeanServer) {
		if(mbeanServer==null) throw new IllegalArgumentException("The passed mbeanServer was null");
		if(id!=null && !id.trim().isEmpty()) {
			if(!knownMBeanServers.containsKey(id)) {
				synchronized(knownMBeanServers) {
					if(!knownMBeanServers.containsKey(id)) {
						knownMBeanServers.put(id, mbeanServer);
					}
				}				
			} else {
				log.warning("MBeanServer with default domain or id [" + id + "] already registered");
			}
		} else {
			log.warning("MBeanServer [" + mbeanServer + "] had null or empty default domain or id");
		}		
	}

	/**
	 * <p>Receives and processes {@link JMXOpInvocation} instances.</p>
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		final Object msg = e.getMessage();
		if(msg instanceof JMXOpInvocation) {
			final Channel channel = e.getChannel();
			JMXOpInvocation op = (JMXOpInvocation)msg;
			Object response = invoke(op);
			log.info("[" + op.opCode + "] request result:" + response);
			sendResponseHeader(ctx, e.getRemoteAddress(), op.opCode, op.requestId);
			writeRequested(ctx, new DownstreamMessageEvent(channel, Channels.future(channel), response, e.getRemoteAddress()));
		} else {
			super.messageReceived(ctx, e);
		}
	}
	
	/**
	 * Sends the header of the response back to the caller
	 * @param ctx The channel handler context
	 * @param remoteAddress The remote address we're sending back to
	 * @param opCode The JMX op code of the original incoming request
	 * @param requestId The request id of the original incoming request
	 */
	protected void sendResponseHeader(ChannelHandlerContext ctx, SocketAddress remoteAddress, JMXOpCode opCode, int requestId) {
		ChannelBuffer buf = ChannelBuffers.buffer(6);
		buf.writeByte(JMXResponseType.JMX_RESPONSE.opCode);
		buf.writeByte(opCode.opCode);
		buf.writeInt(requestId);
		Channel channel = ctx.getChannel();
		ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), buf, remoteAddress));
	}
	
	
	/**
	 * Executes a received JMXOpInvocation instance
	 * @param opInvocation The invocation to execute
	 * @return the result of the invocation
	 */
	protected <T> Object invoke(JMXOpInvocation opInvocation)  {
		String id = opInvocation.getJmxDomain();
		final MBeanServerConnection mbeanServer = knownMBeanServers.get(id);
		if(mbeanServer==null) return new IOException("Failed to locate MBeanServer with id [" + id + "]");
		DynamicTypedIterator argIter =  opInvocation.getArgumentIterator();
		try {
			switch(opInvocation.opCode) {
			case ADDNOTIFICATIONLISTENER_ONNO:
				mbeanServer.addNotificationListener(argIter.next(ObjectName.class), argIter.next(NotificationListener.class), argIter.next(NotificationFilter.class), argIter.next(Object.class));
				return VoidResult.Instance;
			case ADDNOTIFICATIONLISTENER_OONO:
				mbeanServer.addNotificationListener(argIter.next(ObjectName.class), argIter.next(ObjectName.class), argIter.next(NotificationFilter.class), argIter.next(Object.class));
				return VoidResult.Instance;
			case CREATEMBEAN_SO:
				return mbeanServer.createMBean(argIter.next(String.class), argIter.next(ObjectName.class));
			case CREATEMBEAN_SOO:
				return mbeanServer.createMBean(argIter.next(String.class), argIter.next(ObjectName.class), argIter.next(ObjectName.class));
			case CREATEMBEAN_SOOOS:
				return mbeanServer.createMBean(argIter.next(String.class), argIter.next(ObjectName.class), argIter.next(ObjectName.class), argIter.next(Object[].class), argIter.next(String[].class));				
			case CREATEMBEAN_SOOS:
				return mbeanServer.createMBean(argIter.next(String.class), argIter.next(ObjectName.class), argIter.next(Object[].class), argIter.next(String[].class));
			case GETATTRIBUTE:
				return mbeanServer.getAttribute(argIter.next(ObjectName.class), argIter.next(String.class));
			case GETATTRIBUTES:
				return mbeanServer.getAttributes(argIter.next(ObjectName.class), argIter.next(String[].class));
			case GETDEFAULTDOMAIN:
				return mbeanServer.getDefaultDomain();
			case GETDOMAINS:
				return mbeanServer.getDomains();
			case GETMBEANCOUNT:
				return mbeanServer.getMBeanCount();
			case GETMBEANINFO:
				return mbeanServer.getMBeanInfo(argIter.next(ObjectName.class));
			case GETOBJECTINSTANCE:
				return mbeanServer.getObjectInstance(argIter.next(ObjectName.class));
			case INVOKE:
				return mbeanServer.invoke(argIter.next(ObjectName.class), argIter.next(String.class), argIter.next(Object[].class), argIter.next(String[].class));
			case ISINSTANCEOF:
				return mbeanServer.isInstanceOf(argIter.next(ObjectName.class), argIter.next(String.class));
			case ISREGISTERED:
				return mbeanServer.isRegistered(argIter.next(ObjectName.class));
			case QUERYMBEANS:
				return mbeanServer.queryMBeans(argIter.next(ObjectName.class), argIter.next(QueryExp.class));
			case QUERYNAMES:
				return mbeanServer.queryNames(argIter.next(ObjectName.class), argIter.next(QueryExp.class));
			case REMOVENOTIFICATIONLISTENER_ON:
				mbeanServer.removeNotificationListener(argIter.next(ObjectName.class), argIter.next(NotificationListener.class));
				return VoidResult.Instance;
			case REMOVENOTIFICATIONLISTENER_ONNO:
				mbeanServer.removeNotificationListener(argIter.next(ObjectName.class), argIter.next(NotificationListener.class), argIter.next(NotificationFilter.class), argIter.next(Object.class));
				return VoidResult.Instance;
			case REMOVENOTIFICATIONLISTENER_OO:
				mbeanServer.removeNotificationListener(argIter.next(ObjectName.class), argIter.next(ObjectName.class));
				return VoidResult.Instance;
			case REMOVENOTIFICATIONLISTENER_OONO:
				mbeanServer.removeNotificationListener(argIter.next(ObjectName.class), argIter.next(ObjectName.class), argIter.next(NotificationFilter.class), argIter.next(Object.class));
				return VoidResult.Instance;
			case SETATTRIBUTE:
				mbeanServer.setAttribute(argIter.next(ObjectName.class), argIter.next(Attribute.class));
				return VoidResult.Instance;
			case SETATTRIBUTES:
				return mbeanServer.setAttributes(argIter.next(ObjectName.class), argIter.next(AttributeList.class));
			case UNREGISTERMBEAN:
				mbeanServer.unregisterMBean(argIter.next(ObjectName.class));
				return VoidResult.Instance;
			default:
				return new IOException("Failed to match JMXOp to MBeanServer operation");
			
			}
		} catch (Throwable tx) {
			return tx;
		}
	}
	
}
