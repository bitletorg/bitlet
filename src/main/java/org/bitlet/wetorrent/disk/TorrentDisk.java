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

package org.bitlet.wetorrent.disk;

import java.io.IOException;

public interface TorrentDisk {

    public void resume() throws IOException;

    public void resume(ResumeListener rl) throws IOException;

    /* It returns true if a resume could be performed */
    public boolean init() throws IOException;

    public byte[] getBitfieldCopy();

    public void write(int index, int begin, byte[] block) throws IOException;

    public byte[] read(int index, int begin, int length) throws IOException;

    public Long getCompleted();

    public boolean isCompleted(int index);

    public int getDownloaded(int index);

    public boolean isAvailable(int index, int begin, int length);

    public long available(int index, int begin);

    public long available(int index, int begin, long maxLength);

    public int getLength(int index);

    public int getFirstMissingByte(int index);

    public void close();
}
