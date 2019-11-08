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
import java.io.Reader;

/**
 * The {@link LineReader} read in a "logical line" from character or byte stream. It skipping all
 * comments and blank lines and filter out those leading whitespace characters (\u0020, \u0009 and
 * \u000c) from the beginning of a "natural line".
 *
 * Method returns the char length of the "logical line" and stores the line into the buffer.
 */
abstract class LineReader {

  private int    limit  = 0;
  private int    offset = 0;
  private char[] buffer = new char[1024];

  /**
   * Read an integer from the stream.
   */
  protected abstract int read() throws IOException;

  /**
   * Read a character at index from the stream.
   *
   * @param index
   */
  protected abstract char readAt(int index) throws IOException;

  /**
   * Get the line buffer.
   */
  protected final char[] getLineBuffer() {
    return buffer;
  }

  /**
   * Read a single line and return the number of characters.
   */
  protected final int readLine() throws IOException {
    int length = 0;
    char character = 0;

    boolean skipLF = false;
    boolean isNewLine = true;
    boolean skipWhiteSpace = true;
    boolean isCommentLine = false;
    boolean appendedLineBegin = false;
    boolean precedingBackslash = false;
    while (true) {
      if (offset >= limit) {
        limit = read();
        offset = 0;
        if (limit <= 0) {
          if ((length == 0) || isCommentLine) {
            return -1;
          }
          if (precedingBackslash) {
            length--;
          }
          return length;
        }
      }
      character = readAt(offset++);

      if (skipLF) {
        skipLF = false;
        if (character == '\n') {
          continue;
        }
      }
      if (skipWhiteSpace) {
        if ((character == ' ') || (character == '\t') || (character == '\f')) {
          continue;
        }
        if (!appendedLineBegin && ((character == '\r') || (character == '\n'))) {
          continue;
        }
        skipWhiteSpace = false;
        appendedLineBegin = false;
      }
      if (isNewLine) {
        isNewLine = false;
        if ((character == '#') || (character == ';') || (character == '!')) {
          isCommentLine = true;
          continue;
        }
      }

      if ((character != '\n') && (character != '\r')) {
        buffer[length++] = character;
        if (length == buffer.length) {
          int newLength = buffer.length * 2;
          if (newLength < 0) {
            newLength = Integer.MAX_VALUE;
          }
          char[] buf = new char[newLength];
          System.arraycopy(buffer, 0, buf, 0, buffer.length);
          buffer = buf;
        }
        // flip the preceding backslash flag
        if (character == '\\') {
          precedingBackslash = !precedingBackslash;
        } else {
          precedingBackslash = false;
        }
      } else {
        // reached EOL
        if (isCommentLine || (length == 0)) {
          isCommentLine = false;
          isNewLine = true;
          skipWhiteSpace = true;
          length = 0;
          continue;
        }
        if (offset >= limit) {
          limit = read();
          offset = 0;
          if (limit <= 0) {
            if (precedingBackslash) {
              length--;
            }
            return length;
          }
        }
        if (precedingBackslash) {
          length -= 1;
          // skip the leading whitespace characters in following line
          skipWhiteSpace = true;
          appendedLineBegin = true;
          precedingBackslash = false;
          if (character == '\r') {
            skipLF = true;
          }
        } else {
          return length;
        }
      }
    }
  }

  /**
   * Create a {@link LineReader} for a character stream.
   *
   * @param reader
   */
  public static LineReader of(Reader reader) {
    return new CharReader(reader);
  }

  /**
   * Create a {@link LineReader} for a byte stream.
   *
   * @param reader
   */
  public static LineReader of(InputStream stream) {
    return new ByteReader(stream);
  }

  /**
   * The {@link CharReader} implements a {@link LineReader} for a byte stream.
   */
  private static class ByteReader extends LineReader {

    private final InputStream stream;
    private final byte[]      buffer;

    private ByteReader(InputStream stream) {
      this.stream = stream;
      buffer = new byte[8192];
    }

    @Override
    protected final int read() throws IOException {
      return stream.read(buffer);
    }

    // Uses an ISO8859-1 decoder.
    @Override
    protected final char readAt(int index) throws IOException {
      return (char) (0xff & buffer[index]);
    }
  }

  /**
   * The {@link CharReader} implements a {@link LineReader} for a character stream.
   */
  private static class CharReader extends LineReader {

    private final Reader reader;
    private final char[] buffer;

    private CharReader(Reader reader) {
      this.reader = reader;
      buffer = new char[8192];
    }

    @Override
    protected final int read() throws IOException {
      return reader.read(buffer);
    }

    @Override
    protected final char readAt(int index) throws IOException {
      return buffer[index];
    }
  }
}
