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

package org.bitlet.wetorrent;

import static org.bitlet.wetorrent.util.Utils.toByteBuffer;

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
        ByteBuffer failureReasonByteBuffer = (ByteBuffer) responseDictionary.get(toByteBuffer("failure reason"));
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
            ByteBuffer warningMessageByteBuffer = (ByteBuffer) responseDictionary.get(toByteBuffer("warning message"));
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
        ByteBuffer trackerIdByteBuffer = (ByteBuffer) responseDictionary.get(toByteBuffer("tracker id"));
        if (trackerIdByteBuffer != null) {
            trackerIdByteString = trackerIdByteBuffer.array();
        }
        if (trackerIdByteString != null) {
            trackerId = new String(trackerIdByteString);
            if (Torrent.verbose) {
                torrent.addEvent(new Event(this, "Tracker id: " + trackerId, Level.FINE));
            }
        }

        interval = (Long) responseDictionary.get(toByteBuffer("interval"));
        minInterval = (Long) responseDictionary.get(toByteBuffer("min interval"));
        complete = (Long) responseDictionary.get(toByteBuffer("complete"));
        incomplete = (Long) responseDictionary.get(toByteBuffer("incomplete"));

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
