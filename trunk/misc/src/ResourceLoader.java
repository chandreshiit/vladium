/* Copyright (C) 2006 Vladimir Roubtsov. All rights reserved.
 */
package com.vladium.vtp.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

// ----------------------------------------------------------------------------
/**
 * A collection of static convenience methods for retrieving classloader
 * resources. This class also provides methods that support smart classloader
 * strategies.
 * 
 * @author Vlad Roubtsov, 2004
 */
public
abstract class ResourceLoader
{
	/**
	 * A convenience method to retrieve a classloader resource as a stream.
	 * 'resourceName' is looked up in the classloader returned by
	 * <code>getClassLoader(ResourceLoader.class)</code>.
	 * 
	 * @param resourceName name of classloader resource to retrieve [may not be null;
	 * follows the same format as ClassLoader.getResourceAsStream ()]
	 *
	 * @throws IllegalArgumentException if 'resourceName' could not found [note that this is
	 * different from ClassLoader.getResourceAsStream () behavior].
	 *
	 * @throws IllegalArgumentException if 'resourceName' is null.
	 *
	 * @see #getResourceAsURL
	 * @see #getResourceAsProperties
	 */
	public static InputStream getResourceAsStream (final String resourceName)
	{
		if (resourceName == null)
			throw new IllegalArgumentException ("null input: resourceName");
		
		final ClassLoader loader = getClassLoader (LOADER);
		
		final InputStream result;
		if (loader == null)
			result = ClassLoader.getSystemResourceAsStream (resourceName);
		else
			result = loader.getResourceAsStream (resourceName);
		
		if (result == null)
			throw new IllegalArgumentException ("classpath resource [" + resourceName + "] could not be loaded");
		
		return result;
	}
	
	/**
	 * A convenience method to retrieve a classloader resource as a URL.
	 * 'resourceName' is looked up in the classloader returned by
	 * <code>getClassLoader(ResourceLoader.class)</code>.<P>
	 * 
	 * Unlike {@link #getResourceAsStream} the resource is returned as a URL
	 * which is handy when the resource data needs to be iterated over multiple times.
	 * 
	 * @param resourceName name of classloader resource to retrieve [may not be null;
	 * follows the same format as ClassLoader.getResourceAsStream ()]
	 *
	 * @throws IllegalArgumentException if 'resourceName' could not found [note that this is
	 * different from ClassLoader.getResource () behavior].
	 *
	 * @throws IllegalArgumentException if 'resourceName' is null.
	 *
	 * @see #getResourceAsStream
	 * @see #getResourceAsProperties
	 */
	public static URL getResourceAsURL (final String resourceName)
	{
		if (resourceName == null)
			throw new IllegalArgumentException ("null input: resourceName");
		
		final ClassLoader loader = getClassLoader (LOADER);
		
		final URL result;
		if (loader == null)
			result = ClassLoader.getSystemResource (resourceName);
		else
			result = loader.getResource (resourceName);
		
		if (result == null)
			throw new IllegalArgumentException ("classpath resource [" + resourceName + "] could not be loaded");
		
		return result;
	}

	/**
	 * A convenience method to retrieve a classloader resource as java.util.Properties
	 * read from the stream obtained via {@link #getResourceAsStream(String)}.
	 * 'resourceName' is looked up in the classloader returned by
	 * <code>getClassLoader(ResourceLoader.class)</code>.
	 *
	 * @param resourceName name of classpath resource to retrieve [may not be null; follows the
	 * same format as ClassLoader.getResourceAsStream ()]
	 *
	 * @throws IllegalArgumentException if 'resourceName' could not found [note that this is
	 * different from ClassLoader.getResource () behavior].
	 *
	 * @throws IllegalArgumentException if 'resourceName' is null.
	 *
	 * @see #getResourceAsStream
	 * @see #getResourceAsURL
	 */
	public static Properties getResourceAsProperties (final String resourceName)
	{
		InputStream in = null;
		try
		{
			in = getResourceAsStream (resourceName); // throws on failure
			
			final Properties result = new Properties ();
			result.load (in);
			
			return result;
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException ("classpath resource [" + resourceName + "] could not be read as Properties: " + e.toString ());
		}
		finally
		{
			if (in != null) try { in.close (); } catch (Exception ignore) {} 
		}
	}
	
