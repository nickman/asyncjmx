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
package com.heliosapm.asyncjmx.shared.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * <p>Title: JMXLogFormatter</p>
 * <p>Description: The JMXLogger log record formatter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.logging.JMXLogFormatter</code></p>
 */

public class JMXLogFormatter extends Formatter {
	/** The platform end-of-line sequence */
	public static final String EOL = System.getProperty("line.separator", "\n");
	/** The max length of the level names */
	public static final int MAX_LEVEL_LENGTH = 7;
	private static final String LEVEL_FIELD = "       ";
	/** A thread local for timestamp formatters */
	private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>(){
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(JMXLogger.DATE_FORMAT);
		}
	};
	
	/** A thread local for pre-allocated stringbuilers */
	private static final ThreadLocal<StringBuilder> stringBuilder = new ThreadLocal<StringBuilder>(){
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder(128);
		}
	};
	
	/** A thread local for pre-allocated level format*/
	private static final ThreadLocal<StringBuilder> leveFormat = new ThreadLocal<StringBuilder>(){
		@Override
		protected StringBuilder initialValue() {
			return new StringBuilder(LEVEL_FIELD);
		}
	};
	
	/** A map of shortened logger names keyed by the full name */
	private static final Map<String, String> shortenedLoggerNames = new ConcurrentHashMap<String, String>();
	/** A dot pattern for splitting logger names */
	private static final Pattern DOT = Pattern.compile("\\.");
	
	/**
	 * Returns the current thread's allocated string builder with its length set to zero
	 * @return the current thread's allocated string builder
	 */
	private static StringBuilder builder() {
		StringBuilder sb = stringBuilder.get();
		sb.setLength(0);
		return sb;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.logging.SimpleFormatter#format(java.util.logging.LogRecord)
	 */
	@Override
	public synchronized String format(LogRecord record) {
		StringBuilder b = builder();
		String ts = dateFormat.get().format(new Date(record.getMillis()));
		try {
			return b.append(formatLevel(record.getLevel())).append(" - ")
			.append("[").append(ts).append(" / ").append(Thread.currentThread().getName())
			.append(" (").append(shorten(record.getLoggerName())).append(")").append("]")
			.append(" >|").append(record.getMessage()).append(EOL).toString();
		} finally {
			b.setLength(0);
		}
	}
	
	
	/**
	 * Shortens the passed logger name such that <b><code>java.util.Collection</code></b> 
	 * would be shortened to <b><code>j.u.Collection</code></b>.
	 * @param loggerName The logger name to shorten
	 * @return the shortened logger name
	 */
	public static String shorten(String loggerName) {
		String shortened = shortenedLoggerNames.get(loggerName);
		if(shortened==null) {
			synchronized(shortenedLoggerNames) {
				shortened = shortenedLoggerNames.get(loggerName);
				if(shortened==null) {
					int index = loggerName.indexOf('.');
					if(index==-1) {
						shortened = loggerName;						
					} else {
						String[] parts = DOT.split(loggerName);
						StringBuilder b = new StringBuilder();
						for(int i = 0; i < parts.length-1; i++) {
							b.append(parts[i].substring(0, 1)).append(".");
						}
						b.append(parts[parts.length-1]);
						shortened = b.toString();
					}					
					shortenedLoggerNames.put(loggerName, shortened);
				}
			}
		}
		return shortened;
	}
	
	/**
	 * Returns a space right-padded string of the name of the passed level
	 * @param level The level to format
	 * @return the formatted level name
	 */
	public static String formatLevel(Level level) {
		String name = level.getName();
		return leveFormat.get().replace(0, MAX_LEVEL_LENGTH, LEVEL_FIELD).replace(0, name.length(), name).toString();
	}

}
