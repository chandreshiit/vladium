/* Copyright (C) 2006 Vladimir Roubtsov. All rights reserved.
 */
package com.vladium.vtp.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//----------------------------------------------------------------------------
/**
 * Any code that desires to benefit from the custom URL scheme as described
 * in <a href="http://www.javaworld.com/javaqa/2003-08/02-qa-0822-urls.html">URLs: Smart resource identifiers</a>
 * should this Factory for creating all java.util.URLs.
 */
public
abstract class URLFactory
{
    // public: ................................................................
    
    /**
     * All classloader resources with this name ("url.properties") are loaded
     * as .properties definitions and merged together to create a global list of
     * protocol name/stream handler mappings.
     */
    public static final String RESOURCE_NAME_HANDLER_MAPPING = "com/vladium/vtp/url.properties";
    
    /**
     * A convenience method for <code>new URL (base, relative)</code>. The exact
     * rules for URL combination depend on the URL scheme.
     * 
     * @param base base URL [null is equivalent to no base]
     * @param relative relative URL [may not be null]
     * @return a scheme-specific combination of 'base' and 'relative'
     */
    public static URL newURL (final URL base, final String relative)
        throws MalformedURLException
    {
        if (relative == null)
            throw new IllegalArgumentException ("null input: relative");
        
        return new URL (base, relative);
    }
    
    /**
     * A factory method for constructing a java.net.URL out of a plain string.
     * Stock URL schemes will be tried first, followed by custom schemes with
     * a mapping in the union of all {@link #RESOURCE_NAME_HANDLER_MAPPING}
     * resources found at the time this class is initialized.
     *  
     * @param url external URL form [may not be null] 
     * @return java.net.URL corresponding to the scheme in 'url'
     * 
     * @throws MalformedURLException if 'url' does not correspond to a known
     * stock or custom URL scheme
     */
    public static URL newURL (final String url)
        throws MalformedURLException
    {
        if (url == null)
            throw new IllegalArgumentException ("null input: url");
        
        // try already installed protocols first:
        try
        {
            return new URL (url);
        }
        catch (MalformedURLException ignore)
        {
            // ignore: try our handler list next
        }
        
        final int firstColon = url.indexOf (':');
        if (firstColon <= 0)
            throw new MalformedURLException ("no protocol specified: " + url);
        
        final String protocol = url.substring (0, firstColon);
        final URLStreamHandler handler = getHandler (protocol);
        
        if (handler == null)
            throw new MalformedURLException ("unknown protocol: " + protocol);
        
        return new URL (null, url, handler);
    }
    
    /**
     * Returns the (statically scoped to URLFactory class) URLStreamHandler
     * instance for a given URL protocol or null if 'protocol' is not mapped.
     * 
     * @param protocol [may not be null]
     * @return URL stream handler [null if not found]
     */
    public static URLStreamHandler getHandler (final String protocol)
    {
        if (protocol == null)
            throw new IllegalArgumentException ("null input: protocol");
        
        final Map handlerMap = getHandlerMap ();
        final URLStreamHandler handler;
        
        if ((handlerMap == null) ||	(handler = (URLStreamHandler) handlerMap.get (protocol)) == null)
            return null;
        
        return handler;
    }
    
    // protected: .............................................................
    
    // package: ...............................................................
    
    // private: ...............................................................
    
    
    private URLFactory () {} // this class is not extendible
    
    /**
     * Not synchronized by design. You will need to change this if you make
     * HANDLERS initialize lazily (post static initialization time).
     * 
     * @return scheme/stream handler map [can be null if static init failed]
     */
    private static Map /* String->URLContentHandler */ getHandlerMap ()
    {
        return HANDLERS;
    }
    
    /*
     * Loads a scheme/handler map that is a union of *all* resources named
     * 'resourceName' as seen by 'loader'. Null 'loader' is equivalent to the
     * application loader.
     */
    private static Map loadHandlerList (final String resourceName,
            ClassLoader loader)
    {
        if (loader == null) loader = ClassLoader.getSystemClassLoader ();
        
        final Map result = new HashMap ();
        
        try
        {
            // NOTE: using getResources () here
            final Enumeration resources = loader.getResources (resourceName);
            
            if (resources != null)
            {
                // merge all mappings in 'resources':
                
                while (resources.hasMoreElements ())
                {
                    final URL url = (URL) resources.nextElement ();
                    final Properties mapping;
                    
                    InputStream urlIn = null;
                    try
                    {
                        urlIn = url.openStream ();
                        
                        mapping = new Properties ();
                        mapping.load (urlIn); // load in .properties format
                    }
                    catch (IOException ioe)
                    {
                        // ignore this resource and go to the next one
                        continue;
                    }
                    finally
                    {
                        if (urlIn != null) try { urlIn.close (); }
                        catch (Exception ignore) {} 
                    }
                    
                    // load all handlers specified in 'mapping':
                    
                    for (Enumeration keys = mapping.propertyNames ();
                    keys.hasMoreElements (); )
                    {
                        final String protocol = (String) keys.nextElement ();
                        final String implClassName = mapping.getProperty (protocol);
                        
                        final Object currentImpl = result.get (protocol);
                        if (currentImpl != null)
                        {
                            if (implClassName.equals (currentImpl.getClass ().getName ()))
                                continue; // skip duplicate mapping
                            
                            throw new IllegalStateException ("duplicate "  +
                                                             "protocol handler class [" + implClassName +
                                                             "] for protocol " + protocol);
                        }
                        
                        result.put (protocol, loadURLStreamHandler (implClassName, loader));
                    }
                }
            }
        }
        catch (IOException ignore)
        {
            // ignore: an empty result will be returned
        }
        
        return result;  
    }
    
    /*
     * Loads and initializes a single URL stream handler for a given name via a
     * given classloader. For simplicity, all errors are converted to
     * RuntimeExceptions.
     */	
    private static URLStreamHandler loadURLStreamHandler (final String className,
            final ClassLoader loader)
    {
        if (className == null)
            throw new IllegalArgumentException ("null input: className");
        if (loader == null)
            throw new IllegalArgumentException ("null input: loader");
        
        final Class cls;
        final Object handler;
        try
        {
            cls = Class.forName (className, true, loader);
            handler = cls.newInstance ();
        }
        catch (Exception e)
        {
            throw new RuntimeException ("could not load and instantiate" +
                                        " [" + className + "]: " + e.getMessage ());
        }
        
        if (! (handler instanceof URLStreamHandler))
            throw new RuntimeException ("not a java.net.URLStreamHandler" +
                                        " implementation: " + cls.getName ());
        
        return (URLStreamHandler) handler;
    }
    
    /**
     * This method decides on which classloader is to be used by all resource/class
     * loading in this class. Note that the the current thread's context loader
     * must be considered among candidate loaders. 
     */
    private static ClassLoader getClassLoader ()
    {
        return ResourceLoader.getClassLoader (URLFactory.class);
    }
    
    
    private static final Map /* String->URLContentHandler */ HANDLERS; // set in <clinit>
    private static final boolean DEBUG = true;
    
    static
    {
        Map temp = null;
        try
        {
            temp = loadHandlerList (RESOURCE_NAME_HANDLER_MAPPING,
                                    getClassLoader ());
        }
        catch (Exception e)
        {
            if (DEBUG)
            {
                System.out.println ("could not load all" +
                                    " [" + RESOURCE_NAME_HANDLER_MAPPING + "] mappings:");
                e.printStackTrace (System.out);
            }
        }
        
        HANDLERS = temp;
    }
    
} // end of class
//----------------------------------------------------------------------------