	/**
	 * This methods returns the "best" classloader to be used for dynamic
	 * resource loading from code hosted by 'caller'. Using this method is
	 * more J2EE-friendly than merely relying on Class.forName(String) because
	 * this method will also consider the current thread's context loader.<P>
	 * 
	 * Typical usage from an instance method:
	 * <code><pre>
	 *     ClassLoader loader = ResourceLoader.getClassLoader(getClass());
	 *     // use 'loader' ...
	 * <pre></code>
	 * (in a static method, use <code>MyCurrentClass.class</code> instead of
	 * <code>getClass()</code>)<P>
	 * 
	 * <em>IMPORTANT:</em> because of the above reason the result of this
	 * call should never be cached within a scope that transcends an execution
	 * thread.<P> 
	 * 
	 * This method is equivalent to <code>getClassLoader(caller.getClassLoader())</code>.
	 * 
	 * @see <a href="http://www.javaworld.com/javaworld/javaqa/2003-06/01-qa-0606-load.html">why this is useful</a>
	 * 
	 * @param caller class whose defining loader should be considered along with
	 * the current thread context loader [null means use the thread context loader only]
	 * 
	 * @return a ClassLoader handle [can be null; not necessarily the same as
	 * <code>caller.getClassLoader()</code>]
	 */
	public static ClassLoader getClassLoader (final Class caller)
	{
		if (caller == null)
			return getClassLoader ((ClassLoader) null);

        return getClassLoader (caller.getClassLoader ());
	}

	/**
	 * This method is similar to {@link #getClassLoader(Class)}, except it
	 * allows the caller to specify a candidate classloader direactly. This
	 * method is useful under special circumstances only (when working with
	 * an API that accepts explicit java.lang.ClassLoader parameters).
	 * 
	 * @param callerLoader [null means to use the thread context loader only]
	 */	
	public static ClassLoader getClassLoader (final ClassLoader callerLoader)
	{
		final ClassLoader contextLoader = Thread.currentThread ().getContextClassLoader ();
		if (callerLoader == null) return contextLoader;
		
		ClassLoader result;
		
		// if 'callerLoader' and 'contextLoader' are in a parent-child
		// relationship, always choose the child:
		
		if (isChild (contextLoader, callerLoader))
			result = callerLoader;
		else if (isChild (callerLoader, contextLoader))
			result = contextLoader;
		else
		{
			// this else branch could be merged into the previous one,
			// but I show it here to emphasize the ambiguous case:
			result = contextLoader;
		}
		
		final ClassLoader systemLoader = ClassLoader.getSystemClassLoader ();
		
		// precaution for when deployed as a bootstrap or extension class:
		if (isChild (result, systemLoader))
			result = systemLoader;
		
		return result;
	}
	
	// protected: .............................................................

	// package: ...............................................................

	// private: ...............................................................


	private ResourceLoader () {} // this class is not extendible
	
	/**
	 * Returns 'true' if 'loader2' is a delegation child of 'loader1' [or if
	 * 'loader1'=='loader2']. Of course, this works only for classloaders that
	 * set their parent pointers correctly. 'null' is interpreted as the
	 * primordial loader [i.e., everybody's parent].
	 */ 
	private static boolean isChild (final ClassLoader loader1, ClassLoader loader2)
	{
		if (loader1 == loader2) return true; 
		if (loader2 == null) return false; 
		if (loader1 == null) return true;
		
		for ( ; loader2 != null; loader2 = loader2.getParent ())
		{
			if (loader2 == loader1) return true;
		}   

		return false;
	}
	
	// TODO: set this in a better way
	private static final ClassLoader LOADER; // set in <clinit>
	
	static
	{
		LOADER = ResourceLoader.class.getClassLoader ();
	} 
	
} // end of class
// ----------------------------------------------------------------------------
