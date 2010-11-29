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

package org.bitlet.wetorrent.peer;

import java.net.InetAddress;
import org.bitlet.wetorrent.peer.message.Message;

public interface Peer {

    public void sendMessage(Message message);

    public void setAmInterested(boolean b);

    public void setIsChoked(boolean b);

    byte[] getPeerId();

    long getDownloaded();

    InetAddress getIp();

    public int getPort();

    long getLastReceivedMessageMillis();

    int getUnfulfilledRequestNumber();

    long getUploaded();

    boolean hasPiece(int index);

    void interrupt();

    boolean isAmChoked();

    boolean isSeeder();
}
