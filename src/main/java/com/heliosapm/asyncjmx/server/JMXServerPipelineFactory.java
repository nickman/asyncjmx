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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.ssl.SslBufferPool;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import com.heliosapm.asyncjmx.server.serialization.JMXOpDecoder2;
import com.heliosapm.asyncjmx.server.serialization.JMXResponseEncoder;
import com.heliosapm.asyncjmx.server.ssl.SecureJMXSslContextFactory;

/**
 * <p>Title: JMXServerPipelineFactory</p>
 * <p>Description: The JMX server channel pipeline factory</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.server.JMXServerPipelineFactory</code></p>
 */

public class JMXServerPipelineFactory implements ChannelPipelineFactory {

	/*
	 * SSL Handler
	 * Executor Handler
	 * Logging Handler
	 * Deserialization Handler
	 * JMX Auth Handler
	 * JMX Invoker Handler
	 */
	
	
	/** The logger name of the installed logging handler */
	public final String LOG_NAME;

	/** A map of a few differently configured logging handlers that can be activated via JMX */
	protected final Map<String, LoggingHandler> loggingHandlers;
	/** The currently installed logging handler config */
	protected final AtomicReference<String> loggingConfigName = new AtomicReference<String>(LOGGER_HEX_INFO);
	/** Instance logger for handling log requests from the logging handler */
	protected final Logger log;
	/** The async execution handler */
	protected final ExecutionHandler executionHandler;
	/** The JMX Op Response Encoder */
	protected final JMXResponseEncoder responseEncoder;
	/** New connection handler */
	protected final ChannelUpstreamHandler connHandler;
	/** The JMX invocation handler */
	protected final JMXMBeanServerInvocationHandler jmxInvocationHandler;
	/** Indicates if this is an SSL server or not */
	protected final boolean SSL;
	
	// ==================================================================================================================
	//		SSL Server Constructs
	// ==================================================================================================================	
	/** The SSL engine */
	protected final SSLEngine sslEngine;
	/** The SSL buffer pool */
	protected final SslBufferPool sslBufferPool;
	/** The SSL negotiation timeout timer */
	protected final Timer sslTimer;
	/** The SSL start tls flag which is true if the first write request shouldn't be encrypted by the SSLEngine */
	protected final boolean startTls;
	/** The SSL handshake delegation executor */
	protected final ThreadPoolExecutor sslExecutor;
	/** The SSL handshake timeout in ms. */
	protected final long handshakeTimeoutInMillis;
	
	
	// ==================================================================================================================
	//		Logger Config Names
	// ==================================================================================================================
	
	/** A logging handler config with a level of INFO and hex enabled */
	public static final String LOGGER_HEX_INFO = "HexInfo";
	/** A logging handler config with a level of DEBUG and hex enabled */
	public static final String LOGGER_HEX_DEBUG = "HexDebug";
	/** A logging handler config with a level of INFO and hex disabled */
	public static final String LOGGER_INFO = "Info";
	/** A logging handler config with a level of DEBUG and hex disabled */
	public static final String LOGGER_DEBUG = "Debug";
	/** A no logging loggin handler config name */
	public static final String LOGGER_NULL = "NoLogger";
	/** The default logger name */
	public static final String DEFAULT_LOGGER = LOGGER_DEBUG;

	
	// ==================================================================================================================
	//		Handler Binding Names
	// ==================================================================================================================
	
	/** The name that SSL handlers are registered with the pipeline under */
	public static final String SSL_HANDLER_NAME = "ssl";	
	/** The name that the currently configured logging handler is registered with the pipeline under */
	public static final String LOGGING_HANDLER_NAME = "logging";
	/** The name that the execution handler is registered with the pipeline under */
	public static final String EXEC_HANDLER_NAME = "exec";
	/** The name that JMX Op Decoding Handlers are registered with the pipeline under */
	public static final String JMXOPDECODE_HANDLER_NAME = "jmxopdecode";	
	/** The name that the JMX Server Invocation Handler is registered with the pipeline under */
	public static final String JMXINVOCATION_HANDLER_NAME = "jmxinvoker";	
	/** The name that the JMX Response Encoder is registered with the pipeline under */
	public static final String JMXRERSPONSE_ENCODER_NAME = "jmxresenconder";
	/** The name that the connectiuon handler is registered with the pipeline under */
	public static final String CONN_HANDLER_NAME = "connhandler";	
	
	
	// ==================================================================================================================
	//		Server Names
	// ==================================================================================================================	
	
