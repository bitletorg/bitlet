/*
 *              bitlet - Simple bittorrent library
 *
 * Copyright (C) 2008 Alessandro Bahgat Shehata, Daniele Castagna
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Alessandro Bahgat Shehata - ale dot bahgat at gmail dot com
 * Daniele Castagna - daniele dot castagna at gmail dot com
 *
 */

package org.bitlet.wetorrent.util;

import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;

public class Utils {

    public static String byteArrayToURLString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) {
            return null;
        }
        String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F"
        };
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            // First check to see if we need ASCII or HEX
            if ((in[i] >= '0' && in[i] <= '9') || (in[i] >= 'a' && in[i] <= 'z') || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '-' || in[i] == '_' || in[i] == '.' || in[i] == '~') {
                out.append((char) in[i]);
                i++;
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0); // Strip off high nibble

                ch = (byte) (ch >>> 4); // shift the bits down

                ch = (byte) (ch & 0x0F); // must do this is high order bit is
                // on!

                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character

                ch = (byte) (in[i] & 0x0F); // Strip off low nibble

                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character

                i++;
            }
        }

        String rslt = new String(out);

        return rslt;

    }

    /**
     *
     * Convert a byte[] array to readable string format. This makes the "hex"
     * readable!
     */
    // Taken from http://www.devx.com/tips/Tip/13540
    public static String byteArrayToByteString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) {
            return null;
        }
        String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F"
        };
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0); // Strip off high nibble

            ch = (byte) (ch >>> 4); // shift the bits down

            ch = (byte) (ch & 0x0F); // must do this is high order bit is on!

            out.append(pseudo[(int) ch]); // convert the nibble to a String
            // Character

            ch = (byte) (in[i] & 0x0F); // Strip off low nibble

            out.append(pseudo[(int) ch]); // convert the nibble to a String
            // Character

            i++;
        }

        String rslt = new String(out);

        return rslt;
    }

    public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

    public static boolean bytesCompare(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase();
    }

    public static String escapeJavascriptString(String string) {
        if (string == null) {
            return null;
        }
        StringWriter writer = new StringWriter(string.length() * 2);
        {

            int sz;
            sz = string.length();
            for (int i = 0; i < sz; i++) {
                char ch = string.charAt(i);

                // handle unicode
                if (ch > 0xfff) {
                    writer.write("\\u" + hex(ch));
                } else if (ch > 0xff) {
                    writer.write("\\u0" + hex(ch));
                } else if (ch > 0x7f) {
                    writer.write("\\u00" + hex(ch));
                } else if (ch < 32) {
                    switch (ch) {
                        case '\b':
                            writer.write('\\');
                            writer.write('b');
                            break;
                        case '\n':
                            writer.write('\\');
                            writer.write('n');
                            break;
                        case '\t':
                            writer.write('\\');
                            writer.write('t');
                            break;
                        case '\f':
                            writer.write('\\');
                            writer.write('f');
                            break;
                        case '\r':
                            writer.write('\\');
                            writer.write('r');
                            break;
                        default:
                            if (ch > 0xf) {
                                writer.write("\\u00" + hex(ch));
                            } else {
                                writer.write("\\u000" + hex(ch));
                            }
                            break;
                    }
                } else {
                    switch (ch) {
                        case '\'':
                            writer.write('\\');
                            writer.write('\'');
                            break;
                        case '"':
                            writer.write('\\');
                            writer.write('"');
                            break;
                        case '\\':
                            writer.write('\\');
                            writer.write('\\');
                            break;
                        default:
                            writer.write(ch);
                            break;
                    }
                }
            }
        }
        return writer.toString();
    }

    /**
     * 
     * @param byteNumber
     * @return
     * @deprecated The javascript version should be used, instead
     */
    @Deprecated
    static public String byteToHumanReadableString(long byteNumber) {
        if (byteNumber < 1 << 10) {
            return byteNumber + " B";
        } else if (byteNumber < 1 << 20) {
            Double kiloBytePerSecond = (double) byteNumber / (1 << 10);
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            StringBuffer sb = df.format(kiloBytePerSecond, new StringBuffer(), new FieldPosition(NumberFormat.FRACTION_FIELD));
            return sb + " KB";
        } else if (byteNumber < 1 << 30) {
            Double megaBytePerSecond = (double) byteNumber / (1 << 20);
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            StringBuffer sb = df.format(megaBytePerSecond, new StringBuffer(), new FieldPosition(NumberFormat.FRACTION_FIELD));
            return sb + " MB";
        } else {
            Double gigaBytePerSecond = (double) byteNumber / (1 << 30);
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            StringBuffer sb = df.format(gigaBytePerSecond, new StringBuffer(), new FieldPosition(NumberFormat.FRACTION_FIELD));
            return sb + " GB";
        }
    }
}
