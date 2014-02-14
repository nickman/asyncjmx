/**
 * 
 */
package asyncjmx.base;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;

import javax.management.MBeanServer;

import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.JdkLoggerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.heliosapm.asyncjmx.client.AsyncJMXClient;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;

/**
 * <p>Title: BaseTest</p>
 * <p>Description:  Base test class</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>asyncjmx.base.BaseTest</code></b>
 */

@Ignore
public class BaseTest extends Assert {
	
	/** The platform MBeanServer */
	public static final MBeanServer localServer = ManagementFactory.getPlatformMBeanServer();
	
	/** The currently executing test name */
	@Rule public final TestName name = new TestName();
	
	/** Accumulator for error messages */
	protected static final Map<Integer, String> exceptionMessageAccumulator = new TreeMap<Integer, String>();
	
	/** The exception counter for IAssert */
	protected static final AtomicInteger exceptionCounter = new AtomicInteger(0);
	
	/** A random value generator */
	protected static final Random RANDOM = new Random(System.currentTimeMillis());
	
	/**
	 * Returns a random positive long
	 * @return a random positive long
	 */
	protected static long nextPosLong() {
		return Math.abs(RANDOM.nextLong());
	}
	
	/**
	 * Returns a random positive int
	 * @return a random positive int
	 */
	protected static int nextPosInt() {
		return Math.abs(RANDOM.nextInt());
	}
	
	/**
	 * Returns a random positive int within the bound
	 * @param bound the bound on the random number to be returned. Must be positive. 
	 * @return a random positive int
	 */
	protected static int nextPosInt(int bound) {
		return Math.abs(RANDOM.nextInt(bound));
	}
	
	
	
	/**
	 * Prints the test name about to be executed
	 */
	@Before
	public void printTestName() {
		log("\n\t==================================\n\tRunning Test [" + name.getMethodName() + "]\n\t==================================\n");
	}
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		/* No Op */
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		/* No Op */
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		exceptionMessageAccumulator.clear();
		exceptionCounter.set(0);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		if(!exceptionMessageAccumulator.isEmpty()) {
			StringBuilder b = new StringBuilder("Accumulated exceptions reported in [" +  name.getMethodName() + "]:");
			for(String s: exceptionMessageAccumulator.values()) {
				b.append("\n").append(s);
			}
			throw new AssertionError(b);
		}		
	}
	
	/**
	 * Out printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	/**
	 * Err printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void loge(String fmt, Object...args) {
		System.err.print(String.format(fmt, args));
		if(args!=null && args.length>0 && args[0] instanceof Throwable) {
			System.err.println("  Stack trace follows:");
			((Throwable)args[0]).printStackTrace(System.err);
		} else {
			System.err.println("");
		}
	}
	
	public static class ByteArrayOutput extends Output {
		protected final ByteArrayOutputStream baos;
		public ByteArrayOutput() {
			super(new ByteArrayOutputStream(1024));
			baos = (ByteArrayOutputStream)this.outputStream;
		}
		
		public byte[] getBytes() {
			return baos.toByteArray();
		}
	}
	
	public static class ByteArrayInput extends Input {
		
		public ByteArrayInput(byte[] bytes) {
			super(bytes);
		}
		
		public int getBytesRead() {
			return this.position();
		}
		
	}
	
	
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
	
	
}	