	/** The server name when using plain TCP sockets */
	public static final String TCP_SERVER_NAME = "ajmx";
	/** The server name when using SSL TCP sockets */
	public static final String SSL_SERVER_NAME = "ajmxs";
	
	/**
	 * Creates a new standard TCP socket JMXServerPipelineFactory
	 */
	JMXServerPipelineFactory(ChannelUpstreamHandler connHandler) {
		this(connHandler, false, -1L);
	}
	
	/**
	 * Creates a new SSL/TCP JMXServerPipelineFactory
	 * @param startTls true if the first write request shouldn't be encrypted by the SSLEngine
	 * @param handshakeTimeoutInMillis the time in milliseconds after whic the handshake() will be failed, and so the future notified
	 */
	JMXServerPipelineFactory(ChannelUpstreamHandler connHandler, boolean startTls, long handshakeTimeoutInMillis) {
		SSL = handshakeTimeoutInMillis >= 0;
		this.handshakeTimeoutInMillis = handshakeTimeoutInMillis;
		LOG_NAME = SSL ? SSL_SERVER_NAME : TCP_SERVER_NAME;
		log = Logger.getLogger(LOG_NAME);
		loggingHandlers = initLogHandlers();
		executionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576));
		responseEncoder = new JMXResponseEncoder();
		jmxInvocationHandler = JMXMBeanServerInvocationHandler.getInstance();
		this.connHandler = connHandler;
		if(SSL) {
			sslEngine = SecureJMXSslContextFactory.getServerContext().createSSLEngine();
			sslEngine.setUseClientMode(false);			
			sslBufferPool = new SslBufferPool();
			sslTimer = new HashedWheelTimer();
			this.startTls = startTls;
			sslExecutor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		} else {
			sslEngine = null;
			sslBufferPool = null;
			sslTimer = null;
			this.startTls = false;
			sslExecutor = null;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();		
		if(SSL) {
			pipeline.addLast(SSL_HANDLER_NAME, new SslHandler(sslEngine, sslBufferPool, startTls, sslExecutor, sslTimer, handshakeTimeoutInMillis));
		}
		LoggingHandler loggingHandler = loggingHandlers.get(loggingConfigName.get());		
		if(loggingHandler!=null) {
			pipeline.addLast(LOGGING_HANDLER_NAME, loggingHandler);
		}
		pipeline.addLast(CONN_HANDLER_NAME, connHandler);
		pipeline.addLast(EXEC_HANDLER_NAME, executionHandler);
		pipeline.addLast(JMXOPDECODE_HANDLER_NAME, new JMXOpDecoder2());
		pipeline.addLast(JMXRERSPONSE_ENCODER_NAME, this.responseEncoder);
		pipeline.addLast(JMXINVOCATION_HANDLER_NAME, jmxInvocationHandler);
		// <-----  and back down again ------ >
		
		return pipeline;
	}

	/**
	 * Initializes and returns a map of logging handlers that can be installed in the pipeline
	 * @return a map of logging handlers 
	 */
	protected Map<String, LoggingHandler> initLogHandlers() {
		Map<String, LoggingHandler> map = new HashMap<String, LoggingHandler>();
		map.put(LOGGER_HEX_INFO, new LoggingHandler(LOG_NAME, InternalLogLevel.INFO, true));
		map.put(LOGGER_HEX_DEBUG, new LoggingHandler(LOG_NAME, InternalLogLevel.DEBUG, true));
		map.put(LOGGER_INFO, new LoggingHandler(LOG_NAME, InternalLogLevel.INFO, false));
		map.put(LOGGER_DEBUG, new LoggingHandler(LOG_NAME, InternalLogLevel.DEBUG, false));		
		map.put(LOGGER_NULL, null);
		return Collections.unmodifiableMap(map);
	}
	
}
