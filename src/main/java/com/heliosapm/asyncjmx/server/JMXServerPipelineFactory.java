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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

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
	 * JMX Invoker Handler
	 */
	
	/** A map of a few differently configured logging handlers that can be activated via JMX */
	protected final Map<String, LoggingHandler> loggingHandlers;
	/** The currently installed logging handler config */
	protected final AtomicReference<String> loggingConfigName = new AtomicReference<String>(DEFAULT_LOGGER);
	/** Instance logger for handling log requests from the logging handler */
	protected final Logger log = Logger.getLogger(LOG_NAME);
	
	/** The logger name of the installed logging handler */
	public static final String LOG_NAME = "AJMX";
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
	/** The name that the currently configured logging handler is registered with the pipeline under */
	public static final String LOGGING_HANDLER_NAME = "logging";
	
	JMXServerPipelineFactory() {
		loggingHandlers = initLogHandlers();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		
		LoggingHandler loggingHandler = loggingHandlers.get(loggingConfigName.get());
		if(loggingHandler!=null) {
			pipeline.addLast(LOGGING_HANDLER_NAME, loggingHandler);
		}
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
