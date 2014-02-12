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
package com.heliosapm.asyncjmx.client;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;

import com.heliosapm.asyncjmx.shared.JMXOpCode;
import com.heliosapm.asyncjmx.shared.logging.JMXLogger;
import com.heliosapm.asyncjmx.shared.serialization.NullResult;
import com.heliosapm.asyncjmx.shared.serialization.PlaceHolder;
import com.heliosapm.asyncjmx.shared.serialization.VoidResult;
import com.heliosapm.asyncjmx.unsafe.UnsafeAdapter;

/**
 * <p>Title: SyncMBeanServerConnection</p>
 * <p>Description: A synchronous MBeanServerConnection implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.asyncjmx.client.SyncMBeanServerConnection</code></p>
 */

public class SyncMBeanServerConnection implements MBeanServerConnection, ChannelUpstreamHandler {
	/** The connection to the JMX server */
	protected final Channel channel;
	/** The timeout for JMX invocations in ms */
	protected final long timeout;
	/** The request id serial number factory */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** The synchnonous queue on which the requesting thread waits on a response */
	protected final SynchronousQueue<Object> timeoutQueue = new SynchronousQueue<Object>();
	/** The current rid we're waiting on */
	protected final AtomicInteger currentRid = new AtomicInteger(0);
	/** Instance logger */
	protected final JMXLogger log = JMXLogger.getLogger(getClass());
	/** The name of the handler in the pipeline */
	public static final String RESPONSE_HANDLER_NAME = "responseHandler";
	
	/**
	 * Creates a new AsyncMBeanServerConnection
	 * @param channel The netty channel connection to the JMX server
	 * @param timeout The timeout in ms.
	 */
	public SyncMBeanServerConnection(Channel channel, long timeout) {
		this.channel = channel;
		this.timeout = timeout;
		this.channel.getPipeline().addLast(RESPONSE_HANDLER_NAME, this);
	}
	
