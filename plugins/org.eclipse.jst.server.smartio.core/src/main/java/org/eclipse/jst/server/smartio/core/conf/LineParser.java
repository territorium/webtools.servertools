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
 * The {@link LineParser} is a generic parse for line oriented byte/char streams.
 */
class LineParser {

  private final Properties properties;
  private final char[]     conversion = new char[1024];


  private int     length;
  private int     lengthKey;
  private int     offsetValue;

  private char    character;
  private boolean isSection;
  private boolean isEscaped;
  private boolean hasSeparator;

  private String  section = null;

  /**
   * Constructs an instance of {@link LineParser}.
   *
   * @param properties
   */
  public LineParser(Properties properties) {
    this.properties = properties;
  }

  /**
   * Gets the {@link Properties}.
   */
  public final Properties getProperties() {
    return properties;
  }

  /**
   * Reads a property list (key and element pairs) from the input character stream in a simple
   * line-oriented format. {@link Properties} are processed in terms of lines.
   *
   * @param reader
   */
  public final void parse(Reader reader) throws IOException {
    parse(LineReader.of(reader));
  }

  /**
   * Reads a property list (key and element pairs) from the input byte stream. The input stream is
   * in a simple line-oriented format as specified in {@link #load(java.io.Reader) load(Reader)} and
   * is assumed to use the ISO 8859-1 character encoding; that is each byte is one Latin1 character.
   * Characters not in Latin1, and certain special characters, are represented in keys and elements
   * using Unicode escapes as defined in section 3.3 of <cite>The Java&trade; Language
   * Specification</cite>.
   *
   * @param stream
   */
  public final void parse(InputStream stream) throws IOException {
    parse(LineReader.of(stream));
  }

  /**
   * Reads the properties from {@link LineReader}.
   *
   * @param reader
   */
  protected final void parse(LineReader reader) throws IOException {
    while ((length = reader.readLine()) >= 0) {
      character = 0;
      lengthKey = 0;
      offsetValue = length;
      isSection = false;
      isEscaped = false;
      hasSeparator = false;

      // Reading the key
      while (lengthKey < length) {
        character = reader.getLineBuffer()[lengthKey];
        // need check if escaped.
        if (!isEscaped) {
          if (character == '[') {
            isSection = true;
          } else if (character == ']') {
            lengthKey--;
            break;
          } else if (!isSection && ((character == '=') || (character == ':'))) {
            offsetValue = lengthKey + 1;
            hasSeparator = true;
            break;
          } else if (((character == ' ') || (character == '\t') || (character == '\f'))) {
            offsetValue = lengthKey + 1;
            break;
          }
        }

        if (character == '\\') {
          isEscaped = !isEscaped;
        } else {
          isEscaped = false;
        }
        lengthKey++;
      }

      // Reading the value
      while (offsetValue < length) {
        character = reader.getLineBuffer()[offsetValue];
        if ((character != ' ') && (character != '\t') && (character != '\f')) {
          if (!hasSeparator && ((character == '=') || (character == ':'))) {
            hasSeparator = true;
          } else {
            break;
          }
        }
        offsetValue++;
      }
      if (isSection) {
        isSection = false;
        section = LineHelper.toString(reader.getLineBuffer(), 1, lengthKey, conversion);
      } else {
        String key = LineHelper.toString(reader.getLineBuffer(), 0, lengthKey, conversion);
        String value = LineHelper.toString(reader.getLineBuffer(), offsetValue, length - offsetValue, conversion);
        if (key.isEmpty() || value.isEmpty()) {
          continue;
        }
        if (section != null) {
          key = String.join(".", section, key);
        }

        getProperties().set(key.replace('/', '.').replace(':', '.'), value);
      }
    }
  }
}
