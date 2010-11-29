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
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.bitlet.wetorrent.Event;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.peer.message.Cancel;
import org.bitlet.wetorrent.peer.message.Piece;
import org.bitlet.wetorrent.peer.message.Message;
import org.bitlet.wetorrent.peer.message.Request;
import org.bitlet.wetorrent.peer.task.StartMessageReceiver;
import org.bitlet.wetorrent.peer.task.MessageReceiver;
import org.bitlet.wetorrent.peer.task.MessageSender;
import org.bitlet.wetorrent.peer.task.Handshake;
import org.bitlet.wetorrent.peer.task.SendBitfield;
import org.bitlet.wetorrent.peer.task.StartConnection;
import org.bitlet.wetorrent.util.thread.InterruptableTasksThread;
import org.bitlet.wetorrent.util.Utils;

public class TorrentPeer implements Peer {

    private byte[] peerId;
    private String peerIdEncoded;
    private int port;
    private InetAddress ip;
    private byte[] bitfield;
    private Socket socket;
    boolean isChoked = true;
    boolean isInterested = false;
    boolean amChoked = true;
    boolean amInterested = false;
    private PeersManager peersManager;
    private InterruptableTasksThread receiverThread;
    private InterruptableTasksThread mainThread;
    private long downloaded;
    private MessageSender messageSender;
    private MessageReceiver messageReceiver;
    private List<Request> unfulfilledRequests = new LinkedList<Request>();

    public TorrentPeer(byte[] peerId, InetAddress ip, int port, PeersManager peersManager) {
        this.peersManager = peersManager;
        this.peerId = peerId;
        peerIdEncoded = Utils.byteArrayToURLString(peerId);
        this.port = port;
        this.ip = ip;

        bitfield = new byte[peersManager.getTorrent().getTorrentDisk().getBitfieldCopy().length];

        mainThread = new InterruptableTasksThread();
        mainThread.addTask(new StartConnection(this));
        mainThread.addTask(new Handshake(this));
        mainThread.addTask(new SendBitfield(this));

        receiverThread = new InterruptableTasksThread();
        messageReceiver = new MessageReceiver(this);
        receiverThread.addTask(messageReceiver);
        mainThread.addTask(new StartMessageReceiver(this));
        messageSender = new MessageSender(this);
        mainThread.addTask(messageSender);

    }

    public TorrentPeer(Socket socket, IncomingPeerListener incomingPeerListener) {
        this.socket = socket;
        port = socket.getPort();
        ip = socket.getInetAddress();


        mainThread = new InterruptableTasksThread();
        mainThread.addTask(new Handshake(this, incomingPeerListener));
        mainThread.addTask(new SendBitfield(this));

        receiverThread = new InterruptableTasksThread();
        messageReceiver = new MessageReceiver(this);
        receiverThread.addTask(messageReceiver);
        mainThread.addTask(new StartMessageReceiver(this));
        messageSender = new MessageSender(this);
        mainThread.addTask(messageSender);

    }

    public void start() {
        mainThread.start();
    }

