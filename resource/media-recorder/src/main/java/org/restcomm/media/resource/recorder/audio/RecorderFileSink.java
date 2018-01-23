/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author pavel.shlupacek@spinoco.com
 */

package org.restcomm.media.resource.recorder.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sink, that assures the data are written to underlying file.
 *
 * Sink exists once the recording starts, and ceases to exists on recording deactivate.
 *
 * @author Pavel Chlupacek (pchlupacek)
 */
public class RecorderFileSink {

    private static final Logger logger = LogManager.getLogger(RecorderFileSink.class);

    private static final int HDR_SIZE = 44;
    private static final ByteBuffer EMPTY_HEADER = ByteBuffer.wrap(new byte[HDR_SIZE]).asReadOnlyBuffer();

    // target and temp file used for recording
    private final Path target;
    private final Path temp;

    // whether the recording shall be appended to target, if that target exists
    private final boolean append;

    // destination of write operation
    private final FileChannel fout;

    // when true, then this sink accepts new data false otherwise.
    private final AtomicBoolean open;

    /**
     * Creates a sink. If append is true, and target exists, then when recording is finished the resulting recording is appended
     * to current recorded file.
     * 
     * @param target Target to write file to
     * @param append Whether to append recording to `target`
     */
    public RecorderFileSink(Path target, boolean append) throws IOException {
        this.target = target;
        this.temp = target.getParent().resolve(target.getFileName() + "~");
        this.append = append;
        this.open = new AtomicBoolean(true);

        this.fout = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        this.fout.write(EMPTY_HEADER);
    }

    /**
     * Writes supplied data to the Sink (File).
     */
    public void write(ByteBuffer data) throws IOException {
        if (open.get()) {
            fout.write(data);
        }
    }

    /**
     * Commit this sink. Causes to prevent any further write operations, and commits temporary file to target. When this
     * returns, Sink is done and cannot be used again.
     */
    public void commit() throws IOException {
        // assures we perform the close operation only once.
        if (open.compareAndSet(true, false)) {
            // flush & close
            fout.force(true);
            fout.close();

            // if the current file exists, and append is true, then append samples and remove temp file
            // otherwise write header and move tmp file to target
            boolean exists = Files.exists(target);
            if (logger.isInfoEnabled()) {
                logger.info("Finishing recording ...... append: " + append + " exists: " + exists + " target:" + target);
            }
            if (append && exists) {
                appendSamples(target, temp);
                writeHeader(target);
                Files.delete(temp);
            } else {
                writeHeader(temp);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Override
    public String toString() {
        return "RecorderFileSink{" + "target=" + target + ", temp=" + temp + ", append=" + append + ", open=" + open.get()
                + '}';
    }

    /**
     * Writes samples to file following WAVE format.
     *
     *
     * @param file Recording where to write the header
     *
     * @throws IOException
     */
    private static void writeHeader(Path file) throws IOException {
        try (FileChannel fout = FileChannel.open(file, StandardOpenOption.WRITE)) {

            long size = fout.size();
            int sampleSize = (int) size - 44;

            if (logger.isInfoEnabled()) {
                logger.info("Size  " + sampleSize + " of recording file " + file);
            }

            ByteBuffer headerBuffer = ByteBuffer.allocateDirect(44);
            headerBuffer.clear();
            // RIFF
            headerBuffer.put((byte) 0x52);
            headerBuffer.put((byte) 0x49);
            headerBuffer.put((byte) 0x46);
            headerBuffer.put((byte) 0x46);

            int length = sampleSize + 36;

            // Length
            headerBuffer.put((byte) (length));
            headerBuffer.put((byte) (length >> 8));
            headerBuffer.put((byte) (length >> 16));
            headerBuffer.put((byte) (length >> 24));

            // WAVE
            headerBuffer.put((byte) 0x57);
            headerBuffer.put((byte) 0x41);
            headerBuffer.put((byte) 0x56);
            headerBuffer.put((byte) 0x45);

            // fmt
            headerBuffer.put((byte) 0x66);
            headerBuffer.put((byte) 0x6d);
            headerBuffer.put((byte) 0x74);
            headerBuffer.put((byte) 0x20);

            headerBuffer.put((byte) 0x10);
            headerBuffer.put((byte) 0x00);
            headerBuffer.put((byte) 0x00);
            headerBuffer.put((byte) 0x00);

            // format - PCM
            headerBuffer.put((byte) 0x01);
            headerBuffer.put((byte) 0x00);

            // format - MONO
            headerBuffer.put((byte) 0x01);
            headerBuffer.put((byte) 0x00);

            // sample rate:8000
            headerBuffer.put((byte) 0x40);
            headerBuffer.put((byte) 0x1F);
            headerBuffer.put((byte) 0x00);
            headerBuffer.put((byte) 0x00);

            // byte rate
            headerBuffer.put((byte) 0x80);
            headerBuffer.put((byte) 0x3E);
            headerBuffer.put((byte) 0x00);
            headerBuffer.put((byte) 0x00);

            // Block align
            headerBuffer.put((byte) 0x02);
            headerBuffer.put((byte) 0x00);

            // Bits per sample: 16
            headerBuffer.put((byte) 0x10);
            headerBuffer.put((byte) 0x00);

            // "data"
            headerBuffer.put((byte) 0x64);
            headerBuffer.put((byte) 0x61);
            headerBuffer.put((byte) 0x74);
            headerBuffer.put((byte) 0x61);

            // len
            headerBuffer.put((byte) (sampleSize));
            headerBuffer.put((byte) (sampleSize >> 8));
            headerBuffer.put((byte) (sampleSize >> 16));
            headerBuffer.put((byte) (sampleSize >> 24));

            headerBuffer.rewind();

            // lets write header
            fout.position(0);
            fout.write(headerBuffer);
        }
    }

    private static void appendSamples(Path appendTo, Path appendFrom) throws IOException {

        try (FileChannel inChannel = FileChannel.open(appendFrom, StandardOpenOption.READ);
                FileChannel outChannel = FileChannel.open(appendTo, StandardOpenOption.WRITE)) {
            long count = inChannel.size() - HDR_SIZE;
            inChannel.transferTo(HDR_SIZE, count, outChannel);
            if (logger.isInfoEnabled()) {
                logger.info("Appended " + count + " bytes from " + appendFrom + " to " + appendTo);
            }
        }
    }

}
