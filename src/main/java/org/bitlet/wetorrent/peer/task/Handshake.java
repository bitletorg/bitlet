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
