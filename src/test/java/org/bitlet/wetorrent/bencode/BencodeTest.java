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
package org.bitlet.wetorrent.bencode;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.bitlet.wetorrent.util.Utils.toByteBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Unit tests for {@link Bencode}. */
public class BencodeTest {

  @Test
  public void testParsing() throws Exception {
    String bencodedDictionary = "d3:str4:spam3:numi42e4:listli1ei2eee";
    ByteArrayInputStream testStream =
        new ByteArrayInputStream(bencodedDictionary.getBytes(StandardCharsets.UTF_8));
    Bencode bencode = new Bencode(testStream);
    Map<ByteBuffer, Object> result = (Map<ByteBuffer, Object>) bencode.getRootElement();
    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(toByteBuffer("spam"), result.get(toByteBuffer("str")));
    assertEquals(42L, result.get(toByteBuffer("num")));
    List<Long> list = (List<Long>) result.get(toByteBuffer("list"));
    assertEquals(1L, list.get(0).longValue());
    assertEquals(2L, list.get(1).longValue());
  }
  
  @Test
  public void testPrint() throws Exception {
    Map source = new TreeMap();
    source.put(toByteBuffer("str"), toByteBuffer("spam"));
    source.put(toByteBuffer("num"), 42L);
    source.put(toByteBuffer("list"), Arrays.asList(1L, 2L));
    Bencode bencode = new Bencode();
    bencode.setRootElement(source);
    ByteArrayOutputStream testStream = new ByteArrayOutputStream();
    bencode.print(testStream);
    String result = testStream.toString(StandardCharsets.UTF_8.name());
    String expected = "d4:listli1ei2ee3:numi42e3:str4:spame";
    assertEquals(expected, result);
  }
}
