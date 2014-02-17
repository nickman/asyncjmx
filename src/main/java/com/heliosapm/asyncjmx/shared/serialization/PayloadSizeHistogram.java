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
package com.heliosapm.asyncjmx.shared.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.heliosapm.asyncjmx.shared.JMXOp;
import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.util.ConfigurationHelper;
import com.heliosapm.asyncjmx.unsafe.collections.ConcurrentLongSlidingWindow;

/**
 * <p>Title: PayloadSizeHistogram</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.serialization.PayloadSizeHistogram</code></p>
 * @param <T> The assumed type of the histogram index
 */

public class PayloadSizeHistogram<T> {
	/** A histogram of JMXOpCode serialization sizes */
	protected Map<T, ConcurrentLongSlidingWindow> opSizeHistory = new ConcurrentHashMap<T, ConcurrentLongSlidingWindow>();
	
	/** The sampling size, or the maximum number of samples kept to sample from */
	protected final int samplingSize;
	/** The percentile of the samples to estimate a size from */
	protected final int samplingPercentile;
	/** The default size to estimate if insufficient samples have been recorded */
	protected final int defaultSize;

	/** The configuration name for the sampling size of histograms */
	public static final String CONFIG_HISTOGRAM_SIZE = "com.heliosapm.asyncjmx.histogram.size";	
	/** The default sampling size for JMXOp serialization sizes histograms */
	public static final int DEFAULT_HISTOGRAM_SIZE = 128;
	/** The configuration name for the histogram percentile */
	public static final String CONFIG_HISTOGRAM_PERCENTILE = "com.heliosapm.asyncjmx.histogram.percentile";	
	/** The default percentile to calculate estimated sizes with */
	public static final int DEFAULT_HISTOGRAM_PERCENTILE = 90;
	/** The configuration name for the byte size estimate if the histogram has less than 3 samples */
	public static final String CONFIG_DEFAULT_SIZE_ESTIMATE = "com.heliosapm.asyncjmx.histogram.defaultestimate";		
	/** The default byte size estimate if the histogram has less than 3 samples */
	public static final int DEFAULT_SIZE_ESTIMATE = 128;
	
	/**
	 * Creates a new PayloadSizeHistogram
	 * @param samplingSize The sampling size, or the maximum number of samples kept to sample from 
	 * @param samplingPercentile The percentile of the samples to estimate a size from
	 * @param defaultSize The default size to estimate if insufficient samples have been recorded
	 */
	public PayloadSizeHistogram(int samplingSize, int samplingPercentile, int defaultSize) {
		this.samplingSize = samplingSize;
		this.samplingPercentile = samplingPercentile;
		this.defaultSize = defaultSize;
	}
	
	/**
	 * Creates a new PayloadSizeHistogram using the system-property/environmental-variable configured parameters
	 * and/or the defaults.
	 */
	public PayloadSizeHistogram() {
		this(
				ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_HISTOGRAM_SIZE, DEFAULT_HISTOGRAM_SIZE),
				ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_HISTOGRAM_PERCENTILE, DEFAULT_HISTOGRAM_PERCENTILE),
				ConfigurationHelper.getIntSystemThenEnvProperty(CONFIG_DEFAULT_SIZE_ESTIMATE, DEFAULT_SIZE_ESTIMATE)
		);
	}
	
	
	/**
	 * Acquires the histogram for the passed histogram key provider's key
	 * @param keyProvider The HistogramKeyProvider to get the histogram for
	 * @return the histogram
	 */
	protected ConcurrentLongSlidingWindow getJMXOpHistogram(HistogramKeyProvider<T> keyProvider) {
		final T histogramKey = keyProvider.getHistogramKey();		
		ConcurrentLongSlidingWindow csw = opSizeHistory.get(keyProvider.getHistogramKey());
		if(csw==null) {
			synchronized(opSizeHistory) {
				csw = opSizeHistory.get(histogramKey);
				if(csw==null) {
					csw = new ConcurrentLongSlidingWindow(DEFAULT_HISTOGRAM_SIZE);
					opSizeHistory.put(histogramKey, csw);
				}
			}
		}
		return csw;
	}
	
	
	/**
	 * Adds a new sampling to the histogram
	 * @param keyProvider The HistogramKeyProvider to get the histogram for
	 * @param size The sampled size in bytes 
	 */
	public void sample(HistogramKeyProvider<T> keyProvider, int size) {
		getJMXOpHistogram(keyProvider).insert(size);
	}
	
	/**
	 * Estimates the size of the payload
	 * @param keyProvider The HistogramKeyProvider to get the histogram for
	 * @return the estimated size in bytes
	 */
	public int estimateSize(HistogramKeyProvider<T> keyProvider) {
		final T histogramKey = keyProvider.getHistogramKey();
		if(histogramKey==null || Void.class.isInstance(histogramKey)) return keyProvider.getVoidHistogramSize();
		ConcurrentLongSlidingWindow csw = getJMXOpHistogram(keyProvider);
		if(csw.size() < 3) return DEFAULT_SIZE_ESTIMATE;
		return (int)csw.percentile(DEFAULT_HISTOGRAM_PERCENTILE);
	}

}
