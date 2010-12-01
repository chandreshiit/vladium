package com.vladium.vtp.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// ----------------------------------------------------------------------------
/**
 * A simple convenience class for representing spreadsheet-like tables in
 * memory. Allows referencing rows, columns, and cell by absolute position
 * indexes as well via position independent Row, Column, and Cell handles.<P>
 * 
 * Example:
 * <PRE><CODE>
 *   Table t = new Table ();
 *
 *   Table.Row header = t.addRow ();
 *   t.addRow ();
 *   
 *   Table.Column b = t.addColumn ();
 *   Table.Column a = t.insertColumn (0); // insert before b
 *   
 *   header.cell (a).set ("column a");
 *   header.cell (b).set ("column b");
 *   
 *   int i = 0;
 *   for (Iterator rows = t.rows (); rows.hasNext (); )
 *   {
 *       final Table.Row r = (Table.Row) rows.next ();
 *       for (Iterator columns = t.columns (); columns.hasNext (); ++ i)
 *       {
 *           final Table.Column c = (Table.Column) columns.next ();
 *   
 *           r.cell (c).set (i);
 *       }
 *   }
 * </CODE></PRE>
 * 
 * @author Vlad Roubtsov
 */
public final
class Table
{
    // public: ................................................................
    
    public static final IMetadata STRING    = new StringMetadata ();
    public static final IMetadata DOUBLE    = new DoubleMetadata ();
    public static final IMetadata INTEGER   = new IntegerMetadata ();
    public static final IMetadata LONG      = new LongMetadata ();
    
    /**
     * An index-independent handle to a table row. Such a handle remains valid
     * regardless of any table mutations.  
     */
    public final class Row
    {
        /**
         * Returns this row's cell at a specified column.
         */
        public Cell cell (final Column column)
        {
            return (Cell) m_cells.get (Table.this.mapColumn (column));
        }

        /**
         * Returns this row's cell at a specified column index.
         */
        public Cell cell (final int column)
        {
            if (column >= m_maxColumn)
                throw new IllegalArgumentException ("column index " + column + " too large");
            
            return (Cell) m_cells.get (column);
        }
        
        
        Row (final List /* Cell */ cells)
        {
            m_cells = cells;
        }
        
        final List /* Cell */ m_cells;
        
    } // end of nested class
    
    
    /**
     * An index-independent handle to a table column. Such a handle remains valid
     * regardless of any table mutations.  
     */
    public final class Column
    {
        /**
         * Returns this column's cell at a specified row.
         */
        public Cell cell (final Row row)
        {
            // not caching this column's index by design:
            
            return Table.this.cell (row, this);
        }
        
        /**
         * Returns this column's cell at a specified row index.
         */
        public Cell cell (final int row)
        {
            if (row >= m_maxRow)
                throw new IllegalArgumentException ("row index " + row + " too large");
            
            return Table.this.cell (row, this);
        }
        
        
        Column ()
        {
        }
        
    } // end of nested class
    
    
    /**
     * An index-independent handle to a table cell. Such a handle remains valid
     * regardless of any table mutations.<P>
     * 
     * A table cell is a generic holder of any opaque {@link #content() content}
     * which can be set using an Object or any primitive type value. Cell content
     * can be cleared via {@link #clear()}. All cells are created empty (the
     * content is null). Cell's {@link #toString()} will delegate to that of
     * the content.  
     */
    public static final class Cell
    {
        public Object content ()
        {
            return m_content;
        }
        
        public int getByte ()
        {
            return ((Number) m_content).byteValue ();
        }
        
        public short getShort ()
        {
            return ((Number) m_content).shortValue ();
        }
        
        public int getInt ()
        {
            return ((Number) m_content).intValue ();
        }
        
        public long getLong ()
        {
            return ((Number) m_content).longValue ();
        }
        
        public float getFloat ()
        {
            return ((Number) m_content).floatValue ();
        }
        
        public double getDouble ()
        {
            return ((Number) m_content).doubleValue ();
        }
        
        public boolean getBoolean ()
        {
            return ((Boolean) m_content).booleanValue ();
        }
        
