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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.bitlet.wetorrent.bencode.Bencode;
import org.bitlet.wetorrent.bencode.DictionaryComparator;
import org.bitlet.wetorrent.util.Utils;

public class Metafile extends Bencode {

    SortedMap rootDictionary;
    private SortedMap info;
    private String announce;
    private List announceList;
    private Long creationDate;
    private String comment;
    private String createdBy;
    private long pieceLength;
    private Long length;
    private List piecesSha = new LinkedList();
    private List files = new LinkedList();
    private String name;
    private byte[] infoSha1;
    private String infoSha1Encoded;

    /** 
     * Creates a new instance of Metafile  
     */
    public Metafile(InputStream is) throws IOException, NoSuchAlgorithmException {
        super(is);

        rootDictionary = (SortedMap) getRootElement();
        info = (SortedMap) rootDictionary.get(ByteBuffer.wrap("info".getBytes()));


        pieceLength = (Long) info.get(ByteBuffer.wrap("piece length".getBytes()));

        byte[] piecesByteString = ((ByteBuffer) info.get(ByteBuffer.wrap("pieces".getBytes()))).array();
        for (int i = 0; i < piecesByteString.length; i += 20) {
            byte[] sha1 = new byte[20];
            System.arraycopy(piecesByteString, i, sha1, 0, 20);
            piecesSha.add(sha1);
        }


        name = new String(((ByteBuffer) info.get(ByteBuffer.wrap("name".getBytes()))).array());


        length = (Long) info.get(ByteBuffer.wrap("length".getBytes()));

        List files = (List) info.get(ByteBuffer.wrap("files".getBytes()));
        if (files != null) {
            length = new Long(0);
            for (Object fileObj : files) {
                Map file = (Map) fileObj;
                this.files.add(file);
                length += (Long) file.get(ByteBuffer.wrap("length".getBytes()));
            }
        }

        byte[] announceByteString = ((ByteBuffer) rootDictionary.get(ByteBuffer.wrap("announce".getBytes()))).array();
        if (announceByteString != null) {
            announce = new String(announceByteString);
        }
        announceList = (List) rootDictionary.get(ByteBuffer.wrap("announce-list".getBytes()));

        creationDate = (Long) rootDictionary.get(ByteBuffer.wrap("creation date".getBytes()));

        ByteBuffer commentByteBuffer = (ByteBuffer) rootDictionary.get(ByteBuffer.wrap("comment".getBytes()));
        if (commentByteBuffer != null) {
            comment = new String(commentByteBuffer.array());
        }
        ByteBuffer createdByByteBuffer = (ByteBuffer) rootDictionary.get(ByteBuffer.wrap("created by".getBytes()));
        if (createdByByteBuffer != null) {
            createdBy = new String(createdByByteBuffer.array());
        }
        computeInfoSha1();
    }

    public Metafile() {
        rootDictionary = new TreeMap(new DictionaryComparator());
        setRootElement(rootDictionary);
        info = new TreeMap(new DictionaryComparator());
        rootDictionary.put(ByteBuffer.wrap("info".getBytes()), getInfo());
        announceList = new LinkedList();
    }

    public SortedMap getInfo() {
        return info;
    }

    public void setInfo(SortedMap info) {
        this.info = info;
    }

    public String getAnnounce() {
        return announce;
    }

    public void setAnnounce(String announce) {
        this.announce = announce;
    }

    public List getAnnounceList() {
        return announceList;
    }

    public void setAnnounceList(List announceList) {
        this.announceList = announceList;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getPieceLength() {
        return pieceLength;
    }

    public void setPieceLength(Long pieceLength) {
        this.pieceLength = pieceLength;
    }

    public List getPieces() {
        return piecesSha;
    }

    public void setPieces(List pieces) {
        this.piecesSha = pieces;
    }

    public void computeInfoSha1() throws NoSuchAlgorithmException, IOException {

        Bencode bencode = new Bencode();
        bencode.setRootElement(info);


        MessageDigest md = MessageDigest.getInstance("SHA1");


        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bencode.print(out);


        md.update(ByteBuffer.wrap(out.toByteArray()));

        infoSha1 = md.digest();
    }

    public byte[] getInfoSha1() {
        return infoSha1;
    }

    public String getInfoSha1Encoded() {
        if (infoSha1Encoded == null) {
            infoSha1Encoded = Utils.byteArrayToURLString(getInfoSha1());
        }
        return infoSha1Encoded;

    }

    public long getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getFiles() {
        return files;
    }

    public boolean isSingleFile() {
        return (getFiles().size() == 0);
}
}
