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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import org.bitlet.wetorrent.Event;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.peer.message.Message;
import org.bitlet.wetorrent.peer.TorrentPeer;
import org.bitlet.wetorrent.peer.message.Piece;
import org.bitlet.wetorrent.util.stream.OutputStreamLimiter;
import org.bitlet.wetorrent.util.thread.ThreadTask;

public class MessageSender implements ThreadTask {

    private Queue<Message> messagesToBeSent = new LinkedList<Message>();
    private boolean interrupted = false;
    private TorrentPeer peer;
    private long sentBytes;
    private long lastSentMessageMillis;
    private long uploaded = 0;

    public MessageSender(TorrentPeer peer) {
        this.peer = peer;
    }

    public synchronized void addSentBytes(Integer byteNumber) {
        sentBytes += byteNumber;
    }

    public synchronized long getSentBytes() {
        return sentBytes;
    }

    public boolean execute() throws Exception {

        try {

            Message message = getMessage();
            if (message != null) {
                DataOutputStream os = new DataOutputStream(new OutputStreamLimiter(peer.getSocket().getOutputStream(), peer.getPeersManager().getTorrent().getUploadBandwidthLimiter()));

                if (message.getType() != Message.KEEP_ALIVE) {

                    byte[] payload = message.getPayload();

                    if (payload != null) {
                        os.writeInt(payload.length + 1);
                        os.writeByte(message.getType());
                        addSentBytes(4 + 1);

                        int payloadOffset = 0;
                        while (payloadOffset < payload.length) {
                            int payloadMissing = payload.length - payloadOffset;
                            int payloadChunk = payloadMissing > 1 << 10 ? 1 << 10 : payloadMissing;
                            os.write(payload, payloadOffset, payloadChunk);
                            addSentBytes(payloadChunk);

                            if (message.getType() == message.PIECE) {
                                uploaded += payloadChunk - (payloadOffset == 0 ? 13 : 0);
                            }
                            payloadOffset += payloadChunk;
                        }

                    } else {
                        os.writeInt(1);
                        os.writeByte(message.getType());
                        addSentBytes(4 + 1);
                    }


                } else { /* keep alive */
                    os.writeInt(0);
                    addSentBytes(4);
                }

                setLastSentMessageMillis(System.currentTimeMillis());
                if (Torrent.verbose) {
                    peer.getPeersManager().getTorrent().addEvent(new Event(this, "Message sent " + message.getType(), Level.FINEST));
                }
            }

        } catch (Exception e) {
            if (Torrent.verbose) {
                peer.getPeersManager().getTorrent().addEvent(new Event(this, "Problem sending message", Level.WARNING));
            }
            throw e;
        }
        return true;
    }

    public synchronized void addMessage(Message message) {
        messagesToBeSent.add(message);
        notify();
    }

    private synchronized Message getMessage() {
        while (messagesToBeSent.size() == 0 && !interrupted) {
            try {
                wait(2000);
            } catch (InterruptedException ex) {
            }
        }
        return messagesToBeSent.poll();
    }

    public synchronized void interrupt() {
        try {
            peer.getSocket().close();
        } catch (IOException ex) {
        }
        messagesToBeSent.clear();
        interrupted = true;
        notify();
    }

    public void exceptionCought(Exception e) {
        peer.interrupt();
        e.printStackTrace();
    }

    public synchronized void cancel(int index, int begin, int length) {
        Collection<Message> messagesToCancel = new LinkedList<Message>();
        for (Message elem : messagesToBeSent) {
            if (elem.getType() == Message.PIECE) {
                Piece block = (Piece) elem;
                if (block.getIndex() == index && block.getBegin() == begin && block.getLength() == length) {
                    messagesToCancel.add(elem);
                }
            }
        }

        messagesToBeSent.removeAll(messagesToCancel);
    }

    public synchronized void cancelAll() {
        Collection<Message> messagesToCancel = new LinkedList<Message>();
        for (Message elem : messagesToBeSent) {
            if (elem.getType() == Message.PIECE) {
                messagesToCancel.add(elem);
            }
        }

        messagesToBeSent.removeAll(messagesToCancel);
    }

    public long getUploaded() {
        return uploaded;
    }

    public synchronized long getLastSentMessageMillis() {
        return lastSentMessageMillis;
    }

    public synchronized void setLastSentMessageMillis(long lastSentMessageMillis) {
        this.lastSentMessageMillis = lastSentMessageMillis;
    }
}