        public String getString ()
        {
            return m_content.toString ();
        }
        
        // Object:
        
        public String toString ()
        {
            return String.valueOf (m_content);
        }
        
        // MUTATORS:
        
        public void set (final byte b)
        {
            m_content = new Byte (b);
        }
        
        public void set (final short s)
        {
            m_content = new Short (s);
        }
        
        public void set (final int i)
        {
            m_content = new Integer (i);
        }
        
        public void set (final long l)
        {
            m_content = new Long (l);
        }
        
        public void set (final float f)
        {
            m_content = new Float (f);
        }
        
        public void set (final double d)
        {
            m_content = new Double (d);
        }
        
        public void set (final boolean b)
        {
            m_content = new Boolean (b);
        }
        
        public void set (final char c)
        {
            m_content = new Character (c);
        }
        
        public void set (final Object content)
        {
            m_content = content;
        }
        
        public void clear ()
        {
            m_content = null;
        }
        
        
        Cell (final Row rparent, final Column cparent)
        {
            if (rparent == null)
                throw new IllegalArgumentException ("null input: rparent");
            if (cparent == null)
                throw new IllegalArgumentException ("null input: cparent");
            
//            m_rparent = rparent;
//            m_cparent = cparent;
        }
        
        private Object m_content;
        
    } // end of nested class
    
    
    public static interface IMetadata
    {
        void create (String s, Cell out);
        
    } // end of nested interface
    
    
    public static final class StringMetadata implements IMetadata
    {
        /* (non-Javadoc)
         * @see cnbc.Table.IMetadata#create(java.lang.String, cnbc.Table.Cell)
         */
        public final void create (final String s, final Cell out)
        {
            out.set (s);
        }
        
    } // end of nested class
    
    public static final class DoubleMetadata implements IMetadata
    {
        /* (non-Javadoc)
         * @see cnbc.Table.IMetadata#create(java.lang.String, cnbc.Table.Cell)
         */
        public final void create (final String s, final Cell out)
        {
            out.set (Double.parseDouble (s));
        }
        
    } // end of nested class
    
    public static final class IntegerMetadata implements IMetadata
    {
        /* (non-Javadoc)
         * @see cnbc.Table.IMetadata#create(java.lang.String, cnbc.Table.Cell)
         */
        public final void create (final String s, final Cell out)
        {
            out.set (Integer.parseInt (s));
        }
        
    } // end of nested class
    
    public static final class LongMetadata implements IMetadata
    {
        /* (non-Javadoc)
         * @see cnbc.Table.IMetadata#create(java.lang.String, cnbc.Table.Cell)
         */
        public final void create (final String s, final Cell out)
        {
            out.set (Long.parseLong (s));
        }
        
    } // end of nested class
    
    /**
     * Constructs a table with zero rows and columns.
     */
    public Table ()
    {
        m_rows = new ArrayList ();
        m_columns = new ArrayList ();
    }
    
    /**
     *@return total number of rows inserted into the table so far
     */
    public int height ()
    {
        return m_rows.size ();
    }
    
    /**
     *@return total number of columns inserted into the table so far
     */
    public int width ()
    {
        return m_columns.size ();
    }
    
    /**
     *@return immutable fail-fast Row iterator (over the current table rows)
     */
    public Iterator /* Row */ rows ()
    {
        return Collections.unmodifiableList (m_rows).iterator ();
    }
    
    /**
     *@return immutable fail-fast Column iterator (over the current table columns) 
     */
    public Iterator /* Column */ columns ()
    {
        return Collections.unmodifiableList (m_columns).iterator ();
    }
    
    /**
     * Returns the Row at a given absolute 0-based index.
     * 
     *@param row [must be in [0, height()-1] range]
     *@return Row object at a given absolute index
     */
    public Row row (final int row)
    {
        if (row >= m_maxRow)
                throw new IllegalArgumentException ("row index " + row + " too large");
        
        return (Row) m_rows.get (row);
    }
    
