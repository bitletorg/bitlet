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
package org.bitlet.wetorrent;

import static org.bitlet.wetorrent.util.Utils.toByteBuffer;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Unit tests for {@link Metafile}. */
public class MetafileTest {

  private static String TEST_TORRENT_PATH = "/ubuntu-15.04-desktop-amd64.iso.torrent";

  @Test
  public void readMetafile() throws Exception {
    InputStream stream = MetafileTest.class.getResourceAsStream(TEST_TORRENT_PATH);
    Metafile metafile = new Metafile(stream);
    assertEquals("http://torrent.ubuntu.com:6969/announce", metafile.getAnnounce());
    List announceList = new LinkedList();
    List firstList = new LinkedList();
    firstList.add(toByteBuffer("http://torrent.ubuntu.com:6969/announce"));
    announceList.add(firstList);
    List secondList = new LinkedList();
    secondList.add(toByteBuffer("http://ipv6.torrent.ubuntu.com:6969/announce"));
    announceList.add(secondList);
    assertEquals(announceList, metafile.getAnnounceList());
    assertEquals("Ubuntu CD releases.ubuntu.com", metafile.getComment());
    assertNull(metafile.getCreatedBy());
    assertEquals(1429786237L, metafile.getCreationDate().longValue());
    assertEquals(Collections.emptyList(), metafile.getFiles());
    // TODO assert on metafile.getInfo());
    assertEquals(1150844928L, metafile.getLength());
    assertEquals("ubuntu-15.04-desktop-amd64.iso", metafile.getName());
    assertEquals(524288L, metafile.getPieceLength().longValue());
    assertEquals(2196, metafile.getPieces().size());
  }
  
  @Test
  public void updateManifestComment() throws Exception {
    InputStream stream = MetafileTest.class.getResourceAsStream(TEST_TORRENT_PATH);
    Metafile metafile = new Metafile(stream);
    metafile.setComment("foo");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    metafile.print(outputStream);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    Metafile metafileWithUpdatedFields = new Metafile(inputStream);
    assertEquals("foo", metafile.getComment());
  }
}