	/**
	 * Writes the JMX invocation request to the remote server to invoke a void return operation
	 * @param opCode The op code for the jmx operation being invoked
	 * @param args The arguments to the invocation
	 */
	protected void writeRequest(JMXOpCode opCode, Object...args) {
		writeRequest(VoidResult.class, opCode, args);
	}
	
	
	/**
	 * Writes the JMX invocation request to the remote server
	 * @param returnType The expected return type, defaults to {@link VoidResult} if null.
	 * @param opCode The op code for the jmx operation being invoked
	 * @param args The arguments to the invocation
	 * @return the response to the request if sync, or null if async
	 */
	@SuppressWarnings("unchecked")
	protected <T> T writeRequest(Class<T> returnType, final JMXOpCode opCode, Object...args) {
		log.info("\n\t****\n\tCalling [%s]\n\t****", opCode.name());
		if(returnType==null) returnType = (Class<T>) VoidResult.class;
		int rId = serial.incrementAndGet();
		currentRid.set(rId);
		for(int i = 0; i < args.length; i++) {
			if(args[i]==null) args[i] = NullResult.Instance;
		}
		channel.write(new Object[] {opCode.opCode, rId, args}).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				log.info("Write for op [%s] complete", opCode.name());
			}
		});
		Object retValue = null;
		try {
			retValue = timeoutQueue.poll(timeout, TimeUnit.MILLISECONDS);
			if(retValue instanceof PlaceHolder) return null;
			if(retValue instanceof Throwable) {
				UnsafeAdapter.throwException((Throwable)retValue);
			}
			return (T)retValue;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e!=null && e instanceof UpstreamMessageEvent) {
			Object obj = ((UpstreamMessageEvent)e).getMessage();
			try {
				if(obj!=null) {
					log.info("Received Upstream Message Event [%s]", obj.getClass().getName());
					if(obj instanceof JMXOpResponse) {
						Object response = ((JMXOpResponse)obj).getResponse();
						if(response!=null) {
							timeoutQueue.add(response);
						} else {
							timeoutQueue.add(NullResult.Instance);
						}
					} else if(obj instanceof JMXCallback) {
						log.info(obj.toString());
					} else {
						timeoutQueue.add(new Exception("Unexpected internal type returned [" + obj + "]"));
					}					
				} else {
					timeoutQueue.add(NullResult.Instance);
				}
			} catch (IllegalStateException ise) {
				// nobody listening....
			}
		}		
	}
	
	//===============================================================================================================================
	//===============================================================================================================================
	//		MBeanServerConnection Method Impls
	//===============================================================================================================================
	//===============================================================================================================================
	
	
	

    /**
     * <p>Instantiates and registers an MBean in the MBean server.  The
     * MBean server will use its {@link
     * javax.management.loading.ClassLoaderRepository Default Loader
     * Repository} to load the class of the MBean.  An object name is
     * associated to the MBean.	 If the object name given is null, the
     * MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.</p>
     *
     * <p>This method is equivalent to {@link
     * #createMBean(String,ObjectName,Object[],String[])
     * createMBean(className, name, (Object[]) null, (String[])
     * null)}.</p>
     *
     * @param className The class name of the MBean to be instantiated.	   
     * @param name The object name of the MBean. May be null.	 
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred
     * when trying to invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
    	return writeRequest(ObjectInstance.class, JMXOpCode.CREATEMBEAN_SO, className, name);
    }

    /**
     * <p>Instantiates and registers an MBean in the MBean server.  The
     * class loader to be used is identified by its object name. An
     * object name is associated to the MBean. If the object name of
     * the loader is null, the ClassLoader that loaded the MBean
     * server will be used.  If the MBean's object name given is null,
     * the MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.</p>
     *
     * <p>This method is equivalent to {@link
     * #createMBean(String,ObjectName,ObjectName,Object[],String[])
     * createMBean(className, name, loaderName, (Object[]) null,
     * (String[]) null)}.</p>
     *
     * @param className The class name of the MBean to be instantiated.	   
     * @param name The object name of the MBean. May be null.	 
     * @param loaderName The object name of the class loader to be used.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred when trying to
     * invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception InstanceNotFoundException The specified class loader
     * is not registered in the MBean server.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public ObjectInstance createMBean(String className, ObjectName name,
				      ObjectName loaderName) 
	    throws ReflectionException, InstanceAlreadyExistsException,
		   MBeanRegistrationException, MBeanException,
		   NotCompliantMBeanException, InstanceNotFoundException,
		   IOException {
    	return writeRequest(ObjectInstance.class, JMXOpCode.CREATEMBEAN_SOO, className, name);
    }


    /**
     * Instantiates and registers an MBean in the MBean server.  The
     * MBean server will use its {@link
     * javax.management.loading.ClassLoaderRepository Default Loader
     * Repository} to load the class of the MBean.  An object name is
     * associated to the MBean.  If the object name given is null, the
     * MBean must provide its own name by implementing the {@link
     * javax.management.MBeanRegistration MBeanRegistration} interface
     * and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred when trying to
     * invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name,
				      Object params[], String signature[]) 
	    throws ReflectionException, InstanceAlreadyExistsException,
	    	   MBeanRegistrationException, MBeanException,
	    	   NotCompliantMBeanException, IOException {
    	return writeRequest(ObjectInstance.class, JMXOpCode.CREATEMBEAN_SOOS, className, name);
    }

    /**
     * Instantiates and registers an MBean in the MBean server.  The
     * class loader to be used is identified by its object name. An
     * object name is associated to the MBean. If the object name of
     * the loader is not specified, the ClassLoader that loaded the
     * MBean server will be used.  If the MBean object name given is
     * null, the MBean must provide its own name by implementing the
     * {@link javax.management.MBeanRegistration MBeanRegistration}
     * interface and returning the name from the {@link
     * MBeanRegistration#preRegister preRegister} method.
     *
     * @param className The class name of the MBean to be instantiated.
     * @param name The object name of the MBean. May be null.
     * @param params An array containing the parameters of the
     * constructor to be invoked.
     * @param signature An array containing the signature of the
     * constructor to be invoked.
     * @param loaderName The object name of the class loader to be used.
     *
     * @return An <CODE>ObjectInstance</CODE>, containing the
     * <CODE>ObjectName</CODE> and the Java class name of the newly
     * instantiated MBean.  If the contained <code>ObjectName</code>
     * is <code>n</code>, the contained Java class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(n)}.getClassName()</code>.
     *
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.ClassNotFoundException</CODE> or a
     * <CODE>java.lang.Exception</CODE> that occurred when trying to
     * invoke the MBean's constructor.
     * @exception InstanceAlreadyExistsException The MBean is already
     * under the control of the MBean server.
     * @exception MBeanRegistrationException The
     * <CODE>preRegister</CODE> (<CODE>MBeanRegistration</CODE>
     * interface) method of the MBean has thrown an exception. The
     * MBean will not be registered.
     * @exception MBeanException The constructor of the MBean has
     * thrown an exception
     * @exception NotCompliantMBeanException This class is not a JMX
     * compliant MBean
     * @exception InstanceNotFoundException The specified class loader
     * is not registered in the MBean server.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The className
     * passed in parameter is null, the <CODE>ObjectName</CODE> passed
     * in parameter contains a pattern or no <CODE>ObjectName</CODE>
     * is specified for the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     */
    public ObjectInstance createMBean(String className, ObjectName name,
				      ObjectName loaderName, Object params[],
				      String signature[]) 
	    throws ReflectionException, InstanceAlreadyExistsException,
	    	   MBeanRegistrationException, MBeanException,
	    	   NotCompliantMBeanException, InstanceNotFoundException,
	    	   IOException {
    	return writeRequest(ObjectInstance.class, JMXOpCode.CREATEMBEAN_SOOOS, className, name);
    }

    /**
     * Unregisters an MBean from the MBean server. The MBean is
     * identified by its object name. Once the method has been
     * invoked, the MBean may no longer be accessed by its object
     * name.
     *
     * @param name The object name of the MBean to be unregistered.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception MBeanRegistrationException The preDeregister
     * ((<CODE>MBeanRegistration</CODE> interface) method of the MBean
     * has thrown an exception.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or the MBean you are when trying to
     * unregister is the {@link javax.management.MBeanServerDelegate
     * MBeanServerDelegate} MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     */
    public void unregisterMBean(ObjectName name)
	    throws InstanceNotFoundException, MBeanRegistrationException,
	    	   IOException {
    	writeRequest(ObjectInstance.class, JMXOpCode.UNREGISTERMBEAN, name);
    }

    /**
     * Gets the <CODE>ObjectInstance</CODE> for a given MBean
     * registered with the MBean server.
     *
     * @param name The object name of the MBean.
     *
     * @return The <CODE>ObjectInstance</CODE> associated with the MBean
     * specified by <VAR>name</VAR>.  The contained <code>ObjectName</code>
     * is <code>name</code> and the contained class name is
     * <code>{@link #getMBeanInfo getMBeanInfo(name)}.getClassName()</code>.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public ObjectInstance getObjectInstance(ObjectName name)
	    throws InstanceNotFoundException, IOException {
    	return writeRequest(ObjectInstance.class, JMXOpCode.GETOBJECTINSTANCE, name);
    }

    /**
     * Gets MBeans controlled by the MBean server. This method allows
     * any of the following to be obtained: All MBeans, a set of
     * MBeans specified by pattern matching on the
     * <CODE>ObjectName</CODE> and/or a Query expression, a specific
     * MBean. When the object name is null or no domain and key
     * properties are specified, all objects are to be selected (and
     * filtered if a query is specified). It returns the set of
     * <CODE>ObjectInstance</CODE> objects (containing the
     * <CODE>ObjectName</CODE> and the Java Class name) for the
     * selected MBeans.
     *
     * @param name The object name pattern identifying the MBeans to
     * be retrieved. If null or no domain and key properties are
     * specified, all the MBeans registered will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans. If null no query expression will be applied for
     * selecting MBeans.
     *
     * @return A set containing the <CODE>ObjectInstance</CODE>
     * objects for the selected MBeans.  If no MBean satisfies the
     * query an empty list is returned.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    @SuppressWarnings("unchecked")
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
	    throws IOException {
    	return (Set<ObjectInstance>) writeRequest(JMXOpCode.QUERYMBEANS.returnType, JMXOpCode.QUERYMBEANS, name, query);
    }

    /**
     * Gets the names of MBeans controlled by the MBean server. This
     * method enables any of the following to be obtained: The names
     * of all MBeans, the names of a set of MBeans specified by
     * pattern matching on the <CODE>ObjectName</CODE> and/or a Query
     * expression, a specific MBean name (equivalent to testing
     * whether an MBean is registered). When the object name is null
     * or no domain and key properties are specified, all objects are
     * selected (and filtered if a query is specified). It returns the
     * set of ObjectNames for the MBeans selected.
     *
     * @param name The object name pattern identifying the MBean names
     * to be retrieved. If null or no domain and key properties are
     * specified, the name of all registered MBeans will be retrieved.
     * @param query The query expression to be applied for selecting
     * MBeans. If null no query expression will be applied for
     * selecting MBeans.
     *
     * @return A set containing the ObjectNames for the MBeans
     * selected.  If no MBean satisfies the query, an empty list is
     * returned.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    @SuppressWarnings("unchecked")
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
	    throws IOException {
    	return (Set<ObjectName>) writeRequest(JMXOpCode.QUERYNAMES.returnType, JMXOpCode.QUERYNAMES, name, query);
    }



    /**
     * Checks whether an MBean, identified by its object name, is
     * already registered with the MBean server.
     *
     * @param name The object name of the MBean to be checked.
     *
     * @return True if the MBean is already registered in the MBean
     * server, false otherwise.
     *
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public boolean isRegistered(ObjectName name)
	    throws IOException {
    	return writeRequest(Boolean.class, JMXOpCode.ISREGISTERED, name);
    }


    /**
     * Returns the number of MBeans registered in the MBean server.
     *
     * @return the number of MBeans registered.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public Integer getMBeanCount()
	    throws IOException {
    	return writeRequest(Integer.class, JMXOpCode.GETMBEANCOUNT);
    }

    /**
     * Gets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the
     * attribute is to be retrieved.
     * @param attribute A String specifying the name of the attribute
     * to be retrieved.
     *
     * @return	The value of the retrieved attribute.
     *
     * @exception AttributeNotFoundException The attribute specified
     * is not accessible in the MBean.
     * @exception MBeanException Wraps an exception thrown by the
     * MBean's getter.
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.Exception</CODE> thrown when trying to invoke
     * the setter.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or the attribute in parameter is
     * null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #setAttribute
     */
    public Object getAttribute(ObjectName name, String attribute)
	    throws MBeanException, AttributeNotFoundException,
	    	ReflectionException, InstanceNotFoundException,
	    	   IOException {
    	return writeRequest(Object.class, JMXOpCode.GETATTRIBUTE, name, attribute);
    }


    /**
     * Enables the values of several attributes of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The object name of the MBean from which the
     * attributes are retrieved.
     * @param attributes A list of the attributes to be retrieved.
     *
     * @return The list of the retrieved attributes.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception ReflectionException An exception occurred when
     * trying to invoke the getAttributes method of a Dynamic MBean.
     * @exception RuntimeOperationsException Wrap a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or attributes in parameter is null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #setAttributes
     */
    public AttributeList getAttributes(ObjectName name, String[] attributes)
	    throws InstanceNotFoundException, ReflectionException,
		   IOException {
    	return writeRequest(AttributeList.class, JMXOpCode.GETATTRIBUTES, name, attributes);
    }

    /**
     * Sets the value of a specific attribute of a named MBean. The MBean
     * is identified by its object name.
     *
     * @param name The name of the MBean within which the attribute is
     * to be set.
     * @param attribute The identification of the attribute to be set
     * and the value it is to be set to.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception AttributeNotFoundException The attribute specified
     * is not accessible in the MBean.
     * @exception InvalidAttributeValueException The value specified
     * for the attribute is not valid.
     * @exception MBeanException Wraps an exception thrown by the
     * MBean's setter.
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.Exception</CODE> thrown when trying to invoke
     * the setter.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or the attribute in parameter is
     * null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #getAttribute
     */
    public void setAttribute(ObjectName name, Attribute attribute)
	    throws InstanceNotFoundException, AttributeNotFoundException,
		   InvalidAttributeValueException, MBeanException, 
		   ReflectionException, IOException {
    	writeRequest(JMXOpCode.SETATTRIBUTE, name, attribute);
    }



    /**
     * Sets the values of several attributes of a named MBean. The MBean is
     * identified by its object name.
     *
     * @param name The object name of the MBean within which the
     * attributes are to be set.
     * @param attributes A list of attributes: The identification of
     * the attributes to be set and the values they are to be set to.
     *
     * @return The list of attributes that were set, with their new
     * values.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception ReflectionException An exception occurred when
     * trying to invoke the getAttributes method of a Dynamic MBean.
     * @exception RuntimeOperationsException Wraps a
     * <CODE>java.lang.IllegalArgumentException</CODE>: The object
     * name in parameter is null or attributes in parameter is null.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #getAttributes
     */
    public AttributeList setAttributes(ObjectName name,
				       AttributeList attributes)
	throws InstanceNotFoundException, ReflectionException, IOException {
    	return writeRequest(AttributeList.class, JMXOpCode.SETATTRIBUTES, name, attributes);
    }

    /**
     * Invokes an operation on an MBean.
     *
     * @param name The object name of the MBean on which the method is
     * to be invoked.
     * @param operationName The name of the operation to be invoked.
     * @param params An array containing the parameters to be set when
     * the operation is invoked
     * @param signature An array containing the signature of the
     * operation. The class objects will be loaded using the same
     * class loader as the one used for loading the MBean on which the
     * operation was invoked.
     *
     * @return The object returned by the operation, which represents
     * the result of invoking the operation on the MBean specified.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception MBeanException Wraps an exception thrown by the
     * MBean's invoked method.
     * @exception ReflectionException Wraps a
     * <CODE>java.lang.Exception</CODE> thrown while trying to invoke
     * the method.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     */
    public Object invoke(ObjectName name, String operationName,
			 Object params[], String signature[])
	    throws InstanceNotFoundException, MBeanException,
		   ReflectionException, IOException {
    	return writeRequest(Object.class, JMXOpCode.INVOKE, name, operationName, params, signature);
    }
 

  
    /**
     * Returns the default domain used for naming the MBean.
     * The default domain name is used as the domain part in the ObjectName
     * of MBeans if no domain is specified by the user.
     *
     * @return the default domain.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public String getDefaultDomain()
	    throws IOException {
    	return writeRequest(String.class, JMXOpCode.GETDEFAULTDOMAIN);
    }

    /**
     * <p>Returns the list of domains in which any MBean is currently
     * registered.  A string is in the returned array if and only if
     * there is at least one MBean registered with an ObjectName whose
     * {@link ObjectName#getDomain() getDomain()} is equal to that
     * string.  The order of strings within the returned array is
     * not defined.</p>
     *
     * @return the list of domains.
     *
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @since.unbundled JMX 1.2
     */
    public String[] getDomains()
	    throws IOException {
    	return writeRequest(String[].class, JMXOpCode.GETDOMAINS);
    }

    /**
     * <p>Adds a listener to a registered MBean.</p>
     *
     * <P> A notification emitted by an MBean will be forwarded by the
     * MBeanServer to the listener.  If the source of the notification
     * is a reference to an MBean object, the MBean server will replace it
     * by that MBean's ObjectName.  Otherwise the source is unchanged.
     *
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The listener object which will handle the
     * notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #removeNotificationListener(ObjectName, NotificationListener)
     * @see #removeNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     */
    public void addNotificationListener(ObjectName name,
					NotificationListener listener,
					NotificationFilter filter,
					Object handback)
	    throws InstanceNotFoundException, IOException {
    	writeRequest(JMXOpCode.ADDNOTIFICATIONLISTENER_ONNO, name, listener, filter, handback);
    }


    /**
     * <p>Adds a listener to a registered MBean.</p>
     *
     * <p>A notification emitted by an MBean will be forwarded by the
     * MBeanServer to the listener.  If the source of the notification
     * is a reference to an MBean object, the MBean server will
     * replace it by that MBean's ObjectName.  Otherwise the source is
     * unchanged.</p>
     *
     * <p>The listener object that receives notifications is the one
     * that is registered with the given name at the time this method
     * is called.  Even if it is subsequently unregistered, it will
     * continue to receive notifications.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be added.
     * @param listener The object name of the listener which will
     * handle the notifications emitted by the registered MBean.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a
     * notification is emitted.
     *
     * @exception InstanceNotFoundException The MBean name of the
     * notification listener or of the notification broadcaster does
     * not match any of the registered MBeans.
     * @exception RuntimeOperationsException Wraps an {@link
     * IllegalArgumentException}.  The MBean named by
     * <code>listener</code> exists but does not implement the {@link
     * NotificationListener} interface.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #removeNotificationListener(ObjectName, ObjectName)
     * @see #removeNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     */
    public void addNotificationListener(ObjectName name,
					ObjectName listener,
					NotificationFilter filter,
					Object handback)
	    throws InstanceNotFoundException, IOException {
    	writeRequest(JMXOpCode.ADDNOTIFICATIONLISTENER_OONO, name, listener, filter, handback);
    }


    /**
     * Removes a listener from a registered MBean.
     *
     * <P> If the listener is registered more than once, perhaps with
     * different filters or callbacks, this method will remove all
     * those registrations.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The object name of the listener to be removed.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     */
    public void removeNotificationListener(ObjectName name,
					   ObjectName listener) 
	throws InstanceNotFoundException, ListenerNotFoundException,
	       IOException {
    	writeRequest(JMXOpCode.REMOVENOTIFICATIONLISTENER_OO, name, listener);
    }

    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <p>The MBean must have a listener that exactly matches the
     * given <code>listener</code>, <code>filter</code>, and
     * <code>handback</code> parameters.  If there is more than one
     * such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The object name of the listener to be removed.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the
     * listener was added.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean, or it is not registered with the given
     * filter and handback.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, ObjectName,
     * NotificationFilter, Object)
     *
     * @since.unbundled JMX 1.2
     */
    public void removeNotificationListener(ObjectName name,
					   ObjectName listener,
					   NotificationFilter filter,
					   Object handback)
	    throws InstanceNotFoundException, ListenerNotFoundException,
		   IOException {
    	writeRequest(JMXOpCode.REMOVENOTIFICATIONLISTENER_OONO, name, listener, filter, handback);
    }


    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <P> If the listener is registered more than once, perhaps with
     * different filters or callbacks, this method will remove all
     * those registrations.
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The listener to be removed.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     */
    public void removeNotificationListener(ObjectName name,
					   NotificationListener listener)
	    throws InstanceNotFoundException, ListenerNotFoundException,
		   IOException {
    	writeRequest(JMXOpCode.REMOVENOTIFICATIONLISTENER_ON, name, listener);
    }

    /**
     * <p>Removes a listener from a registered MBean.</p>
     *
     * <p>The MBean must have a listener that exactly matches the
     * given <code>listener</code>, <code>filter</code>, and
     * <code>handback</code> parameters.  If there is more than one
     * such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param name The name of the MBean on which the listener should
     * be removed.
     * @param listener The listener to be removed.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the
     * listener was added.
     *
     * @exception InstanceNotFoundException The MBean name provided
     * does not match any of the registered MBeans.
     * @exception ListenerNotFoundException The listener is not
     * registered in the MBean, or it is not registered with the given
     * filter and handback.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see #addNotificationListener(ObjectName, NotificationListener,
     * NotificationFilter, Object)
     *
     * @since.unbundled JMX 1.2
     */
    public void removeNotificationListener(ObjectName name,
					   NotificationListener listener,
					   NotificationFilter filter,
					   Object handback)
	    throws InstanceNotFoundException, ListenerNotFoundException,
		   IOException {
    	writeRequest(JMXOpCode.REMOVENOTIFICATIONLISTENER_ONNO, name, listener, filter, handback);
    }

    /**
     * This method discovers the attributes and operations that an
     * MBean exposes for management.
     *
     * @param name The name of the MBean to analyze
     *
     * @return An instance of <CODE>MBeanInfo</CODE> allowing the
     * retrieval of all attributes and operations of this MBean.
     *
     * @exception IntrospectionException An exception occurred during
     * introspection.
     * @exception InstanceNotFoundException The MBean specified was
     * not found.
     * @exception ReflectionException An exception occurred when
     * trying to invoke the getMBeanInfo of a Dynamic MBean.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     */
    public MBeanInfo getMBeanInfo(ObjectName name)
	    throws InstanceNotFoundException, IntrospectionException,
	    	   ReflectionException, IOException {
    	return writeRequest(MBeanInfo.class, JMXOpCode.GETMBEANINFO, name);
    }

 
    /**
     * <p>Returns true if the MBean specified is an instance of the
     * specified class, false otherwise.</p>
     *
     * <p>If <code>name</code> does not name an MBean, this method
     * throws {@link InstanceNotFoundException}.</p>
     *
     * <p>Otherwise, let<br>
     * X be the MBean named by <code>name</code>,<br>
     * L be the ClassLoader of X,<br>
     * N be the class name in X's {@link MBeanInfo}.</p>
     *
     * <p>If N equals <code>className</code>, the result is true.</p>
     *
     * <p>Otherwise, if L successfully loads <code>className</code>
     * and X is an instance of this class, the result is true.
     *
     * <p>Otherwise, if L successfully loads both N and
     * <code>className</code>, and the second class is assignable from
     * the first, the result is true.</p>
     *
     * <p>Otherwise, the result is false.</p>
     * 
     * @param name The <CODE>ObjectName</CODE> of the MBean.
     * @param className The name of the class.
     *
     * @return true if the MBean specified is an instance of the
     * specified class according to the rules above, false otherwise.
     *
     * @exception InstanceNotFoundException The MBean specified is not
     * registered in the MBean server.
     * @exception IOException A communication problem occurred when
     * talking to the MBean server.
     *
     * @see Class#isInstance
     */
    public boolean isInstanceOf(ObjectName name, String className)
	    throws InstanceNotFoundException, IOException {
    	return writeRequest(Boolean.class, JMXOpCode.ISINSTANCEOF, name, className);
    }

}
