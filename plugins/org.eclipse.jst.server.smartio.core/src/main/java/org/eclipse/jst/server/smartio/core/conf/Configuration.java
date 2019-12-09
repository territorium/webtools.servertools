/*
 * Copyright (c) 2001-2019 Territorium Online Srl / TOL GmbH. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as defined in and that are
 * subject to the Territorium Online License Version 1.0. You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at http://www.tol.info/license/
 * and read it before using this file.
 *
 * The Original Code and all software distributed under the License are distributed on an 'AS IS'
 * basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, AND TERRITORIUM ONLINE HEREBY
 * DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT. Please see the License for
 * the specific language governing rights and limitations under the License.
 */

package org.eclipse.jst.server.smartio.core.conf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The {@link Configuration} class represents a persistent set of properties. The
 * {@link Configuration} can be saved to a stream or loaded from a stream. Each key and its
 * corresponding value in the property list is a string.
 */
public class Configuration extends Properties implements Iterable<String> {

  private static final String DEFAULT_NAME = "";

  private class Section {

    private final String name;


    private final Map<String, String> values = new Hashtable<>();

    /**
     * Constructs an instance of {@link Section}.
     *
     * @param name
     */
    private Section(String name) {
      this.name = name;
    }
  }

  private final Map<String, Section> sections = new Hashtable<>();

  /**
   * Constructs an instance of {@link Configuration}.
   */
  public Configuration() {
    sections.put(Configuration.DEFAULT_NAME, new Section(Configuration.DEFAULT_NAME));
  }

  /**
   * Get the section names.
   */
  @Override
  public Iterator<String> iterator() {
    return sections.keySet().iterator();
  }

  /**
   * Get the section type
   * 
   * @param source
   * @param target
   */
  public void renameSection(String source, String target) {
    sections.put(target, sections.remove(source));
  }

  /**
   * Get the section type
   * 
   * @param name
   */
  public String getSectionType(String name) {
    return get(String.format("%s.type", name));
  }

  /**
   * Searches for the property with the specified key in this property list. If the key is not found
   * in this property list, the default property list, and its defaults, recursively, are then
   * checked. The method returns {@code null} if the property is not found.
   *
   * @param key the property key.
   */
  @Override
  public final String get(String key) {
    int indexOf = key.lastIndexOf('.');
    String prefix = (indexOf < 0) ? Configuration.DEFAULT_NAME : key.substring(0, indexOf);
    String suffix = (indexOf < 0) ? key : key.substring(indexOf + 1);

    Section section = sections.get(prefix);
    return (section == null) ? null : section.values.get(suffix);
  }

  /**
   * Calls the <tt>Hashtable</tt> method {@code put}. Provided for parallelism with the
   * <tt>getProperty</tt> method. Enforces use of strings for property keys and values. The value
   * returned is the result of the <tt>Hashtable</tt> call to {@code put}.
   *
   * @param key
   * @param value
   */
  @Override
  public synchronized void set(String key, String value) {
    int indexOf = key.lastIndexOf('.');
    String prefix = (indexOf < 0) ? Configuration.DEFAULT_NAME : key.substring(0, indexOf);
    String suffix = (indexOf < 0) ? key : key.substring(indexOf + 1);

    Section section = sections.get(prefix);
    if (section == null) {
      section = new Section(prefix);
      sections.put(prefix, section);
    }
    section.values.put(suffix, value);
  }

  /**
   * Writes the configuration list in a format suitable for loading into a {@link Configuration}
   * table using the {@link #load(java.io.Reader)} method.
   *
   * @param writer
   * @param comment
   * @param format
   */
  public final void save(Writer writer, String comment, Format format) throws IOException {
    write(LineWriter.of(writer, format == Format.INI ? ';' : '#'), comment, format);
  }

  /**
   * Writes the configuration list in a format suitable for loading into a {@link Configuration}
   * table using the {@link #load(InputStream)} method.
   *
   * @param stream
   * @param comment
   * @param format
   */
  public final void save(OutputStream stream, String comment, Format format) throws IOException {
    write(LineWriter.of(stream, format == Format.INI ? ';' : '#'), comment, format);
  }

  /**
   * Stores the properties to the Buffered Writer.
   *
   * @param writer
   * @param comment
   * @param format
   */
  @Override
  protected final void write(LineWriter writer, String comment, Format format) throws IOException {
    super.write(writer, comment, format);

    int offset = 0;
    synchronized (this) {
      Stream<String> keys = sections.keySet().stream().sorted(Properties.COMPARATOR);

      if (format == Format.INI) {
        offset = sections.values().stream().mapToInt(s -> Configuration.maxLength(s, format)).max().getAsInt();
      }

      for (String key : keys.collect(Collectors.toList())) {
        Section section = sections.get(key);
        if (section.values.isEmpty()) {
          continue;
        }

        if ((format == Format.INI) && !key.isEmpty()) {
          writer.newLine();
          writer.write('[');
          writer.write(key);
          writer.write(']');
        }
        writer.newLine();

        if (format == Format.NATIVE) {
          offset = Configuration.maxLength(section, format);
        }

        writeSection(writer, format, section, offset);
      }
    }
    writer.flush();
  }

  protected void writeSection(LineWriter writer, Format format, Section section, int offset) throws IOException {
    for (String key : section.values.keySet().stream().sorted().collect(Collectors.toList())) {
      String name = Configuration.toKey(section, format, key);
      String value = LineHelper.escape(section.values.get(key), false);
      String formatter = (offset > 0) ? "%-" + offset + "s = %s" : "%s=%s";
      writer.write(String.format(formatter, name, value));
      writer.newLine();
    }
  }


  /**
   * Calculates the max key length for the {@link Section}.
   *
   * @param section
   * @param format
   */
  private static int maxLength(Section section, Format format) {
    return section.values.keySet().stream().mapToInt(k -> Configuration.toKey(section, format, k).length()).max()
        .orElse(0);
  }

  /**
   * Creates a key for the provided {@link Format}.
   *
   * @param section
   * @param format
   * @param name
   */
  private static String toKey(Section section, Format format, String name) {
    boolean isLocal = ((format == Format.INI) || section.name.isEmpty());
    return LineHelper.escape(isLocal ? name : String.format("%s.%s", section.name, name), true);
  }
}