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