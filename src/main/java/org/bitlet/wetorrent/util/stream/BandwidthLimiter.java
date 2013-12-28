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

package org.bitlet.wetorrent.util.stream;

public class BandwidthLimiter {

    private final static int CHUNK_SIZE = 256;
    int bytesToChunk = 0;
    long lastChunkSent = System.nanoTime();
    int maximumRate = 24;
    long nanosToWait = (1000000000l * CHUNK_SIZE) / (maximumRate * 1024l);

    public synchronized void setMaximumRate(int maximumRate) throws IllegalArgumentException {
        if (maximumRate < 1) {
            throw new IllegalArgumentException("maximumRate must be grater than 0");
        }
        this.maximumRate = maximumRate;
        nanosToWait = (1000000000l * CHUNK_SIZE) / (maximumRate * 1024l);
    }

    public synchronized void limitNextByte() {
        limitNextBytes(1);
    }

    public synchronized void limitNextBytes(int len) {

        bytesToChunk += len;

        while (bytesToChunk > CHUNK_SIZE) { /* passed a chunk */
            long now = System.nanoTime();
            long missingNanos = nanosToWait - (now - lastChunkSent);
            if (missingNanos > 0) {
                try {
                    Thread.sleep(missingNanos / 1000000, (int) missingNanos % 1000000);
                } catch (InterruptedException ex) {
                }
            }
            bytesToChunk -= CHUNK_SIZE;
            lastChunkSent = now + (missingNanos > 0 ? missingNanos : 0);
        }

    }
}
