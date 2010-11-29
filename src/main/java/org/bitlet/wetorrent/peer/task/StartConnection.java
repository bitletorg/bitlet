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
