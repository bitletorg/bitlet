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

package org.bitlet.wetorrent.peer.message;

import java.io.IOException;
import org.bitlet.wetorrent.disk.TorrentDisk;
import org.bitlet.wetorrent.util.Utils;

public class Piece extends Message {

    private int index;
    private int length;
    private int begin;
    private TorrentDisk torrentDisk;

    /** Creates a new instance of Piece */
    public Piece(int index, int begin, int length, TorrentDisk torrentDisk) {
        super(Message.PIECE, null);

        this.torrentDisk = torrentDisk;
        this.index = index;
        this.length = length;
        this.begin = begin;
    }

    public int getIndex() {
        return index;
    }

    public int getLength() {
        return length;
    }

    public int getBegin() {
        return begin;
    }

    public byte[] getPayload() {
        if (super.getPayload() == null) {
            try {

                byte[] block = torrentDisk.read(index, begin, length);
                byte[] payload = new byte[block.length + 8];

                System.arraycopy(block, 0, payload, 8, block.length);
                System.arraycopy(Utils.intToByteArray(index), 0, payload, 0, 4);
                System.arraycopy(Utils.intToByteArray(begin), 0, payload, 4, 4);

                setPayload(payload);
            } catch (IOException ex) {
            }
        }
        return super.getPayload();

    }
}
