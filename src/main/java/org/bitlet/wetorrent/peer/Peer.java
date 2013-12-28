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
import org.bitlet.wetorrent.peer.message.Message;

public interface Peer {

    public void sendMessage(Message message);

    public void setAmInterested(boolean b);

    public void setIsChoked(boolean b);

    byte[] getPeerId();

    long getDownloaded();

    InetAddress getIp();

    public int getPort();

    long getLastReceivedMessageMillis();

    int getUnfulfilledRequestNumber();

    long getUploaded();

    boolean hasPiece(int index);

    void interrupt();

    boolean isAmChoked();

    boolean isSeeder();
}
