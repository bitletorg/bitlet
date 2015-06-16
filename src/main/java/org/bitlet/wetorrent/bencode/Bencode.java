/*
 *              bitlet - Simple bittorrent library
 *  Copyright (C) 2008 Alessandro Bahgat Shehata, Daniele Castagna
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * This is just a little class that lets you read and write bencoded files.
 * It uses List, Map, Long, and ByteBuffer in memory to represent data
 */
package org.bitlet.wetorrent.bencode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Bencode {

    private Object rootElement = null;

    /**
     * This creates and parse a bencoded InputStream
     */
    public Bencode(InputStream is) throws IOException {
        if (!is.markSupported()) {
            throw new IOException("is.markSupported should be true");
        }
        rootElement = parse(is);
    }

    /**
     * This creates a new instance of Bencode class
     */
    public Bencode() {
    }

    /**
     * This method prints the bencoded file on the OutputStream os
     */
    public void print(OutputStream os) throws IOException {
        print(rootElement, os);
    }

    private void print(Object object, OutputStream os) throws IOException {
        if (object instanceof Long) {
            os.write('i');
            os.write(((Long) object).toString().getBytes());
            os.write('e');
        }
        if (object instanceof ByteBuffer) {
            byte[] byteString = ((ByteBuffer) object).array();
            os.write(Integer.toString(byteString.length).getBytes());
            os.write(':');
            for (int i = 0; i < byteString.length; i++) {
                os.write(byteString[i]);
            }
        } else if (object instanceof List) {
            List list = (List) object;
            os.write('l');
            for (Object elem : list) {
                print(elem, os);
            }
            os.write('e');
        } else if (object instanceof Map) {
            Map map = (Map) object;
            os.write('d');

            SortedMap<ByteBuffer, Object> sortedMap = new TreeMap<ByteBuffer, Object>(new DictionaryComparator());
            // sortedMap.putAll(map);

            for (Object elem : map.entrySet()) {
                Map.Entry entry = (Map.Entry) elem;
                sortedMap.put((ByteBuffer) entry.getKey(), entry.getValue());
            }

            for (Object elem : sortedMap.entrySet()) {
                Map.Entry entry = (Map.Entry) elem;
                print(entry.getKey(), os);
                print(entry.getValue(), os);
            }
            os.write('e');
        }
    }

    private Object parse(InputStream is) throws IOException {
        is.mark(0);
        int readChar = is.read();
        switch (readChar) {
            case 'i':
                return parseInteger(is);
            case 'l':
                return parseList(is);
            case 'd':
                return parseDictionary(is);
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
                is.reset();
                return parseByteString(is);
            default:
                throw new IOException("Problem parsing bencoded file");
        }
    }

    public Object getRootElement() {
        return rootElement;
    }

    public void setRootElement(Object rootElement) {
        this.rootElement = rootElement;
    }

    private Long parseInteger(InputStream is) throws IOException {

        int readChar = is.read();

        StringBuffer buff = new StringBuffer();
        do {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            buff.append((char) readChar);
            readChar = is.read();
        } while (readChar != 'e');

        // System.out.println("Loaded int: " + buff);
        return Long.parseLong(buff.toString());
    }

    private List<Object> parseList(InputStream is) throws IOException {

        List<Object> list = new LinkedList<Object>();
        is.mark(0);
        int readChar = is.read();
        while (readChar != 'e') {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            is.reset();
            list.add(parse(is));
            is.mark(0);
            readChar = is.read();
        }

        return list;
    }

    private SortedMap parseDictionary(InputStream is) throws IOException {
        SortedMap<ByteBuffer, Object> map = new TreeMap<ByteBuffer, Object>(new DictionaryComparator());
        is.mark(0);
        int readChar = is.read();
        while (readChar != 'e') {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            is.reset();
            map.put(parseByteString(is), parse(is));
            is.mark(0);
            readChar = is.read();
        }

        return map;
    }

    private ByteBuffer parseByteString(InputStream is) throws IOException {

        int readChar = is.read();

        StringBuffer buff = new StringBuffer();
        do {
            if (readChar < 0) {
                throw new IOException("Unexpected EOF found");
            }
            buff.append((char) readChar);
            readChar = is.read();
        } while (readChar != ':');
        Integer length = Integer.parseInt(buff.toString());

        byte[] byteString = new byte[length];
        for (int i = 0; i < byteString.length; i++) {
            byteString[i] = (byte) is.read();
        // System.out.println("Loaded string: " + new String(byteString));
        }
        return ByteBuffer.wrap(byteString);
    }
}
