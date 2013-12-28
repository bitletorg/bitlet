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

/*
 * This is a very simple byte string comparator.
 */
package org.bitlet.wetorrent.bencode;

import java.nio.ByteBuffer;
import java.util.Comparator;

public class DictionaryComparator implements Comparator<ByteBuffer> {

    public DictionaryComparator() {
    }

    public int bitCompare(byte b1, byte b2) {
        int int1 = b1 & 0xFF;
        int int2 = b2 & 0xFF;
        return int1 - int2;
    }

    public int compare(ByteBuffer o1, ByteBuffer o2) {
        byte[] byteString1 = o1.array();
        byte[] byteString2 = o2.array();
        int minLength = byteString1.length > byteString2.length ? byteString2.length : byteString1.length;
        for (int i = 0; i < minLength; i++) {
            int bitCompare = bitCompare(byteString1[i], byteString2[i]);
            if (bitCompare != 0) {
                return bitCompare;
            }
        }

        if (byteString1.length > byteString2.length) {
            return 1;
        } else if (byteString1.length < byteString2.length) {
            return -1;
        }
        return 0;
    }
}
