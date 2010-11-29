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

package org.bitlet.wetorrent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Level;
import org.bitlet.wetorrent.bencode.Bencode;

public class Tracker {

    String announce;
    private Long lastRequestTime;
    private Long interval;
    private Long minInterval;
    private String trackerId;
    private String key;
    private Long complete;
    private Long incomplete;

    public Tracker(String announce) {
        this.announce = announce;
    }

    public Map trackerRequest(Torrent torrent, String event) throws MalformedURLException, IOException {

        String trackerUrlString = announce;
        trackerUrlString += "?info_hash=" + torrent.getMetafile().getInfoSha1Encoded();
        trackerUrlString += "&peer_id=" + torrent.getPeerIdEncoded();
        trackerUrlString += "&port=" + torrent.getIncomingPeerListener().getPort();
        trackerUrlString += "&uploaded=" + torrent.getPeersManager().getUploaded();
        trackerUrlString += "&downloaded=" + torrent.getPeersManager().getDownloaded();
        trackerUrlString += "&left=" + (torrent.getMetafile().getLength() - torrent.getTorrentDisk().getCompleted());
        trackerUrlString += "&compact=1";
        if (event != null) {
            trackerUrlString += "&event=" + event;
        }
        if (trackerId != null) {
            trackerUrlString += "&tracker_id=" + trackerId;
        }
        if (key != null){
            trackerUrlString += "&key=" + key;
        }
        URL trackerUrl = new URL(trackerUrlString);

        if (Torrent.verbose) {
            torrent.addEvent(new Event(this, "Querying tracker: " + trackerUrl.toString(), Level.FINE));
        }
        HttpURLConnection conn = (HttpURLConnection) trackerUrl.openConnection();
        conn.setRequestProperty("User-Agent", torrent.agent);

        Bencode trackerResponse = new Bencode(new BufferedInputStream(conn.getInputStream()));
        lastRequestTime = System.currentTimeMillis();

        Map responseDictionary = (Map) trackerResponse.getRootElement();

        byte[] failureReasonByteString = null;
        ByteBuffer failureReasonByteBuffer = (ByteBuffer) responseDictionary.get(ByteBuffer.wrap("failure reason".getBytes()));
        if (failureReasonByteBuffer != null) {
            failureReasonByteString = failureReasonByteBuffer.array();
        }
        if (failureReasonByteString != null) {
            String failureReason = new String(failureReasonByteString);
            if (Torrent.verbose) {
                torrent.addEvent(new Event(this, "Tracker response failure: " + failureReason, Level.SEVERE));
            }
            return null;
        }

        try {
            byte[] warningMessageByteString = null;
            ByteBuffer warningMessageByteBuffer = (ByteBuffer) responseDictionary.get(ByteBuffer.wrap("warning message".getBytes()));
            if (warningMessageByteBuffer != null) {
                warningMessageByteString = warningMessageByteBuffer.array();
            }
            if (warningMessageByteString != null) {
                String warningMessage = new String(warningMessageByteString);
                if (Torrent.verbose) {
                    torrent.addEvent(new Event(this, "Tracker response warning " + warningMessage, Level.WARNING));
                }
            }
        } catch (Exception e) {
        }

        byte[] trackerIdByteString = null;
        ByteBuffer trackerIdByteBuffer = (ByteBuffer) responseDictionary.get(ByteBuffer.wrap("tracker id".getBytes()));
        if (trackerIdByteBuffer != null) {
            trackerIdByteString = trackerIdByteBuffer.array();
        }
        if (trackerIdByteString != null) {
            trackerId = new String(trackerIdByteString);
            if (Torrent.verbose) {
                torrent.addEvent(new Event(this, "Tracker id: " + trackerId, Level.FINE));
            }
        }

        interval = (Long) responseDictionary.get(ByteBuffer.wrap("interval".getBytes()));
        minInterval = (Long) responseDictionary.get(ByteBuffer.wrap("min interval".getBytes()));
        complete = (Long) responseDictionary.get(ByteBuffer.wrap("complete".getBytes()));
        incomplete = (Long) responseDictionary.get(ByteBuffer.wrap("incomplete".getBytes()));

        return responseDictionary;
    }

    public Long getInterval() {
        return interval;
    }

    public Long getMinInterval() {
        return minInterval;
    }

    public Long getLastRequestTime() {
        return lastRequestTime;
    }

    public void setKey(String key) {
        this.key = key;
    }
    
}