    /**
     * Returns the Column at a given absolute 0-based index.
     * 
     *@param column [must be in [0, width()-1] range]
     *@return Column object at a given absolute index
     */
    public Column column (final int column)
    {
        if (column >= m_maxColumn)
                throw new IllegalArgumentException ("row index " + column + " too large");
        
        return (Column) m_columns.get (column);
    }
    
    /**
     * Returns the Cell at a given (Row, Column) intersection.
     * 
     *@return Cell object at a given row/column intersection
     */
    public Cell cell (final Row row, final Column column)
    {
        return cell (mapRow (row), mapColumn (column));
    }
    
    /**
     * Returns the Cell at a given (row index, Column) intersection.
     * 
     *@param row [must be in [0, height()-1] range]
     *@return Cell object at a given row/column intersection
     */
    public Cell cell (final int row, final Column column)
    {
        return cell (row, mapColumn (column));
    }
    
    /**
     * Returns the Cell at a given (Row, column index) intersection.
     * 
     *@param column [must be in [0, width()-1] range]
     *@return Cell object at a given row/column intersection
     */
    public Cell cell (final Row row, final int column)
    {
        return cell (mapRow (row), column);
    }
    
    /**
     * Returns a Cell at a given given (row index, column index) intersection.
     * 
     *@param row [must be in [0, height()-1] range]
     *@param column [must be in [0, width()-1] range]
     *@return Cell object at a given row/column intersection
     */
    public Cell cell (final int row, final int column)
    {
        return ((Row) m_rows.get (row)).cell (column);
    }
    
    
    /**
     * Equivalent to render(out, ",") (renders in CSV format).
     */
    public void render (final PrintWriter out)
    {
        render (out, ",");
    }
    
    public void render (final File file)
        throws IOException
    {
        PrintWriter out = null;
        try
        {
            out = new PrintWriter (new BufferedOutputStream (new FileOutputStream (file),  8 * 1024));
            render (out);
        }
        finally
        {
            if (out != null) out.close ();
        }
    }
    
    /**
     * Dumps this table into a PrintWriter stream using a caller-provided
     * cell separator string.
     * 
     *@param out [may not be null]
     *@param separator [may not be null]
     */
    public void render (final PrintWriter out, final String separator)
    {
        if (out == null)
            throw new IllegalArgumentException ("null input: out");
        if (separator == null)
            throw new IllegalArgumentException ("null input: separator");
        
        for (int r = 0; r < height (); ++ r)
        {
            final Row row = row (r);
            for (int c = 0; c < width (); ++ c)
            {
                if (c != 0) out.print (separator);
                
                final Object content = row.cell (c).content ();
                if (content != null) out.print (content);
            }
            
            out.println ();
        }
    }
    
    public static Table load (final File file, final String separator, final boolean header, final IMetadata [] metadata)
        throws IOException
    {
        Reader in = null;
        try
        {
            in = new BufferedReader (new FileReader (file), 32 * 1024);
            return load (in, separator, header, metadata);
        }
        finally
        {
            if (in != null) in.close ();
        }
    }
    
    public static Table load (final Reader in, final String separator, final boolean header, final IMetadata [] metadata)
        throws IOException
    {
        if (in == null)
            throw new IllegalArgumentException ("null input: in");
        if (separator == null)
            throw new IllegalArgumentException ("null input: separator");
        
        final Table result = new Table ();
        
        final int width = metadata.length;
        result.insertColumn (width - 1);
        
        int row = 0;
        
        final BufferedReader bin = new BufferedReader (in);
        for (String line; (line = bin.readLine ()) != null; ++ row)
        {
            final String [] tokens = line.split (separator);
            if (tokens.length != width)
                throw new IllegalArgumentException ("invalid width " + tokens.length + " for row " + row);
            
            result.addRow ();
            if (header && (row == 0))
            {
                for (int column = 0; column < width; ++ column)
                {
                    result.cell (row, column).set (tokens [column]);
                }
            }
            else
            {
                for (int column = 0; column < width; ++ column)
                {
                    metadata [column].create (tokens [column], result.cell (row, column));
                }
            }
        }
        
        return result;
    }
    

    
    // Object:
    
