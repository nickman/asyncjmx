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
package com.heliosapm.asyncjmx.client;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

/**
 * <p>Title: JMXClientPipelineFactory</p>
 * <p>Description: The netty channel pipeline factory for the JMX client</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.JMXClientPipelineFactory</code></p>
 */

public class JMXClientPipelineFactory implements ChannelPipelineFactory {
	/** The JMX Op enconding initiating handler */
	protected final JMXOpEncoder opEncoder = new JMXOpEncoder();
	/** The new connection handler */
	protected final ChannelUpstreamHandler connectionHandler;

	
	//===========================================================================================
	//		Channel Handler Names
	//===========================================================================================
	/** The new connection handler  */
	public static final String NEWCONN_HANDLER = "connHandler";
	/** The JMX Op Encoder  */
	public static final String JMXOP_ENCODER = "opEncoder";
	/** The JMX Response Decoder  */
	public static final String JMXOP_DECODER = "opDecoder";
	
	
	/**
	 * Creates a new JMXClientPipelineFactory
	 * @param connectionHandler The new connection handler
	 */
	JMXClientPipelineFactory(ChannelUpstreamHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("log", new LoggingHandler(InternalLogLevel.INFO, true));
		pipeline.addLast(NEWCONN_HANDLER, connectionHandler);
		pipeline.addLast(JMXOP_ENCODER, opEncoder);
		pipeline.addLast(JMXOP_DECODER, new JMXResponseDecoder());
		return pipeline;
	}

}
