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
