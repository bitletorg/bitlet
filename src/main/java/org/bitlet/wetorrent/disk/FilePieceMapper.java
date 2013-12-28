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

package org.bitlet.wetorrent.disk;

import java.io.RandomAccessFile;

public class FilePieceMapper {

    private Long fileOffset;
    private Long pieceOffset;
    private RandomAccessFile file;

    public FilePieceMapper(RandomAccessFile file, Long fileOffset, Long pieceOffset) {
        this.file = file;
        this.fileOffset = fileOffset;
        this.pieceOffset = pieceOffset;
    }

    public Long getFileOffset() {
        return fileOffset;
    }

    public Long getPieceOffset() {
        return pieceOffset;
    }

    public RandomAccessFile getFile() {
        return file;
    }
}
