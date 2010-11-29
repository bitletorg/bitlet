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

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.bitlet.wetorrent.util.Utils;

public class Piece {

    private byte[] sha1;
    private int length;
    private List<FilePieceMapper> files = new ArrayList<FilePieceMapper>();

    private class PieceBlock {

        public PieceBlock(Integer begin, Integer lenght) {
            this.begin = begin;
            this.length = lenght;
        }
        public Integer begin;
        public Integer length;

        public String toString() {
            return "b:" + begin + " l:" + length;
        }
    }
    private Set<PieceBlock> blocks = new TreeSet<PieceBlock>(new Comparator<PieceBlock>() {

        public int compare(Piece.PieceBlock o1, Piece.PieceBlock o2) {
            int beginDiff = o1.begin - o2.begin;
            if (beginDiff != 0) {
                return beginDiff;
            }
            return o1.length - o2.length;
        }
    });

    /**
     * Creates a new instance of Piece
     */
    public Piece(byte[] sha1) {
        this.sha1 = sha1;
    }

    public void addFilePointer(FilePieceMapper filePointer) {
        files.add(filePointer);
    }

    public int getCompleted() {
        Integer completed = 0;

        for (PieceBlock block : blocks) {
            completed += block.length;
        }
        return completed;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public boolean isCompleted() {
        return getCompleted() == length;
    }

    public void write(int begin, byte[] block) throws IOException {

        int filePieceIndex = findFilePieceIndex(begin);
        FilePieceMapper filePiece = files.get(filePieceIndex);

        int writtenBytes = 0;
        while (writtenBytes < block.length) {
            RandomAccessFile raf = filePiece.getFile();
            Long seek = filePiece.getFileOffset() + ((begin + writtenBytes) - filePiece.getPieceOffset());
            raf.seek(seek);

            int byteToWrite = block.length - writtenBytes;
            Long byteAvaiableInThisFile = raf.length() - seek;

            Long byteAvaiableToWrite = byteToWrite < byteAvaiableInThisFile ? byteToWrite : byteAvaiableInThisFile;
            raf.write(block, writtenBytes, byteAvaiableToWrite.intValue());
            writtenBytes += byteAvaiableToWrite.intValue();

            if (byteAvaiableToWrite.equals(byteAvaiableInThisFile) && writtenBytes < block.length) {
                filePiece = files.get(++filePieceIndex);
            }
        }


        addPieceBlock(begin, block.length);

        if (isCompleted()) {
            if (!checkSha1()) {
                blocks.clear();
                throw new IOException("sha check failed");
            }
        }
    }

    public byte[] read(int begin, int length) throws IOException {

        if (!isAvaiable(begin, length)) {
            throw new EOFException("Data not available " + "begin: " + begin + " length: " + length);
        }
        int filePieceIndex = findFilePieceIndex(begin);
        FilePieceMapper filePiece = files.get(filePieceIndex);
        byte[] block = new byte[length];

        int readBytes = 0;
        while (readBytes < length) {
            RandomAccessFile raf = filePiece.getFile();
            Long seek = filePiece.getFileOffset() + ((begin + readBytes) - filePiece.getPieceOffset());
            raf.seek(seek);

            int byteToRead = length - readBytes;
            Long byteAvaiableInThisFile = raf.length() - seek;

            Long byteAvaiableToRead = byteToRead < byteAvaiableInThisFile ? byteToRead : byteAvaiableInThisFile;
            raf.readFully(block, readBytes, byteAvaiableToRead.intValue());
            readBytes += byteAvaiableToRead.intValue();

            if (byteAvaiableToRead.equals(byteAvaiableInThisFile) && readBytes < length) {
                filePiece = files.get(++filePieceIndex);
            }
        }

        return block;
    }

    /* TODO: Optimize with bisection
     */
    private int findFilePieceIndex(int begin) {
        int i = 0;
        for (i = 0; i < files.size() - 1; i++) {
            if (files.get(i).getPieceOffset() <= begin && files.get(i + 1).getPieceOffset() > begin) {
                return i;
            }
        }

        return i;
    }

    public void addPieceBlock(int begin, int length) {

        PieceBlock newPieceBlock = new PieceBlock(begin, length);

        /*The pieceBlocks are sorted using begin as key*/
        blocks.add(newPieceBlock);

        Iterator<PieceBlock> iterator = blocks.iterator();
        PieceBlock prev = iterator.next();

        Collection<PieceBlock> blocksToBeRemoved = new LinkedList<PieceBlock>();

        while (iterator.hasNext()) {
            PieceBlock p = iterator.next();
            if (prev.begin + prev.length >= p.begin) {

                p.length = Math.max(p.length + (p.begin - prev.begin), prev.length);
                p.begin = prev.begin;
                blocksToBeRemoved.add(prev);
            }
            prev = p;
        }

        for (PieceBlock pb : blocksToBeRemoved) {
            blocks.remove(pb);
        }
    }

    public boolean isAvaiable(int begin, int length) {
        for (PieceBlock block : blocks) {
            if (begin >= block.begin && length <= block.length) {
                return true;
            } else if (begin + length < block.begin) {
                return false;
            }
        }

        return false;
    }

    public int getFirstMissingByte() {

        if (blocks.size() > 0) {
            PieceBlock firstBlock = blocks.iterator().next();
            if (firstBlock.begin == 0) {
                return firstBlock.length;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public boolean checkSha1() throws IOException {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }

        byte[] pieceBuffer = read(0, length);
        byte[] sha1Digest = md.digest(pieceBuffer);
        return Utils.bytesCompare(sha1, sha1Digest);
    }

    public void clear() {
        blocks.clear();
    }

    public int available(int begin) {
        for (PieceBlock pb : blocks) {
            if (pb.begin <= begin && (pb.begin + pb.length) > begin) {
                return (pb.begin + pb.length) - begin;
            }
        }

        return 0;
    }
}
