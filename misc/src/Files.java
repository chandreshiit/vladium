/* Copyright (C) 2006 Vladimir Roubtsov. All rights reserved.
 */
package com.vladium.vtp.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

// ----------------------------------------------------------------------------
/**
 * Static utility API for working with files and file content. See individual
 * methods for details.
 * 
 * @author Vlad Roubtsov, 2006
 */
public abstract
class Files
{
    // public: ................................................................
    
    /**
     * Reads a given file into a Properties object.
     * 
     * @param file input file descriptor [may not be null]
     * 
     * @throws IOException on file I/O errors
     */
    public static Properties loadFileAsProperties (final File file)
        throws IOException
    {
        if (file == null)
            throw new IllegalArgumentException ("null input: file");
        
        InputStream in = null;
        try
        {
            in = new BufferedInputStream (new FileInputStream (file), 8 * 1024);
            final Properties result = new Properties ();
            
            result.load (in);
            
            return result;
        }
        finally
        {
            if (in != null) try { in.close (); } catch (IOException ignore) { ignore.printStackTrace (); }
        }
    }
    
    /**
     * Saves a Properties object into a file.
     * 
     * @param properties property set to save [may not be null]
     * @param file output file descriptor [may not be null; overwritten if exists]
     * @throws IOException on file I/O errors
     */
    public static void savePropertiesInFile (final Properties properties, final File file)
        throws IOException
    {
        if (file == null)
            throw new IllegalArgumentException ("null input: file");
        if (properties == null)
            throw new IllegalArgumentException ("null input: properties");
        
        OutputStream out = null;
        try
        {
            out = new BufferedOutputStream (new FileOutputStream (file), 8 * 1024);
            
            properties.store (out, null); // note that this method adds a date comment
            out.flush ();
        }
        finally
        {
            if (out != null) try { out.close (); } catch (IOException ignore) { ignore.printStackTrace (); }
        }
    }
    
    /**
     * Invariant: (getFileName (file) + getFileExtension (file)).equals (file.getName ()).
     * 
     * @param file File input file descriptor [must be non-null]
     * 
     * @return String file name without the extension [excluding '.' separator]
     * [if 'file' does not appear to have an extension, the full name is returned].
     * 
     * @throws IllegalArgumentException if 'file' is null
     */ 
    public static String getFileName (final File file)
    {
        if (file == null)
            throw new IllegalArgumentException ("null input: file");
        
        final String name = file.getName ();
        int lastDot = name.lastIndexOf ('.');
        if (lastDot < 0) return name;
        
        return name.substring (0, lastDot);
    }
    
    /**
     * Invariant: (getFileName (file) + getFileExtension (file)).equals (file.getName ()).
     * 
     * @param file File input file descriptor [must be non-null]
     * 
     * @return String extension [including '.' separator] or "" if 'file' does not
     * appear to have an extension.
     * 
     * @throws IllegalArgumentException if 'file' is null
     */
    public static String getFileExtension (final File file)
    {
        if (file == null)
            throw new IllegalArgumentException ("null input: file");
        
        final String name = file.getName ();
        int lastDot = name.lastIndexOf ('.');
        if (lastDot < 0) return "";
        
        return name.substring (lastDot);
    }
    
    /**
     * Creates a new File from a parent/child pair. Takes care of 'file'
     * being absolute (or drive-relative on win32).
     * 
     * @param dir [null is ignored]
     * @param file [absolute overrides 'dir'; may not be null]
     */
    public static File newFile (final File dir, final String file)
    {
        if (file == null)
            throw new IllegalArgumentException ("null input: file");

        final File fileFile = new File (file);
        
        if (dir == null) return fileFile;
        if (fileFile.isAbsolute ()) return fileFile;

        // note that on win32 there is an odd inconsistency between File.isAbsolute()
        // and the rest of File API whereby "drive-relative" pathnames (starting
        // with a single leading slash) are not considered absolute and yet the API
        // does not present a correct way to figure that out. When using such a pathname 
        // as the 'child' argument to the File(File,String) constructor
        // it is not resolved in an intuitive way. Sun refuses to change
        // this behavior (see Sun's bug 4479742, for example).
        
        if (file.length () > 0)
        {
            final char first = file.charAt (0);
            if ((first == '\\') || (first == '/'))
                return fileFile;
        }
        
        return new File (dir, file);
    }
    
    public static File canonicalizeFile (final File file)
    {
        if (file == null) throw new IllegalArgumentException ("null input: file");
        
        try
        {
            return file.getCanonicalFile ();
        }
        catch (Exception e)
        {
            return file.getAbsoluteFile ();
        }
    }
    
    /**
     * Returns 'true' if on 'file' does not exist on completion (including the case when
     * it didn't exist to begin with).
     */
    public static boolean recursiveDelete (final File file)
    {
        if (file == null) throw new IllegalArgumentException ("null input: file");
        
        if (file.exists ())
        {
            final File [] files = file.listFiles (); // returns null if 'file' is not a directory
            if (files != null)
            {
                for (int f = 0; f < files.length; ++ f)
                {
                    final File child = files [f];
                    
                    if (child.isDirectory ())
                        recursiveDelete (child);
                    else
                        child.delete ();
                }
            }
            
            return file.delete ();
        }
        
        return true;
    }

    // protected: .............................................................

    // package: ...............................................................

    // private: ...............................................................
    
    private Files () {} // not extendible

} // end of class
// ----------------------------------------------------------------------------
