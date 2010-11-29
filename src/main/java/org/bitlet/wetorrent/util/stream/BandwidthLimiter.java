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
