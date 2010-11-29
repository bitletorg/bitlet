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

package org.bitlet.wetorrent.choker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.peer.Peer;

public class Choker {

    private Torrent torrent;
    private static final int maxUnchoked = 16;
    private Map<Peer, Long> unchoked = new HashMap<Peer, Long>();
    private Set<Peer> interested = new HashSet<Peer>();

    public Choker(Torrent torrent) {
        this.torrent = torrent;
    }

    public synchronized void interested(Peer peer) {
        interested.add(peer);
        if (unchoked.size() < maxUnchoked) {
            peer.setIsChoked(false);
            unchoked.put(peer, System.currentTimeMillis());
        }
    }

    public synchronized void notInterested(Peer peer) {
        peer.setIsChoked(true);
        unchoked.remove(peer);
        interested.remove(peer);
    }

    public synchronized void choke(Peer peer) {
    }

    public synchronized void unchoke(Peer peer) {
    }

    public synchronized void interrupted(Peer peer) {
        unchoked.remove(peer);
        interested.remove(peer);
    }

    public synchronized void tick() {


        if (unchoked.size() == maxUnchoked && interested.size() > maxUnchoked) {
            long now = System.currentTimeMillis();
            Peer slowest = null;
            for (Map.Entry<Peer, Long> e : unchoked.entrySet()) {

                if (now - e.getValue() > 10000) {
                    if (slowest == null) {
                        slowest = e.getKey();
                    } else {
                        if ((float) slowest.getDownloaded() / (now - unchoked.get(slowest)) >
                                (float) e.getKey().getDownloaded() / (now - e.getValue())) {
                            slowest = e.getKey();
                        }
                    }
                }
            }

            if (slowest != null) {
                slowest.setIsChoked(true);
                unchoked.remove(slowest);

                List<Peer> interestedChoked = new LinkedList<Peer>(interested);
                interestedChoked.removeAll(unchoked.keySet());

                Peer newUnchoked = interestedChoked.get(new Random().nextInt(interestedChoked.size()));
                newUnchoked.setIsChoked(false);
                unchoked.put(newUnchoked, now);
            }

        }

    }
}
