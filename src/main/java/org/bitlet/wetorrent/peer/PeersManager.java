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
