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

package org.bitlet.wetorrent.peer.task;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;
import org.bitlet.wetorrent.Event;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.peer.IncomingPeerListener;
import org.bitlet.wetorrent.peer.TorrentPeer;
import org.bitlet.wetorrent.util.stream.OutputStreamLimiter;
import org.bitlet.wetorrent.util.thread.ThreadTask;
import org.bitlet.wetorrent.util.Utils;

public class Handshake implements ThreadTask {

    TorrentPeer peer;

    public Handshake(TorrentPeer peer) {
        this.peer = peer;
    }
    IncomingPeerListener incomingPeerListener = null;

    public Handshake(TorrentPeer peer, IncomingPeerListener incomingPeerListener) {
        this.peer = peer;
        this.incomingPeerListener = incomingPeerListener;
    }

    private void sendProtocolHeader(TorrentPeer peer) throws IOException {
        DataOutputStream os = new DataOutputStream(new OutputStreamLimiter(peer.getSocket().getOutputStream(), peer.getPeersManager().getTorrent().getUploadBandwidthLimiter()));
        os.writeByte(19);
        os.write("BitTorrent protocol".getBytes());
        os.write(new byte[8]);
        os.write(peer.getPeersManager().getTorrent().getMetafile().getInfoSha1());
        os.write(peer.getPeersManager().getTorrent().getPeerId());
    }

    public boolean execute() throws Exception {

        try {
            DataInputStream is = new DataInputStream(peer.getSocket().getInputStream());


            /* if *we* are starting the connection */
            if (peer.getPeersManager() != null && incomingPeerListener == null) {
                sendProtocolHeader(peer);
            }
            int protocolIdentifierLength = is.readByte();

            if (protocolIdentifierLength != 19) {
                throw new Exception("Error, wrong protocol identifier length " + protocolIdentifierLength);
            }
            byte[] protocolByteString = new byte[protocolIdentifierLength];
            is.readFully(protocolByteString);

            if (!Utils.bytesCompare("BitTorrent protocol".getBytes(), protocolByteString)) {
                throw new Exception("Error, wrong protocol identifier");
            }
            byte[] reserved = new byte[8];
            is.readFully(reserved);

            byte[] infoHash = new byte[20];
            is.readFully(infoHash);
            /* if it's an incoming connection */
            if (peer.getPeersManager() == null && incomingPeerListener != null) {
                if (!incomingPeerListener.dispatchPeer(peer, infoHash)) {
                    peer.getSocket().close();
                    throw new Exception("Wrong info hash");
                }
                sendProtocolHeader(peer);

            } else if (!Utils.bytesCompare(infoHash, peer.getPeersManager().getTorrent().getMetafile().getInfoSha1())) {
                peer.getSocket().close();
                throw new Exception("Wrong info hash");
            }

            byte[] peerId = new byte[20];
            is.readFully(peerId);

            if (Torrent.verbose) {
                peer.getPeersManager().getTorrent().addEvent(new Event(this, "new peerId: " + Utils.byteArrayToURLString(peerId), Level.INFO));
            }
            if (Utils.bytesCompare(peerId, peer.getPeersManager().getTorrent().getPeerId())) {
                peer.getSocket().close();
                throw new Exception("Avoid self connections");
            }

            if (peer.getPeerId() != null) {
                if (!Utils.bytesCompare(peer.getPeerId(), peerId)) {
                    peer.getSocket().close();
                    throw new Exception("Wrong peer id");
                }
            } else {
                peer.setPeerId(peerId);
            }
            if (Torrent.verbose) {
                peer.getPeersManager().getTorrent().addEvent(new Event(this, "Connection header correctly parsed ", Level.FINER));
            }
            peer.getPeersManager().connected(peer);

        } catch (IOException e) {
            if (Torrent.verbose) {
                System.err.println("Problem parsing header");
            }
            throw e;
        }
        return false;
    }

    public void interrupt() {
        try {
            peer.getSocket().close();
        } catch (IOException ex) {
        }
    }

    public void exceptionCought(Exception e) {
        if (e instanceof EOFException) {
            if (Torrent.verbose) {
                System.err.println("Connection dropped");
            }
        }
        if (incomingPeerListener != null) {
            incomingPeerListener.removePeer(peer);
        }
        peer.interrupt();
    }
}
