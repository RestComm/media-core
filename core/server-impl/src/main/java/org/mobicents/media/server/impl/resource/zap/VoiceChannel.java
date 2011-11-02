/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.impl.resource.zap;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceGroup;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 *
 * @author kulikov
 */
public class VoiceChannel extends BaseComponent implements ResourceGroup {

    private RandomAccessFile channel;
    
    private VoiceSource source;
    private VoiceSink sink;
    
    private Format[] formats = new Format[]{AVProfile.PCMA};
    
    private static ArrayList<MediaType> mediaTypes = new ArrayList();
    static {
        mediaTypes.add(MediaType.AUDIO);
    }
    
    public VoiceChannel(String path, int cic) {
        super(path);
        
        source = new VoiceSource(path);
        sink = new VoiceSink(path);
    }
    
    public Collection<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public MediaSink getSink(MediaType media) {
        return sink;
    }

    public MediaSource getSource(MediaType media) {
        return source;
    }

    private class VoiceSource extends AbstractSource {
        
        public VoiceSource(String name) {
            super(name);
        }

        @Override
        public void evolve(Buffer buffer, long timestamp) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Format[] getFormats() {
            return formats;
        }
        
    }
    
    private class VoiceSink extends AbstractSink {
        
        public VoiceSink(String name) {
            super(name);
        }

        @Override
        public void onMediaTransfer(Buffer buffer) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Format[] getFormats() {
            return formats;
        }
        
    }

}
