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

package org.bitlet.wetorrent.disk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bitlet.wetorrent.Metafile;

public class PlainFileSystemTorrentDisk implements TorrentDisk {

    Metafile metafile;
    private List<Piece> pieces = new ArrayList<Piece>();
    List<RandomAccessFile> files = new LinkedList<RandomAccessFile>();
    File saveDirectory;

    public PlainFileSystemTorrentDisk(Metafile metafile, File saveDirectory) {
        this.metafile = metafile;
        this.saveDirectory = saveDirectory;
    }

    public synchronized void resume() throws IOException {
        resume(null);
    }

    public synchronized void resume(ResumeListener rl) throws IOException {

        long completed = 0;
        long resumed = 0;
        for (Piece p : pieces) {
            /*pretend that the piece is already downloaded and check the hash*/
            if (rl != null) {
                rl.percent(completed, resumed);
            }
            p.addPieceBlock(0, p.getLength());
            if (!p.checkSha1()) {
                p.clear();
            } else {
                resumed += p.getLength();
            }
            completed += p.getLength();
        }
        if (rl != null) {
            rl.percent(completed, resumed);
        }
    }

    public synchronized boolean init() throws IOException {

        boolean resume = false;

        /* create pieces */
        Long pieceNumber = 0l;
        for (Object elem : metafile.getPieces()) {
            byte[] sha1 = (byte[]) elem;
            Piece piece = new Piece(sha1);
            if (pieceNumber < metafile.getPieces().size() - 1 && (metafile.getLength() % metafile.getPieceLength()) > 0) {
                piece.setLength(metafile.getPieceLength().intValue());
            } else {
                piece.setLength(new Long(metafile.getLength() % metafile.getPieceLength()).intValue());
            }
            pieces.add(piece);

            pieceNumber++;
        }

        saveDirectory.mkdirs();
        /*create files*/
        Long fileLength = 0l;
        if (metafile.isSingleFile()) {
            File persistentFile = new File(saveDirectory, metafile.getName());
            if (persistentFile.exists()) {
                resume = true;
            }
            RandomAccessFile raf = new RandomAccessFile(persistentFile, "rw");
            raf.setLength(metafile.getLength());
            files.add(raf);
        } else {
            if (!saveDirectory.getName().equals(metafile.getName())) {
                saveDirectory = new File(saveDirectory, metafile.getName());
                saveDirectory.mkdir();
            }

            for (Object elem : metafile.getFiles()) {
                Map file = (Map) elem;
                List path = (List) file.get(ByteBuffer.wrap("path".getBytes()));
                String pathName = "";

                Iterator pathIterator = path.iterator();
                while (pathIterator.hasNext()) {
                    byte[] pathElem = ((ByteBuffer) pathIterator.next()).array();
                    pathName += "/" + new String(pathElem);
                    if (pathIterator.hasNext()) {
                        new File(saveDirectory, pathName).mkdir();
                    }
                }
                /*
                for (Object pathElem : path)
                pathName += "/" + new String((byte[])pathElem );*/


                Long length = (Long) file.get(ByteBuffer.wrap("length".getBytes()));
                File persistentFile = new File(saveDirectory.getAbsolutePath() + pathName);
                if (persistentFile.exists()) {
                    resume = true;
                }
                RandomAccessFile raf = new RandomAccessFile(persistentFile, "rw");
                raf.setLength(length);
                fileLength += length;
                files.add(raf);
            }
        }



        /*Associate pieces to files*/
        Iterator<RandomAccessFile> fileIterator = files.iterator();
        Iterator<Piece> pieceIterator = pieces.iterator();

        Long fileOffset = 0l;
        Long pieceOffset = 0l;

        Piece piece = pieceIterator.next();
        RandomAccessFile file = fileIterator.next();

        while (piece != null && file != null) {

            piece.addFilePointer(new FilePieceMapper(file, fileOffset, pieceOffset));

            Long pieceFreeBytes = piece.getLength() - pieceOffset;
            Long fileMissingBytes = file.length() - fileOffset;

            if (pieceFreeBytes < fileMissingBytes) {
                fileOffset += pieceFreeBytes;
                if (pieceIterator.hasNext()) {
                    piece = pieceIterator.next();
                } else {
                    piece = null;
                }
                pieceOffset = 0l;
            } else if (pieceFreeBytes > fileMissingBytes) {
                pieceOffset += fileMissingBytes;
                if (fileIterator.hasNext()) {
                    file = fileIterator.next();
                } else {
                    file = null;
                }
                fileOffset = 0l;
            } else /* == */ {
                fileOffset = 0l;
                pieceOffset = 0l;
                if (fileIterator.hasNext()) {
                    file = fileIterator.next();
                } else {
                    file = null;
                }
                if (pieceIterator.hasNext()) {
                    piece = pieceIterator.next();
                } else {
                    piece = null;
                }
            }
        }

        return resume;
    }

    public synchronized byte[] getBitfieldCopy() {
        byte[] bitField = new byte[(pieces.size() >> 3) + ((pieces.size() & 0x7) != 0 ? 1 : 0)];
        for (int i = 0; i < pieces.size(); i++) {
            bitField[i >> 3] |= (pieces.get(i).isCompleted() ? 0x80 : 0) >> (i & 0x7);
        }
        return bitField;
    }

    public synchronized void write(int index, int begin, byte[] block) throws IOException {
        Piece piece = pieces.get(index);
        if (!piece.isCompleted()) {
            piece.write(begin, block);
        }
    }

    public synchronized byte[] read(int index, int begin, int length) throws IOException {
        Piece piece = pieces.get(index);
        return piece.read(begin, length);
    }

    public synchronized Long getCompleted() {
        Long completed = 0l;
        for (Piece p : pieces) {
            completed += p.getCompleted();
        }
        return completed;
    }

    public synchronized boolean isCompleted(int index) {
        return pieces.get(index).isCompleted();
    }

    public synchronized int getDownloaded(int index) {
        return pieces.get(index).getCompleted();
    }

    public synchronized boolean isAvailable(int index, int begin, int length) {
        return pieces.get(index).isAvaiable(begin, length);
    }

    public synchronized int getLength(int index) {
        return pieces.get(index).getLength();
    }

    public synchronized int getFirstMissingByte(int index) {
        return pieces.get(index).getFirstMissingByte();
    }

    public synchronized void close() {
        for (RandomAccessFile file : files) {
            try {
                file.close();
            } catch (IOException ex) {
            }
        }
    }

    public synchronized long available(int index, int begin) {
        return available(index, begin, Long.MAX_VALUE);
    }

    public synchronized long available(int index, int begin, long length) {

        int pieceLength = pieces.get(0).getLength();

        boolean goOn = true;
        long avaiable = 0;
        while (goOn) {
            Piece p = pieces.get(index);
            int pieceAvaiable = p.available(begin);

            avaiable += pieceAvaiable;
            index++;
            begin = 0;

            if (pieceAvaiable != p.getLength() - begin ||
                    pieceAvaiable >= length) {
                goOn = false;
            }
        }

        return avaiable;
    }
}
