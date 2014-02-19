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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.heliosapm.asyncjmx.client.JMXOpResponse;
import com.heliosapm.asyncjmx.client.notifications.ListenerRegistration;
import com.heliosapm.asyncjmx.shared.JMXCallback;
import com.heliosapm.asyncjmx.shared.JMXOp;
import com.heliosapm.asyncjmx.shared.JMXOp.DynamicTypedIterator;
import com.heliosapm.asyncjmx.shared.JMXResponseType;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
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
	private final JMXLogger log = JMXLogger.getLogger(getClass());
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
				log.warn("MBeanServer with default domain or id [%s] already registered", id);
			}
		} else {
			log.warn("MBeanServer [%s] had null or empty default domain or id", mbeanServer);
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
		if(msg instanceof JMXOp) {
			final Channel channel = e.getChannel();
			JMXOp op = (JMXOp)msg;
			Object response = invoke(op, channel);
			log.info("[%s] request result: type:[%s], value:[%s]", op.getJmxOpCode(), response.getClass().getName(), response);
			JMXOpResponse jmxResponse = new JMXOpResponse(op.getJmxOpCode(), op.getOpSeq(), response); 
			writeRequested(ctx, new DownstreamMessageEvent(channel, Channels.future(channel), jmxResponse, e.getRemoteAddress()));
		} else {
			super.messageReceived(ctx, e);
		}
	}
	
//	/**
//	 * Sends the header of the response back to the caller
//	 * @param ctx The channel handler context
//	 * @param remoteAddress The remote address we're sending back to
//	 * @param opCode The JMX op code of the original incoming request
//	 * @param requestId The request id of the original incoming request
//	 */
//	protected void sendResponseHeader(ChannelHandlerContext ctx, SocketAddress remoteAddress, JMXOpCode opCode, int requestId) {
//		{JMXResponseType.JMX_RESPONSE.opCode, op.opCode, op.requestId, response}
//		ChannelBuffer buf = ChannelBuffers.buffer(6);
//		buf.writeByte(JMXResponseType.JMX_RESPONSE.opCode);
//		buf.writeByte(opCode.opCode);
//		buf.writeInt(requestId);
//		Channel channel = ctx.getChannel();
//		ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), buf, remoteAddress));
//	}
	
	
	/**
	 * Executes a received JMXOpInvocation instance
	 * @param opInvocation The invocation to execute
	 * @param channel The channel on which the invocation is being invoked
	 * @return the result of the invocation
	 */
	protected <T> Object invoke(JMXOp opInvocation, Channel channel)  {
		String id = opInvocation.getJmxDomain();
		final MBeanServerConnection mbeanServer = knownMBeanServers.get(id);
		if(mbeanServer==null) return new IOException("Failed to locate MBeanServer with id [" + id + "]");
		DynamicTypedIterator argIter =  opInvocation.getArgumentIterator();
		try {
			switch(opInvocation.getJmxOpCode()) {
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
	
	
	/**
	 * Invokes a notification listener registration
	 * @param opInvocation The wrapped invocation
	 * @param channel The channel on which the registration is being executed
	 * @param argIter The operation argument iterator
	 * @param mbeanServer The target MBeanServer in which to register the listener
	 * @throws InstanceNotFoundException thrown if the target MBean is not found
	 * @throws IOException thrown on an IO remoting error
	 */
	protected void registerNotificationListener(JMXOp opInvocation, final Channel channel, DynamicTypedIterator argIter, final MBeanServerConnection mbeanServer) throws InstanceNotFoundException, IOException {
		final ObjectName target = argIter.next(ObjectName.class);
		NotificationListener listener = argIter.next(NotificationListener.class);
		NotificationFilter filter = argIter.next(NotificationFilter.class);
		Object handback = argIter.next(Object.class);
		if(listener instanceof ListenerRegistration) {
			ListenerRegistration lr = (ListenerRegistration)listener;
			final int registrationId = lr.getRegistrationId();
			final NotificationFilter registrationFilter = lr.getFilter();
			final NotificationListener actualListener = new NotificationListener() {
				@Override
				public void handleNotification(Notification notification, Object handback) {
					final JMXCallback callback = new JMXCallback(JMXResponseType.JMX_NOTIFICATION, notification, registrationId);
					channel.write(callback).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							log.info("JMXCallback [%s] Transmission Complete", callback);
						}
					});
				}
			};
			channel.getCloseFuture().addListener(new ChannelFutureListener() {				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					mbeanServer.removeNotificationListener(target, actualListener, registrationFilter, null);
				}
			});
		} else {
			mbeanServer.addNotificationListener(target, listener, filter, handback);
		}
		
	}
	
}
