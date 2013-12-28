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