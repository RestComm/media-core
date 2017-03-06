/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.resource.player.video.mpeg;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.restcomm.media.spi.format.Format;



/**
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class MpegPresentation {

    //File container
    private FileTypeBox fileTypeBox;
    
    //Movei box container
    private MovieBox movieBox;    
    private MediaDataBox mediaDataBox;
    
    //audio tack box and audio hint box
    private volatile TrackBox audioTrackBox = null;
    private volatile TrackBox audioHintTrackBox = null;
    
    //video track box and hint box
    private volatile TrackBox videoTrackBox = null;
    private volatile TrackBox videoHintTrackBox = null;
    
    private AudioTrack audioTrack;
    private VideoTrack videoTrack;
    
    private Format audioForamt = null;
    private Format videFormat = null;
    
    public MpegPresentation(URL url) throws IOException {
        File file = new File(url.getPath());
        InputStream input = url.openStream();
        DataInputStream ds = new DataInputStream(input);
        try {
            //parse file and create tracks
            parseFile(ds);
            prepareTracks();
            
            if(audioTrackBox != null && audioHintTrackBox != null){
            	audioTrack = new AudioTrack(audioTrackBox, audioHintTrackBox, file);
            }
            
            if(videoTrackBox != null && videoHintTrackBox != null){
            	videoTrack = new VideoTrack(videoTrackBox, videoHintTrackBox, file);
            }
        } finally {
            ds.close();
            input.close();
        }
    }


    public AudioTrack getAudioTrack() {
        return audioTrack;
    }
    
    public VideoTrack getVideoTrack() {
        return videoTrack;
    }
    
    public Format getAudioForamt() {
		return audioForamt;
	}


	public Format getVideFormat() {
		return videFormat;
	}


	public void prepareTracks() {
        for (TrackBox tBox : this.movieBox.getTrackBoxes()) {
            String type = tBox.getMediaBox().getHandlerReferenceBox().getHandlerType();
            if (type.equalsIgnoreCase("soun")) {
                audioTrackBox = tBox;
            } else if (type.equalsIgnoreCase("vide")) {
                videoTrackBox = tBox;
            } else if (type.equalsIgnoreCase("hint")) {
                TrackReferenceBox trackReferenceBox = tBox.getTrackReferenceBox();
                List<TrackReferenceTypeBox> trackRefTypList = trackReferenceBox.getTrackReferenceTypeBoxes();

                for (TrackReferenceTypeBox trkRefTypBox : trackRefTypList) {
                    if (trkRefTypBox.getType().equals(HintTrackReferenceTypeBox.TYPE_S)) {
                        long[] trackIds = trkRefTypBox.getTrackIDs();
                        // FIXME we are assuming here there is always 1 track that this hint track is referencing
                        // but there could be more than 1
                        long trackId = trackIds[0];
                        // FIXME audioTrackBox can be still null if hint track is placed before audioTrack
                        if (audioTrackBox.getTrackHeaderBox().getTrackID() == trackId) {
                            audioHintTrackBox = tBox;
                            
                            //Calculate the Format
                            for(Box b : audioHintTrackBox.getUserDataBox().getUserDefinedBoxes()){
                            	if(b.getType().compareTo(TrackHintInformation.TYPE_S) == 0){
                            		TrackHintInformation trackHintInfBox = (TrackHintInformation)b;
                            		RTPTrackSdpHintInformation rtpTrackSdpHintInf = trackHintInfBox.getRtpTrackSdpHintInformation();
                            		if(rtpTrackSdpHintInf != null ){
                            			String sdp = rtpTrackSdpHintInf.getSdpText();
//                            			SessionDescriptor sessionDescriptor = new SessionDescriptor(sdp, false);
//                            			MediaDescriptor md = sessionDescriptor.getMediaDescriptor(MediaType.AUDIO);
  //                          			audioForamt = md.getFormat(0);
                            		}
                            	}
                            }
                        } else if (videoTrackBox.getTrackHeaderBox().getTrackID() == trackId) {
                            videoHintTrackBox = tBox;
                        }
                    }
                }
            }
        }// for

    }

    private void parseFile(DataInputStream ds) throws IOException {
        long count = 0;
        while (ds.available() > 0) {
            long len = readU32(ds);
            byte[] type = read(ds);

            if (comparebytes(type, FileTypeBox.TYPE)) {
                fileTypeBox = new FileTypeBox(len);
                count += fileTypeBox.load(ds);

            } else if (comparebytes(type, MovieBox.TYPE)) {
                movieBox = new MovieBox(len);
                count += movieBox.load(ds);
            } else if (comparebytes(type, FreeSpaceBox.TYPE_FREE)) {
                FreeSpaceBox free = new FreeSpaceBox(len, FreeSpaceBox.TYPE_FREE_S);
                count += free.load(ds);
            } else if (comparebytes(type, FreeSpaceBox.TYPE_SKIP)) {
                FreeSpaceBox skip = new FreeSpaceBox(len, FreeSpaceBox.TYPE_SKIP_S);
                count += skip.load(ds);
            } else if (comparebytes(type, MediaDataBox.TYPE)) {
                // TODO : How should we handle multiple MediaDataBox?
                mediaDataBox = new MediaDataBox(len);
                count += mediaDataBox.load(ds);

            // if (file == null) {
            // file = new File(this.fileName);
            // }
            } else {
                // TODO : Do we care for other boxes?
                if (len - 8 > 0) {
                    ds.skipBytes((int) len - 8);
                }
            }
        }
    }

    private byte[] read(DataInputStream in) throws IOException {
        byte[] buff = new byte[4];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = in.readByte();
        }
        return buff;
    }
    
    private boolean comparebytes(byte[] arg1, byte[] arg2) {
        if (arg1.length != arg2.length) {
            return false;
        }
        for (int i = 0; i < arg1.length; i++) {
            if (arg1[i] != arg2[i]) {
                return false;
            }
        }
        return true;
    }

    private long readU32(DataInputStream in) throws IOException {
        return ((long) (in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read())) & 0xFFFFFFFFL;
    }

    public void close(){
    	if(this.audioTrack != null){
    		this.audioTrack.close();
    		this.audioTrack = null;
    	}
    	
    	if(this.videoTrack != null){
    		this.videoTrack.close();
    		this.videoTrack = null;
    	}
    }
}
