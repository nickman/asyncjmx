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
package com.heliosapm.asyncjmx.shared.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.heliosapm.asyncjmx.client.JMXOpResponse;
import com.heliosapm.asyncjmx.unsafe.UnsafeAdapter;

/**
 * <p>Title: IndexedBlockingResultQueue</p>
 * <p>Description: A blocking map style construct for retrieving synch results</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.shared.util.IndexedBlockingResultQueue</code></p>
 */

public class IndexedBlockingResultQueue {
	/** A map of latches keyed by the request id */
	protected final Map<Integer, IndexClearingCountDownLatch> latches = new ConcurrentHashMap<Integer, IndexClearingCountDownLatch>();
	/** A map of JMX responses keyed by the request id */
	protected final Map<Integer, JMXOpResponse> results = new ConcurrentHashMap<Integer, JMXOpResponse>();
	/** A map of JMX response exceptions keyed by the request id */
	protected final Map<Integer, Throwable> errors = new ConcurrentHashMap<Integer, Throwable>();
	
	/** The default timeout in ms. */
	protected final long timeout;
	
	/**
	 * Creates a new IndexedBlockingResultQueue
	 * @param timeout The default timeout in ms.
	 */
	public IndexedBlockingResultQueue(long timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * Waits for the JMXOpResponse with the passed request id using the default timeout
	 * @param requestId The request id of the response we're waiting for 
	 * @return The JMXOpResponse
	 * @throws InterruptedException Thrown if the waiting thread is interrupted while wating
	 */
	public JMXOpResponse registerAndWait(int requestId) throws InterruptedException {
		return registerAndWait(requestId, timeout);
	}
	
	/**
	 * Deposits a response and drops the waiting latch
	 * @param response The response
	 */
	public void depositResponse(JMXOpResponse response) {
		IndexClearingCountDownLatch latch = latches.get(response.getRequestId());
		if(latch!=null) {
			results.put(response.getRequestId(), response);
			latch.countDown();
		}
	}

	/**
	 * Deposits an exception and drops the waiting latch
	 * @param requestId The request id the error is associated to 
	 * @param error The exception
	 */
	public void depositResponse(int requestId, Throwable error) {
		IndexClearingCountDownLatch latch = latches.get(requestId);
		if(latch!=null) {
			errors.put(requestId, error);
			latch.countDown();
		}
	}

	/**
	 * Waits for the JMXOpResponse with the passed request id
	 * @param requestId The request id of the response we're waiting for 
	 * @param timeout The timeout in ms.
	 * @return The JMXOpResponse
	 * @throws InterruptedException Thrown if the waiting thread is interrupted while wating
	 */
	public JMXOpResponse registerAndWait(int requestId, long timeout) throws InterruptedException {
		IndexClearingCountDownLatch latch = new IndexClearingCountDownLatch(requestId);
		latches.put(requestId, latch);
		try {
			if(latch.await(timeout, TimeUnit.MILLISECONDS)) {
				JMXOpResponse resp = results.remove(requestId);
				if(resp!=null) {
					return resp;					
				} else {
					Throwable t = errors.remove(requestId);
					if(t!=null) {
						UnsafeAdapter.throwException(t);
					}
				}
			}
			throw new RuntimeException("Timeout");			
		} catch (InterruptedException iex) {
			latches.remove(requestId);
			results.remove(requestId);			
			throw iex;
		}
	}
	
	protected class IndexClearingCountDownLatch extends CountDownLatch {
		/** The id of the index to clear on timeout */
		private final int id;
		
		/**
		 * Creates a new IndexClearingCountDownLatch
		 * @param id The id of the index to clear on timeout
		 */
		public IndexClearingCountDownLatch(int id) {
			super(1);
			this.id = id;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.CountDownLatch#await(long, java.util.concurrent.TimeUnit)
		 */
		@Override
		public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
			try {
				final boolean onTime = super.await(timeout, unit);
				latches.remove(id);						
				return onTime;
			} catch (InterruptedException iex) {
				latches.remove(id);
				results.remove(id);
				throw iex;
			}
		}
		
	}
}
