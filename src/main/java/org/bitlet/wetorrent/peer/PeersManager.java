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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.bitlet.wetorrent.Event;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.peer.message.Have;
import org.bitlet.wetorrent.peer.message.Message;
import org.bitlet.wetorrent.util.Utils;

public class PeersManager {

    private List<Peer> connectingPeers = new LinkedList<Peer>();
    private List<Peer> activePeers = new LinkedList<Peer>();
    /**
     * Creates a new instance of PeersManager
     */
    private int connectionCreationTreshold = 35;
    private int maxConnection = 45;
    private Torrent torrent;
    private long disconnectedClientDownloaded = 0l;
    private long disconnectedClientUploaded = 0l;

    public PeersManager(Torrent torrent) {
        this.torrent = torrent;
    }

    /*
     * This is called when we would like to start a connection to
     */
    public synchronized TorrentPeer offer(byte[] peerId, InetAddress ip, int port) {

        // TODO: this could be optimized with a proper indexing
        for (Peer peer : connectingPeers) {
            if (peer.getPort() == port && peer.getIp().equals(ip)) {
                return null;
            }
        }
        for (Peer peer : activePeers) {
            if (peer.getPort() == port && peer.getIp().equals(ip)) {
                return null;
            }
        }

        if (activePeers.size() < connectionCreationTreshold) {
            TorrentPeer peer = new TorrentPeer(peerId, ip, port, this);

            if (Torrent.verbose) {
                torrent.addEvent(new Event(this, "Starting connection to new peer: " + ip, Level.FINE));
            }
            peer.start();
            connectingPeers.add(peer);
            return peer;
        }

        if (Torrent.verbose) {
            torrent.addEvent(new Event(this, "Too many connection, peer refused: " + ip, Level.FINER));
        }
        return null;
    }

    public synchronized TorrentPeer offer(TorrentPeer peer) {
        if (activePeers.size() > maxConnection) {
            if (Torrent.verbose) {
                torrent.addEvent(new Event(this, "Refusing incoming connection: too many connection", Level.FINER));
            }
            peer.interrupt();
            return null;
        }

        if (Torrent.verbose) {
            torrent.addEvent(new Event(this, "Accpeting incoming peer connection ", Level.FINER));
        }
        peer.setPeersManager(this);
        connectingPeers.add(peer);
        return peer;
    }

    public Torrent getTorrent() {
        return torrent;
    }

    public synchronized long getUploaded() {
        long uploaded = disconnectedClientUploaded;
        for (Peer p : activePeers) {
            uploaded += p.getUploaded();
        }
        return uploaded;
    }

    public synchronized void interrupted(Peer peer) {
        connectingPeers.remove(peer);
        if (activePeers.remove(peer)) {
            torrent.interrupted(peer);
            disconnectedClientDownloaded += peer.getDownloaded();
            disconnectedClientUploaded += peer.getUploaded();
        }

        if (Torrent.verbose) {
            torrent.addEvent(new Event(this, activePeers.size() + " active peers, " + connectingPeers.size() + "connecting peer", Level.INFO));
        }
    }

    public synchronized int[] getPiecesFrequencies() {
        int[] frequencies = new int[torrent.getMetafile().getPieces().size()];

        for (int i = 0; i < frequencies.length; i++) {
            for (Peer p : activePeers) {
                if (p.hasPiece(i)) {
                    frequencies[i]++;
                }
            }
        }

        return frequencies;
    }

    public synchronized long getDownloaded() {
        long downloaded = disconnectedClientDownloaded;
        for (Peer p : activePeers) {
            downloaded += p.getDownloaded();
        }
        return downloaded;

    }

    public synchronized void tick() {
        long now = System.currentTimeMillis();

        List<Peer> peersTimedOut = new LinkedList<Peer>();
        // TODO: Remove hardcoded millis
        for (Peer p : activePeers) {
            if (now - p.getLastReceivedMessageMillis() > 120000) {
                peersTimedOut.add(p);
            } else if (now - p.getLastReceivedMessageMillis() > 110000) {
                p.sendMessage(new Message(Message.KEEP_ALIVE, null));
            }
        }

        for (Peer p : peersTimedOut) {
            p.interrupt();
            interrupted(p);
        }
    }

    public synchronized void interrupt() {
        while (connectingPeers.size() > 0) {
            connectingPeers.get(0).interrupt();
        }
        while (activePeers.size() > 0) {
            activePeers.get(0).interrupt();
        }
    }

    public synchronized void sendHave(Have have) {
        for (Peer p : activePeers) {
            if (!p.hasPiece(have.getIndex())) {
                p.sendMessage(have);
            }
        }
    }

    public synchronized int getActivePeersNumber() {
        return activePeers.size();
    }

    public synchronized int getSeedersNumber() {
        int acc = 0;
        for (Peer peer : activePeers)
            acc += peer.isSeeder() ? 1 : 0;
        return acc;
    }
    public synchronized void connected(Peer peer) {

        if (!connectingPeers.remove(peer)) {
            if (Torrent.verbose) {
                torrent.addEvent(new Event(peer, "Peer connected", Level.WARNING));
            }
        }
        for (Peer p : activePeers) {
            if (Utils.bytesCompare(peer.getPeerId(), p.getPeerId())) {
                peer.interrupt();
                torrent.addEvent(new Event(this, "Peer already connected", Level.FINE));
                return;
            }
        }

        activePeers.add(peer);
    }
}
