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

package org.bitlet.wetorrent.pieceChooser;

import java.util.Random;
import org.bitlet.wetorrent.Torrent;
import org.bitlet.wetorrent.peer.Peer;

public class RouletteWheelPieceChooser extends PieceChooser {

    protected Integer choosePiece(Peer peer, int[] piecesFrequencies) {

        int[] probabilities = piecesFrequencies.clone();
        int maxFrequency = 0;
        Torrent torrent = getTorrent();
        for (int i = 0; i < torrent.getMetafile().getPieces().size(); i++) {
            if (peer.hasPiece(i) && !torrent.getTorrentDisk().isCompleted(i) && !isCompletingPiece(i)) {
                if (maxFrequency < probabilities[i]) {
                    maxFrequency = probabilities[i];
                }
            } else {
                probabilities[i] = Integer.MAX_VALUE;
            }
        }

        int total = 0;
        for (int i = 0; i < torrent.getMetafile().getPieces().size(); i++) {
            if (probabilities[i] == Integer.MAX_VALUE) {
                probabilities[i] = 0;
            } else {
                probabilities[i] = 1 + maxFrequency - probabilities[i];
            }
            total += probabilities[i];
            probabilities[i] = total;
        }

        if (total == 0) {
            return null;
        }
        long random = new Random(System.currentTimeMillis()).nextInt(total);
        int i;
        if (random < probabilities[0]) {
            return 0;
        }
        for (i = 1; i < probabilities.length; i++) {
            if (probabilities[i - 1] <= random && probabilities[i] > random) {
                break;
            }
        }

        return i;
    }
}