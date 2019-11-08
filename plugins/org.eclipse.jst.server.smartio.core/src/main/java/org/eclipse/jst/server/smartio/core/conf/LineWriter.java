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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;


/**
 * The {@link LineWriter} class represents a persistent set of properties. The {@link LineWriter}
 * can be saved to a stream or loaded from a stream. Each key and its corresponding value in the
 * property list is a string.
 */
public class LineWriter extends Writer {

  private final BufferedWriter writer;
  private final char           commentChar;
  private final boolean        escapeUnicode;

  /**
   * Constructs an instance of {@link LineWriter}.
   *
   * @param writer
   * @param commentChar
   * @param escapeUnicode
   */
  public LineWriter(BufferedWriter writer, char commentChar, boolean escapeUnicode) {
    this.writer = writer;
    this.commentChar = commentChar;
    this.escapeUnicode = escapeUnicode;
  }

  /**
   * Writes a portion of an array of characters.
   *
   * @param buffer
   * @param offset
   * @param length
   */
  @Override
  public final void write(char buffer[], int offset, int length) throws IOException {
    writer.write(buffer, offset, length);
  }

  /**
   * Writes a portion of a string.
   *
   * @param string
   * @param offset
   * @param length
   *
   */
  @Override
  public final void write(String string, int offset, int length) throws IOException {
    if (escapeUnicode) {
      writer.write(LineHelper.toAscii(string), offset, length);
    } else {
      writer.write(string, offset, length);
    }
  }

  /**
   * Flushes the stream.
   */
  @Override
  public final void flush() throws IOException {
    writer.flush();
  }

  /**
   * Closes the stream, flushing it first.
   */
  @Override
  public final void close() throws IOException {
    writer.close();
  }

  /**
   * Writes a new line
   */
  public final void newLine() throws IOException {
    writer.append('\n');
  }

  /**
   * Writes a comment to the {@link BufferedWriter}.
   *
   * @param comment
   * @throws IOException
   */
  public final void writeComment(String comment) throws IOException {
    LineHelper.writeComment(writer, comment, commentChar);
  }

  /**
   * Creates a {@link LineWriter} for the provided {@link Writer}.
   *
   * @param writer
   * @param commentChar
   */
  public static LineWriter of(Writer writer, char commentChar) {
    BufferedWriter buffer = (writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer);
    return new LineWriter(buffer, commentChar, false);
  }

  /**
   * Creates a {@link LineWriter} for the provided {@link OutputStream}.
   *
   * @param stream
   * @param commentChar
   */
  public static LineWriter of(OutputStream stream, char commentChar) throws IOException {
    return new LineWriter(new BufferedWriter(new OutputStreamWriter(stream, "8859_1")), commentChar, true);
  }
}