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

package org.bitlet.wetorrent.pieceChooser;

import java.util.Collection;
import java.util.LinkedList;
import org.bitlet.wetorrent.peer.Peer;
import org.bitlet.wetorrent.peer.message.Request;

// TODO: This really sucks and looks like a M$ piece of code.
// An HashMap would be probabily better even if a little bit slower.

public class RequestExtended extends Request implements Comparable<RequestExtended>{
    private long millisTimeStamp = 0;
    private Collection<Peer> peers = new LinkedList<Peer>();

    public RequestExtended(int index, int begin, int length){
        super(index, begin, length);
        this.millisTimeStamp = System.currentTimeMillis();
    }

    public long getMillisTimeStamp() {
        return millisTimeStamp;
    }

    public Collection<Peer> getPeers() {
        return peers;
    }

    public String toString() {
        return millisTimeStamp + " " + getIndex() + " " + getBegin() + " " + getLength();
    }

    public int compareTo(RequestExtended o) {
        return (int) (this.millisTimeStamp - o.millisTimeStamp);
    }


}