    public String toString ()
    {
        final StringWriter sw = new StringWriter ();
        final PrintWriter pw = new PrintWriter (sw);
        
        pw.print ("table ");
        pw.print (height ());
        pw.print ('x');
        pw.print (width ());
        pw.println (":");
        
        render (pw, ", ");
        
        pw.flush ();
        pw.close ();
        
        return sw.toString ();
    }
    
    // MUTATORS:
    
    /**
     * Inserts a new row after the last existing table row, if any.
     * 
     * @return Row object that was added
     */
    public Row addRow ()
    {
        return insertRow (m_maxRow);
    }

    /**
     * Inserts a new column after the last existing table column, if any.
     * 
     * @return Row object that was added
     */
    public Column addColumn ()
    {
        return insertColumn (m_maxColumn);
    }
    
    /**
     * Inserts a new row into the table at a specified index, shifting down all
     * existing rows after the insertion point. It is legal to insert beyond the
     * last row, in which case enough empty rows are inserted to increase the
     * table height to 'index'+1. 
     * 
     *@param index [must be non-negative]
     *@return Row object inserted at position 'index' 
     */
    public Row insertRow (final int index)
    {
        final int start = index >= m_maxRow ? m_maxRow : index;
        
        for (int r = start; r <= index; ++ r)
        {
            final List /* Cell */ newcells = new ArrayList (m_maxColumn);
            final Row newrow = new Row (newcells);
            
            for (int c = 0; c < m_maxColumn; ++ c)
            {
                newcells.add (new Cell (newrow, (Column) m_columns.get (c)));
            }

            m_rows.add (r, newrow);
            
            ++ m_maxRow;
        }
        
        m_rowMap = null; // clear copy-on-write cache
        
        return (Row) m_rows.get (index);
    }

    /**
     * Inserts a new column into the table at a specified index, shifting right
     * all existing columns after the insert position. It is legal to insert beyond
     * the last column, in which case enough empty columns are inserted to increase
     * the table width to 'index'+1. 
     * 
     *@param index [must be non-negative]
     *@return Column object inserted at position 'index' 
     */
    public Column insertColumn (final int index)
    {
        final int start = index >= m_maxColumn ? m_maxColumn : index;
        
        for (int c = start; c <= index; ++ c)
        {
            final Column newcolumn = new Column ();
            
            for (int r = 0; r < m_maxRow; ++ r)
            {
                final Row row = (Row) m_rows.get (r);
                final List /* Cell */ cells = row.m_cells;
                
                cells.add (new Cell (row, newcolumn));
            }

            m_columns.add (c, newcolumn);
            
            ++ m_maxColumn;
        }
        
        m_columnMap = null; // clear copy-on-write cache
        
        return (Column) m_columns.get (index);
    }

    // protected: .............................................................

    // package: ...............................................................

    // these kep package-private to make inner class access more efficient:
    
    /* private */ int mapRow (final Row row)
    {
        Map rowMap = m_rowMap;
        if (rowMap == null) // populate mapping cache
        {
            rowMap = new HashMap ();
            for (int i = 0; i < m_rows.size (); ++ i)
            {
                rowMap.put (m_rows.get (i), new Integer (i));
            }
            
            m_rowMap = rowMap;
        }
        
        return ((Integer) rowMap.get (row)).intValue ();
                
    }
    
    /* private */ int mapColumn (final Column column)
    {
        Map columnMap = m_columnMap;
        if (columnMap == null) // populate mapping cache
        {
            columnMap = new HashMap ();
            for (int i = 0; i < m_columns.size (); ++ i)
            {
                columnMap.put (m_columns.get (i), new Integer (i));
            }
            
            m_columnMap = columnMap;
        }
        
        return ((Integer) columnMap.get (column)).intValue ();
    }
    
    // private: ...............................................................

    private final List /* Row */ m_rows;
    private final List /* Column */ m_columns;
    
    /*private*/ int m_maxRow, m_maxColumn; // TODO: I think these are redundant
    private transient Map /* Row -> Integer */ m_rowMap;
    private transient Map /* Column -> Integer */ m_columnMap;
    
} // end of class
// ----------------------------------------------------------------------------
