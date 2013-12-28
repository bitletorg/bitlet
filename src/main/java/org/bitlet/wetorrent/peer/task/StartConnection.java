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

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import org.bitlet.wetorrent.Event;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.peer.TorrentPeer;
import org.bitlet.wetorrent.util.thread.ThreadTask;

public class StartConnection implements ThreadTask {

    boolean interrupted = false;
    private TorrentPeer peer;

    public StartConnection(TorrentPeer peer) {
        this.peer = peer;
    }

    public boolean execute() throws Exception {
        Socket socket = connect(peer.getIp(), peer.getPort());
        peer.setSocket(socket);
        if (socket != null) {
            if (Torrent.verbose) {
                peer.getPeersManager().getTorrent().addEvent(new Event(this, "Connected to " + peer.getIp(), Level.FINE));
            }
        } else {
            throw new Exception("Problem connecting to " + peer.getIp());
        }
        return false;

    }

    public void interrupt() {
        interrupted = true;
    }

    public synchronized boolean isInterrupted() {
        return interrupted;
    }

    public synchronized Socket connect(InetAddress address, int port) throws Exception {
        if (!interrupted) {
            return new Socket(address, port);
        } else {
            throw new Exception("Interrupted before connecting");
        }
    }

    public void exceptionCought(Exception e) {
        if (e instanceof ConnectException) {
            if (Torrent.verbose) {
                peer.getPeersManager().getTorrent().addEvent(new Event(this, "Connection refused: " + peer.getIp(), Level.FINE));
            }
        }
        peer.interrupt();
    }
}