    public void exceptionCought(Exception e) {

        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(e, "UUoops exception cought.", Level.WARNING));
        }
        receiverThread.interrupt();
        mainThread.interrupt();
    }

    public String getPeerIdEncoded() {
        return peerIdEncoded;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public void setPeerId(byte[] peerId) {
        this.peerId = peerId;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public PeersManager getPeersManager() {
        return peersManager;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public synchronized void setBitfield(byte[] bitfield) {
        this.bitfield = bitfield;
    }

    /*messages sent by remote peer*/
    public void bitfield(byte[] bitfield) {

        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Bitfield received ", Level.FINEST));
        }
        setBitfield(bitfield);
        peersManager.getTorrent().bitfield(bitfield, this);
    }

    public void choke() {

        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Choke received ", Level.FINEST));
        }
        amChoked = true;
        peersManager.getTorrent().choke(this);
    }

    public void unchoke() {
        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Unchoke received ", Level.FINEST));
        }
        amChoked = false;
        /* if there are pending request not satisfied */
        messageSender.cancelAll();
        peersManager.getTorrent().unchoke(this);
    }

    public void interested() {
        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Interested received ", Level.FINEST));
        }
        isInterested = true;
        peersManager.getTorrent().interested(this);
    }

    public void notInterested() {
        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Not interested received ", Level.FINEST));
        }
        isInterested = false;
        peersManager.getTorrent().notInterested(this);
    }

    public void have(int i) {

        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Have received ", Level.FINEST));
        }
        setPiece(i);
        peersManager.getTorrent().have(i, this);
    }

    public void request(int index, int begin, int length) {
        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Request received ", Level.FINEST));
        /* TODO: Check block avaiabilty */
        }
        if (!isChoked) {
            messageSender.addMessage(new Piece(index, begin, length, peersManager.getTorrent().getTorrentDisk()));
        }
    }

    public void piece(int index, int begin, byte[] block) {
        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Piece received " + index + " " + begin + " " + block.length, Level.FINEST));
        }
        downloaded += block.length;
        if (requestFulfilled(index, begin, block))
            peersManager.getTorrent().piece(index, begin, block, this);
    }

    public void cancel(int index, int begin, int length) {
        if (Torrent.verbose) {
            peersManager.getTorrent().addEvent(new Event(this, "Cancel received ", Level.FINEST));
        }
        messageSender.cancel(index, begin, length);
    }

    public InterruptableTasksThread getReceiverThread() {
        return receiverThread;
    }

    public synchronized void sendMessage(Message message) {

        messageSender.addMessage(message);
        switch (message.getType()) {
            case Message.REQUEST:
                addRequest((Request) message);
                break;
            case Message.CANCEL:
                Cancel cancel = (Cancel) message;
                requestCanceled(cancel.getIndex(), cancel.getBegin());
                break;
        }


    }

    public void interrupt() {
        mainThread.interrupt();
        receiverThread.interrupt();
        if (peersManager != null) {
            peersManager.interrupted(this);
        }
    }

    public synchronized byte[] getBitfieldCopy() {
        return bitfield.clone();
    }

    public synchronized boolean hasPiece(int index) {
        return (bitfield[index >> 3] & (0x80 >> (index & 0x7))) > 0;
    }

    public synchronized void setPiece(int index) {
        bitfield[index >> 3] |= (0x80 >> (index & 0x7));
    }

    public void keepAlive() {
        long now = System.currentTimeMillis();

        if (now - messageSender.getLastSentMessageMillis() < 2000) {
            sendMessage(new Message(Message.KEEP_ALIVE, null));
        }
    }

    public long getUploaded() {
        return messageSender.getUploaded();
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setAmInterested(boolean amInterested) {
        if (!this.amInterested && amInterested) {
            sendMessage(new Message(Message.INTERESTED, null));
        } else if (this.amInterested && !amInterested) {
            sendMessage(new Message(Message.NOT_INTERESTED, null));
        }
        this.amInterested = amInterested;

    }

    public void setIsChoked(boolean isChoked) {
        if (!this.isChoked && isChoked) {
            sendMessage(new Message(Message.CHOKE, null));
        } else if (this.isChoked && !isChoked) {
            sendMessage(new Message(Message.UNCHOKE, null));
        }
        this.isChoked = isChoked;
    }

    public boolean isIsChoked() {
        return isChoked;
    }

    public boolean isAmChoked() {
        return amChoked;
    }

    public synchronized boolean isSeeder() {
        for (int i = 0; i < getPeersManager().getTorrent().getMetafile().getPieces().size(); i++) {
            if(!hasPiece(i))
                return false;
        }
        return true;
    }

    private synchronized void addRequest(Request request) {
        unfulfilledRequests.add(request);
    }

    public synchronized int getUnfulfilledRequestNumber() {
        return unfulfilledRequests.size();
    }

    private synchronized boolean requestFulfilled(int index, int begin, byte[] block) {
        for (Request r : unfulfilledRequests) {
            if (r.getIndex() == index && r.getBegin() == begin && r.getLength() == block.length) {
                unfulfilledRequests.remove(r);
                return true;
            }
        }

        return false;
    }

    private synchronized boolean requestCanceled(int index, int begin) {
        for (Request r : unfulfilledRequests) {
            if (r.getIndex() == index && r.getBegin() == begin) {
                unfulfilledRequests.remove(r);
                return true;
            }
        }

        return false;
    }

    public synchronized Request getLastUnfulfilledRequest() {
        if (unfulfilledRequests.size() == 0) {
            return null;
        }
        Request last = unfulfilledRequests.get(unfulfilledRequests.size() - 1);
        return last;
    }

    public long getLastReceivedMessageMillis() {
        return messageReceiver.getLastReceivedMessageMillis();
    }

    void setPeersManager(PeersManager peersManager) {
        this.peersManager = peersManager;
        bitfield = new byte[peersManager.getTorrent().getTorrentDisk().getBitfieldCopy().length];
    }
}





