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

/**
 * The {@link LineHelper} is a utility class to .
 */
abstract class LineHelper {

  private static final char[] HEX_DIGITS =
      { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

  /**
   * Convert a nibble to a hex character
   *
   * @param nibble the nibble to convert.
   */
  private static char toHex(int nibble) {
    return LineHelper.HEX_DIGITS[(nibble & 0xF)];
  }

  /**
   * Write comment to the {@link BufferedWriter}.
   *
   * @param writer
   * @param comments
   * @param character
   */
  public static void writeComment(BufferedWriter writer, String comments, char character) throws IOException {
    writer.write(character);
    writer.write(' ');
    int len = comments.length();
    int current = 0;
    int last = 0;
    char[] uu = new char[6];
    uu[0] = '\\';
    uu[1] = 'u';
    while (current < len) {
      char c = comments.charAt(current);
      if ((c > '\u00ff') || (c == '\n') || (c == '\r')) {
        if (last != current) {
          writer.write(comments.substring(last, current));
        }
        if (c > '\u00ff') {
          uu[2] = LineHelper.toHex((c >> 12) & 0xf);
          uu[3] = LineHelper.toHex((c >> 8) & 0xf);
          uu[4] = LineHelper.toHex((c >> 4) & 0xf);
          uu[5] = LineHelper.toHex(c & 0xf);
          writer.write(new String(uu));
        } else {
          writer.newLine();
          if ((c == '\r') && (current != (len - 1)) && (comments.charAt(current + 1) == '\n')) {
            current++;
          }
          if ((current == (len - 1))
              || ((comments.charAt(current + 1) != '#') && (comments.charAt(current + 1) != '!'))) {
            writer.write("#");
          }
        }
        last = current + 1;
      }
      current++;
    }
    if (last != current) {
      writer.write(comments.substring(last, current));
    }
    writer.newLine();
  }

  /**
   * Converts encoded &#92;uxxxx to unicode chars and changes special saved chars to their original
   * format.
   *
   * @param buffer
   * @param offset
   * @param length
   * @param tmpBuffer
   */
  public static String toString(char[] buffer, int offset, int length, char[] tmpBuffer) {
    if (tmpBuffer.length < length) {
      int newLen = length * 2;
      if (newLen < 0) {
        newLen = Integer.MAX_VALUE;
      }
      tmpBuffer = new char[newLen];
    }
    char aChar;
    char[] out = tmpBuffer;
    int outLen = 0;
    int end = offset + length;

    while (offset < end) {
      aChar = buffer[offset++];
      if (aChar == '\\') {
        aChar = buffer[offset++];
        if (aChar == 'u') {
          // Read the xxxx
          int value = 0;
          for (int i = 0; i < 4; i++) {
            aChar = buffer[offset++];
            switch (aChar) {
              case '0':
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7':
              case '8':
              case '9':
                value = ((value << 4) + aChar) - '0';
                break;
              case 'a':
              case 'b':
              case 'c':
              case 'd':
              case 'e':
              case 'f':
                value = ((value << 4) + 10 + aChar) - 'a';
                break;
              case 'A':
              case 'B':
              case 'C':
              case 'D':
              case 'E':
              case 'F':
                value = ((value << 4) + 10 + aChar) - 'A';
                break;
              default:
                throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
            }
          }
          out[outLen++] = (char) value;
        } else {
          if (aChar == 't') {
            aChar = '\t';
          } else if (aChar == 'r') {
            aChar = '\r';
          } else if (aChar == 'n') {
            aChar = '\n';
          } else if (aChar == 'f') {
            aChar = '\f';
          }
          out[outLen++] = aChar;
        }
      } else {
        out[outLen++] = aChar;
      }
    }
    return new String(out, 0, outLen);
  }

  /**
   * Converts Unicode to encoded &#92;uxxxx and escapes special characters with a preceding slash
   *
   * @param text
   * @param escapeSpace
   * @param escapeUnicode
   */
  static String toAscii(String text) {
    int len = text.length();
    int length = len * 2;
    if (length < 0) {
      length = Integer.MAX_VALUE;
    }

    StringBuffer buffer = new StringBuffer(length);
    for (int x = 0; x < len; x++) {
      char aChar = text.charAt(x);
      if (((aChar < 0x0020) || (aChar > 0x007e))) {
        buffer.append('\\');
        buffer.append('u');
        buffer.append(LineHelper.toHex((aChar >> 12) & 0xF));
        buffer.append(LineHelper.toHex((aChar >> 8) & 0xF));
        buffer.append(LineHelper.toHex((aChar >> 4) & 0xF));
        buffer.append(LineHelper.toHex(aChar & 0xF));
      } else {
        buffer.append(aChar);
      }
    }
    return buffer.toString();
  }

  /**
   * Converts Unicode to encoded &#92;uxxxx and escapes special characters with a preceding slash
   *
   * @param text
   * @param escape
   */
  static String escape(String text, boolean escape) {
    int len = text.length();
    int bufLen = len * 2;
    if (bufLen < 0) {
      bufLen = Integer.MAX_VALUE;
    }
    StringBuffer buffer = new StringBuffer(bufLen);

    for (int x = 0; x < len; x++) {
      char aChar = text.charAt(x);
      // Handle common case first, selecting largest block that
      // avoids the specials below
      if ((aChar > 61) && (aChar < 127)) {
        if (aChar == '\\') {
          buffer.append('\\');
          buffer.append('\\');
          continue;
        }
        buffer.append(aChar);
        continue;
      }
      switch (aChar) {
        case ' ':
          if ((x == 0) || escape) {
            buffer.append('\\');
          }
          buffer.append(' ');
          break;
        case '\t':
          buffer.append('\\');
          buffer.append('t');
          break;
        case '\n':
          buffer.append('\\');
          buffer.append('n');
          break;
        case '\r':
          buffer.append('\\');
          buffer.append('r');
          break;
        case '\f':
          buffer.append('\\');
          buffer.append('f');
          break;
        case '=': // Fall through
        case ':': // Fall through
          buffer.append('\\');
          buffer.append(aChar);
          break;
        case '#': // Fall through
        case ';': // Fall through
        case '!':
          buffer.append('\\');
          buffer.append(aChar);
          break;
        default:
          buffer.append(aChar);
      }
    }
    return buffer.toString();
  }
}
