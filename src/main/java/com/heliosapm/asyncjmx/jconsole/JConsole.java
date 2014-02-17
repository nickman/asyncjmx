/**
 * 
 */
package com.heliosapm.asyncjmx.jconsole;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * <p>Title: JConsole</p>
 * <p>Description:  JConsole launcher. Attempts to locate the <b><code>jconsole.jar</code></b>
 * relative to the <b><code>java.home</code></b> system property.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.asyncjmx.jconsole.JConsole</code></b>
 * <p><b>Command Line Parameters<b>
 * <pre>
		Usage: java com.heliosapm.asyncjmx.jconsole.JConsole [ -interval=n ] [ -notile ] [ -pluginpath <path> ] [ -version ] [ connection ... ]
		
		  -interval   Set the update interval to n seconds (default is 4 seconds)
		  -notile     Do not tile windows initially (for two or more connections)
		  -pluginpath Specify the path that jconsole uses to look up the plugins
		  -version    Print program version
		
		  connection = pid || host:port || JMX URL (service:jmx:<protocol>://...)
		  pid         The process id of a target process
		  host        A remote host name or IP address
		  port        The port number for the remote connection
		
		  -J          Specify the input arguments to the Java virtual machine
		              on which jconsole is running
 * </pre></p>
 */

public class JConsole {

	/**
	 * Launches JConsole, optionally accepting jconsole command line parameters
	 * @param args See command line parameters above.
	 */
	public static void main(String[] args) {
		try {
			 URL jconsoleURL = findJConsoleJAR("jconsole.jar");
			 URL toolsURL = null;
			 try { toolsURL = findJConsoleJAR("tools.jar"); } catch (Exception x) { /* No Op */ }
			 URL[] urls = new URL[toolsURL==null ? 1 :2 ];
			 urls[0] = jconsoleURL;
			 if(toolsURL!=null) urls[1] = toolsURL; 
			 URLClassLoader ucl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
			 Class<?> jconsoleClazz = Class.forName("sun.tools.jconsole.JConsole", true, ucl);
			 //String[] pid = new String[]{ManagementFactory.getRuntimeMXBean().getName().split("@")[0]};
//			 //String[] pid = new String[]{"1823"};
			 String[] jmxurl = new String[]{"service:jmx:syncajmx://localhost:9061"};
//			 jconsoleClazz.getDeclaredMethod("main", String[].class).invoke(null, new Object[]{new String[]{"(service:jmx:syncajmx://localhost:9061"}});
			 jconsoleClazz.getDeclaredMethod("main", String[].class).invoke(null, new Object[]{jmxurl});
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}

	}
	
	/**
	 * Attempts to find the named jar file
	 * @return A URL to the located jar.
	 * @throws Exception thrown on any error or failure to find the named jar.
	 */
	protected static URL findJConsoleJAR(String name) throws Exception {
			File javaHome = new File(System.getProperty("java.home"));
			File libDir = null;
			File jConJar = null;
			if("jre".equals(javaHome.getName())) {
				libDir = new File(javaHome.getParent(), "lib");
			} else {
				libDir = new File(javaHome, "lib");
			}
			if(libDir.exists() && libDir.isDirectory()) {
				jConJar = new File(libDir, name);
				if(jConJar.exists()) {
					return jConJar.toURI().toURL();
				}
			}			
			throw new Exception("Failed to find [" + name + "] from [" + javaHome.getAbsolutePath() + "]");
	}

}
