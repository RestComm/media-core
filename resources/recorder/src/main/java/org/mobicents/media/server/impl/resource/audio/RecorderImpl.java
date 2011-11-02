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

package org.mobicents.media.server.impl.resource.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;


import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.recorder.Recorder;
import org.mobicents.media.server.spi.recorder.RecorderListener;
import org.xiph.speex.spi.SpeexEncoding;

/**
 * Implements Audio recorder.
 * Audio recorder supports WAV, GSM and speeex media types.
 * Supported formats are: G711(a-law,u-law), Linear PCM,  Speex.
 * 
 * @author Oleg Kulikov
 * @author amit bhayani
 * @author baranowb
 */
public class RecorderImpl extends AbstractSink implements Recorder {

    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();
    
    private final static AudioFileFormat.Type GSM = new AudioFileFormat.Type("GSM0610", ".gsm");
    private final static AudioFileFormat.Type SPEEX = new AudioFileFormat.Type("SPEEX", ".spx");
    
    /** GSM Encoding constant used by Java Sound API */
    private final static Encoding GSM_ENCODING = new Encoding("GSM0610");
    
    //private final static ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private String recordDir = null;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    
    private RecorderCache recorderCache;
    
    private OutputStream outputStream;
    private AudioInputStream inputStream;
    
    private volatile boolean first = true;
    private javax.sound.sampled.AudioFormat format;
    
    private boolean isAcceptable = false;
    private Format fmt;
    
    static {
        formats.add(LINEAR);
    }
    
    /**
     * Creates new instance if Recorder.
     * 
     * @param name the name of the recorder to be created.
     */
    public RecorderImpl(String name, Scheduler scheduler) {
        super(name, scheduler,scheduler.SPLITTER_INPUT_QUEUE);
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.resource.Recorder#setRecordDir(java.lang.String) 
     */ 
    public void setRecordDir(String recordDir) {
        if (recordDir == null) {
            throw new IllegalArgumentException("RecordDir cannot be null");
        }
        this.recordDir = recordDir;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.resource.Recorder#setRecordFile(String) 
     */ 
    public void setRecordFile(String passedURI) throws IOException {
        //if output stream is still open try to close it before doing something
        if (outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
//        //FIXME: baranowb should we add here case for absolute ?
//        //construct folders first. 
//        //folders have to be created before creating bore file
//        String[] tokens = uri.split("/");
//        String fileName = tokens[tokens.length - 1];
//        String path = recordDir + "/";
//        for (int i = 0; i < tokens.length - 1; i++) {
//            path += tokens[i] + "/";
//        }
//
//        //create folders
//        java.io.File file = new java.io.File(path);
//        file.mkdirs();
     
        //prepare output stream
        //outputStream = new FileOutputStream(recordFile);
        
        
        
        //here passed "uri" MAY be URI, or something like file:/xxx/yy/z.wav, file:/g:/xxx/t.wav,file://usr/media/x.wav
        try{
        	URL targetURL = null;//Utils.getAbsoluteURL(this.recordDir, passedURI);
        	//check scheme, if its file, we should try to create dirs

        	if(targetURL.toURI().getScheme().equals("" /*Utils._FILE_SCHEME_*/))
        	{
        		File f = new File(targetURL.getFile());
        		//FIXME: check?
        		f.getParentFile().mkdirs();
        		//and open stream;
        		this.outputStream = new FileOutputStream(f);
        	}else
        	{
        		//This may throw - "protocol does not support output"
        		URLConnection urlConnection = targetURL.openConnection();
        		urlConnection.connect();
        		this.outputStream = urlConnection.getOutputStream();
        	}
        	
        }catch(URISyntaxException e )
       {
        	//damn IOException
        	e.printStackTrace();
    	   throw new IOException(e.getMessage());
       }
    }
    
    
    @Override
    public void stop() {
        if (recorderCache != null && recorderCache.available() > 0) {
            inputStream = new AudioInputStream(recorderCache, format, recorderCache.available());
            recorderCache.unblock();
            //executor.submit(new RecorderRunnable());
            (new RecorderRunnable()).run();
        }
        super.stop();
    }
    
    @Override
    public void start() {
    	super.start();
    	this.first = true;
    }

    /**
     * Closes all previously opened resources.
     */
    private void release() {
        try {
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
            if (this.inputStream != null) {
                this.inputStream.close();
                this.inputStream = null;
            }
        } catch (Exception e) {
        }
    }

   
    public Formats getNativeFormats() {
        return formats;
    }

   
    @Override
	public <T> T getInterface(Class<T> interfaceType) {
		if(interfaceType.equals(Recorder.class))
		{
			return (T) this;
		}else
		{
			return null;
		}
	}
    
    
    /**
     * Converts format encoding.
     *  
     * @param encodingName format's encoding name.
     * @return the name of format used by Java Sound.
     */
    private javax.sound.sampled.AudioFormat.Encoding getEncoding(String encodingName) {
        if (encodingName.equalsIgnoreCase("pcma")) {
            return javax.sound.sampled.AudioFormat.Encoding.ALAW;
        } else if (encodingName.equalsIgnoreCase("pcmu")) {
            return javax.sound.sampled.AudioFormat.Encoding.ULAW;
        } else if (encodingName.equalsIgnoreCase("speex")) {
            return SpeexEncoding.SPEEX;
        } else if (encodingName.equalsIgnoreCase("gsm")) {
            return GSM_ENCODING;
        } else {
            return javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
        }
    }

    /**
     * Initializes Recorder.
     * 
     * This methods is called when first packet arrives and format becomes known. 
     * 
     * @param fmt the format of the first arrived packet.
     */
    private void openRecorderLine(AudioFormat fmt) {
        float sampleRate = (float) fmt.getSampleRate();
        int sampleSizeInBits = fmt.getSampleSize();
        int channels = fmt.getChannels();
        
        boolean bigEndian = true;/*fmt.getEndian() == 1*/;
        Encoding encoding = getEncoding(fmt.getName().toString());
        

		// int frameSize = (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED) ?
		// AudioSystem.NOT_SPECIFIED
		// : ((sampleSizeInBits + 7) / 8) * channels;
        
        format = new javax.sound.sampled.AudioFormat(
                encoding, sampleRate, sampleSizeInBits, channels, 1, sampleRate, bigEndian);

        //assign file type
        if (encoding == SpeexEncoding.SPEEX) {
            fileType = SPEEX;
        } else {
            fileType = AudioFileFormat.Type.WAVE;
        }
        
        recorderCache = new RecorderCache();
    }

    @Override
    public void onMediaTransfer(Frame buffer) throws IOException {
        if (first) {
            first = false;
            openRecorderLine(null);
        }
        recorderCache.push(buffer);
    }

    public void setRecordFile(String uri, boolean append) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setMaxRecordTime(long maxRecordTime) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addListener(RecorderListener listener) throws TooManyListenersException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeListener(RecorderListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPostSpeechTimer(long value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class RecorderRunnable implements Runnable {

        public RecorderRunnable() {
        }

        public void run() {
            try {
                AudioSystem.write(inputStream, fileType, outputStream);
            } catch (Exception e) {
            } finally {
                release();
            }
        }
    }

}
