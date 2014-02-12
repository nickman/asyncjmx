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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Title: JMXLogger</p>
 * <p>Description: Wraps a {@link java.util.logging.Logger} and provides some useful overloads</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.logging.JMXLogger</code></p>
 */

public class JMXLogger {
	/** The wrapped log */
	protected final Logger log;
	
	/** The platform end-of-line sequence */
	public static final String EOL = System.getProperty("line.separator", "\n");
	
	/** A map of created JMXLoggers keyed by the logger name */
	private static final Map<String, JMXLogger> jmxLoggers = new ConcurrentHashMap<String, JMXLogger>();
	
	/**
	 * Returns a JMXLogger for the passed name
	 * @param name The name of the logger to get
	 * @return the JMXLogger for the passed name
	 */
	public static JMXLogger getLogger(String name) {
		JMXLogger logger = jmxLoggers.get(name);
		if(logger==null) {
			synchronized(jmxLoggers) {
				logger = jmxLoggers.get(name);
				if(logger==null) {
					logger = new JMXLogger(name);
				}
			}
		}
		return logger;
	}

	/**
	 * Returns a JMXLogger for the passed class
	 * @param clazz The class of the logger to get
	 * @return the JMXLogger for the passed class
	 */
	public static JMXLogger getLogger(Class<?> clazz) {
		return new JMXLogger(clazz.getName());
	}
	
	/**
	 * Creates a new JMXLogger
	 * @param name The name of the wrapped logger
	 */
	public JMXLogger(String name) {
		log = Logger.getLogger(name);
	}
	
	/**
	 * Creates a new JMXLogger
	 * @param clazz The class the wrapped logger is logging for
	 */
	public JMXLogger(Class<?> clazz) {
		this(clazz.getName());
	}
	
	/**
	 * Logs a formatted message if the logger is enabled for {@link Level#INFO}.
	 * @param format The format specifier for the message
	 * @param args The arguments to populate the message format
	 */
	public void info(String format, Object...args) {
		log(Level.INFO, format, args);
	}

	/**
	 * Logs a formatted message if the logger is enabled for {@link Level#WARNING}.
	 * @param format The format specifier for the message
	 * @param args The arguments to populate the message format
	 */
	public void warn(String format, Object...args) {
		log(Level.WARNING, format, args);
	}

	/**
	 * Logs a formatted message if the logger is enabled for {@link Level#SEVERE}.
	 * @param format The format specifier for the message
	 * @param args The arguments to populate the message format
	 */
	public void error(String format, Object...args) {
		log(Level.SEVERE, format, args);
	}
	
	/**
	 * Logs a formatted message if the logger is enabled for {@link Level#FINER}.
	 * @param format The format specifier for the message
	 * @param args The arguments to populate the message format
	 */
	public void debug(String format, Object...args) {
		log(Level.FINER, format, args);
	}
	
	/**
	 * Logs a formatted message if the logger is enabled for {@link Level#FINEST}.
	 * @param format The format specifier for the message
	 * @param args The arguments to populate the message format
	 */
	public void trace(String format, Object...args) {
		log(Level.FINEST, format, args);
	}
	
	/**
	 * Indicates if the logger is enabled for {@link Level#FINER}
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabledForDebug() {
		return log.isLoggable(Level.FINER);
	}
	
	/**
	 * Indicates if the logger is enabled for {@link Level#FINEST}
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabledForTrace() {
		return log.isLoggable(Level.FINEST);
	}
	
	
	/**
	 * Logs a message created using the passed format and arguments.
	 * If the last argument is a {@link Throwable}, an extra <b><code>:%s</code></b> is appended to the format
	 * and the argument is replaced with a formatted stack trace of the throwable
	 * @param level The level to log at. If the associated logger is not enabled for this level, the call is a No Op.
	 * @param format The format specified for the log message
	 * @param args The arguments to fill in the mesage format
	 */
	public void log(Level level, String format, Object...args) {
		if(!log.isLoggable(level)) return;
		boolean hasThrowable = false;
		if(args!=null && args.length>0) {
			Object last = args[args.length-1];
			if(last!=null && last instanceof Throwable) {
				hasThrowable = true;
				format = format + ":%s";
				args[args.length-1] = stackTrace((Throwable)last);
			}
		}		
		log.log(level, String.format(format, args));		
	}
	
	/**
	 * Returns a throwable's stack trace as a string
	 * @param t The throwable to print
	 * @return the stack trace as a string
	 */
	public static String stackTrace(Throwable t) {
		if(t==null) return "";
		StringWriter sw = new StringWriter(1024);
		sw.append(EOL).append(t.getClass().getName()).append(":").append(EOL);
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
}