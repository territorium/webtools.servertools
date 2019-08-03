/*
 * Copyright (c) 2001-2019 Territorium Online Srl / TOL GmbH. All Rights
 * Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the Territorium Online License Version
 * 1.0. You may not use this file except in compliance with the License. Please
 * obtain a copy of the License at http://www.tol.info/license/ and read it
 * before using this file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS
 * OR IMPLIED, AND TERRITORIUM ONLINE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 * A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the
 * License for the specific language governing rights and limitations under the
 * License.
 */

package org.eclipse.jst.server.smartio.core.conf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Comparator;
import java.util.Date;


/**
 * The {@link Properties} class represents a persistent set of properties. The
 * {@link Properties} can be saved to a stream or loaded from a stream. Each key
 * and its corresponding value in the property list is a string.
 */
public abstract class Properties {

  public enum Format {
    NATIVE,
    INI
  }

  public enum Pretty {
    NONE,
    LOCAL,
    GLOBAL
  }

  protected static final Comparator<String> COMPARATOR = new NameComparator();

  /**
   * Searches for the property with the specified key in this property list. If
   * the key is not found in this property list, the default property list, and
   * its defaults, recursively, are then checked. The method returns
   * {@code null} if the property is not found.
   *
   * @param key the property key.
   */
  public abstract String get(String key);

  /**
   * Searches for the property with the specified key in this property list. If
   * the key is not found in this property list, the default property list, and
   * its defaults, recursively, are then checked. The method returns the default
   * value argument if the property is not found.
   *
   * @param key
   * @param string
   */
  public final String get(String key, String string) {
    String value = get(key);
    return (value == null) ? string : value;
  }

  /**
   * Get the property as string array. Otherwise returns an empty array.
   *
   * @param key
   */
  public final String[] getArray(String key) {
    String value = get(key);
    return ((key == null) || key.isEmpty()) ? new String[0] : value.split(",");
  }

  /**
   * Get a integer value for the key.
   *
   * @param key
   * @param integer
   */
  public final int get(String key, int integer) {
    String value = get(key);
    return ((key == null) || key.isEmpty()) ? integer : Integer.parseInt(value);
  }

  /**
   * Get a numeric value for the key.
   *
   * @param key
   * @param number
   */
  public final double get(String key, double number) {
    String value = get(key);
    return ((key == null) || key.isEmpty()) ? number : Double.parseDouble(value);
  }

  /**
   * Calls the <tt>Hashtable</tt> method {@code put}. Provided for parallelism
   * with the <tt>getProperty</tt> method. Enforces use of strings for property
   * keys and values. The value returned is the result of the <tt>Hashtable</tt>
   * call to {@code put}.
   *
   * @param key
   * @param value
   */
  public abstract void set(String key, String value);

  /**
   * Reads a property list (key and element pairs) from the input character
   * stream in a simple line-oriented format. {@link Properties} are processed
   * in terms of lines.
   *
   * @param reader
   */
  public final synchronized void load(Reader reader) throws IOException {
    new LineParser(this).parse(reader);
  }

  /**
   * Reads a property list (key and element pairs) from the input byte stream.
   * The input stream is in a simple line-oriented format as specified in
   * {@link #load(java.io.Reader) load(Reader)} and is assumed to use the ISO
   * 8859-1 character encoding; that is each byte is one Latin1 character.
   * Characters not in Latin1, and certain special characters, are represented
   * in keys and elements using Unicode escapes as defined in section 3.3 of
   * <cite>The Java&trade; Language Specification</cite>.
   *
   * @param stream
   */
  public final synchronized void load(InputStream stream) throws IOException {
    new LineParser(this).parse(stream);
  }

  /**
   * Writes this property list (key and element pairs) in this
   * {@link Properties} table to the output character stream in a format
   * suitable for using the {@link #load(java.io.Reader) load(Reader)} method.
   *
   * @param writer
   * @param comment
   */
  public final void save(Writer writer, String comment) throws IOException {
    write(LineWriter.of(writer, '#'), comment, Format.NATIVE);
  }

  /**
   * Writes this property list (key and element pairs) in this
   * {@link Properties} table to the output stream in a format suitable for
   * loading into a {@link Properties} table using the {@link #load(InputStream)
   * load(InputStream)} method.
   *
   * @param stream
   * @param comment
   */
  public final void save(OutputStream stream, String comment) throws IOException {
    write(LineWriter.of(stream, '#'), comment, Format.NATIVE);
  }

  /**
   * Stores the properties to the Buffered Writer.
   *
   * @param writer
   * @param comment
   * @param format
   */
  protected void write(LineWriter writer, String comment, Format format) throws IOException {
    if (comment != null) {
      writer.writeComment(comment);
    }
    writer.writeComment(new Date().toString());
  }

  /**
   * The {@link NameComparator} implements a String.
   */
  private static class NameComparator implements Comparator<String> {

    @Override
    public final int compare(String source, String target) {
      int srcIndex = source.lastIndexOf('.');
      int trgIndex = target.lastIndexOf('.');

      if ((srcIndex < 0) && (trgIndex < 0)) {
        return source.compareTo(target);
      }
      if ((srcIndex < 0) && (trgIndex > 0)) {
        return -1;
      }
      if ((srcIndex > 0) && (trgIndex < 0)) {
        return 1;
      }

      int compareTo = source.substring(0, srcIndex).compareTo(target.substring(0, trgIndex));
      return (compareTo == 0) ? compare(source.substring(srcIndex + 1), target.substring(trgIndex + 1)) : compareTo;
    }
  }
}