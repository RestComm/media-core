/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol;

import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.concurrent.locks.ReentrantLock;
import javax.media.mscontrol.Configuration;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaConfigException;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.video.VideoLayout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.javax.media.mscontrol.mediagroup.MediaGroupImpl;
import org.restcomm.javax.media.mscontrol.mixer.MediaMixerImpl;
import org.restcomm.javax.media.mscontrol.networkconnection.NetworkConnectionImpl;
import org.restcomm.javax.media.mscontrol.spi.DriverImpl;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MsControlFactoryImpl implements MsControlFactory {

    private DriverImpl driver;
    private List<Integer> list = new ArrayList<Integer>();
    private List<MediaSession> sessions = new ArrayList<MediaSession>();
    private XMLParser parser = new XMLParser();

    private final Logger logger = LogManager.getLogger(MsControlFactoryImpl.class);
    
    private ReentrantLock lock = new ReentrantLock();
    
    // protected static Map<Configuration, MediaConfigImpl>
    // configVsMediaConfigMap = new HashMap<Configuration, MediaConfigImpl>();
    public MsControlFactoryImpl(DriverImpl driver) {
        this.driver = driver;
    }

    public DriverImpl getDriver() {
        return driver;
    }
    
    public int getUniqueHandler() {
        return driver.getNextTxID();
    }
    
    public MediaSession createMediaSession() {
        lock.lock();
        try {
            //create new media session and put into the list of active sessions
            MediaSession session = new MediaSessionImpl(this);
            sessions.add(session);
        
            //return result
            return session;
        } catch (MsControlException e){
            return null;
        } finally {
            lock.unlock();
        }
    }

    protected void removeSession(MediaSessionImpl session) {
        lock.lock();
        try {
            sessions.remove(session);
        } finally {
            lock.unlock();
        }
    }
    
    public Parameters createParameters() {
        return new ParametersImpl();
    }

    public VideoLayout createVideoLayout(String mimeType, Reader xmlDef)
            throws MediaConfigException {
        // TODO Auto-generated method stub
        return null;
    }

    public VideoLayout getPresetLayout(String type) throws MediaConfigException {
        return null;
    }

    public VideoLayout[] getPresetLayouts(int numberOfLiveRegions)
            throws MediaConfigException {
        // TODO Auto-generated method stub
        return null;
    }

    public MediaConfig getMediaConfig(Configuration<?> cfg) throws MediaConfigException {        
        if (cfg.equals(NetworkConnection.BASIC)) {
            return NetworkConnectionImpl.BASE_CONFIG;
        } else if (cfg.equals(NetworkConnection.DTMF_CONVERSION)) {
            return null;
        } else if (cfg.equals(NetworkConnection.ECHO_CANCEL)) {
            return null;
        } else if (cfg.equals(MediaGroup.PLAYER)) {
            return MediaGroupImpl.PLAYER_CONFIG;
        } else if (cfg.equals(MediaGroup.SIGNALDETECTOR)) {
            return MediaGroupImpl.SIGNAL_DETECTOR_CONFIG;
        } else if (cfg.equals(MediaGroup.PLAYER_SIGNALDETECTOR)) {
            return MediaGroupImpl.PLAYER_SIGNAL_DETECTOR_CONFIG;
        } else if (cfg.equals(MediaGroupImpl.RECORDER_CONFIG)) {
            return MediaGroupImpl.RECORDER_CONFIG;
        } else if (cfg.equals(MediaGroupImpl.PLAYER_RECORDER_SIGNALDETECTOR)) {
            return MediaGroupImpl.PLAYER_RECORDER_SIGNAL_DETECTOR_CONFIG;
        } else if (cfg.equals(MediaMixer.AUDIO)) {
            return MediaMixerImpl.AUDIO_CONFIG;
        } 

        throw new MediaConfigException("Unsupported Configuration " + cfg);
    }

    public MediaConfig getMediaConfig(Reader paramReader)  throws MediaConfigException {
/*        int c;
        MediaConfigImpl config = null;

        try {
            while ((c = paramReader.read()) != -1) {
                list.add(c);
            }

            byte[] b = new byte[list.size()];
            int count = 0;
            for (int i : list) {
                b[count] = (byte) i;
                count++;
            }

            list.clear();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
            config = parser.parse(null, inputStream);
        } catch (IOException e) {
            logger.error(e);
            throw new MediaConfigException(e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            logger.error(e);
            throw new MediaConfigException(e.getMessage(), e);
        } catch (SAXException e) {
            logger.error(e);
            throw new MediaConfigException(e.getMessage(), e);
        }
*/        
        return null;
    }

    public MediaObject getMediaObject(URI paramURI) {
        return null;
    }

    protected int getActiveSessions() {
        return this.sessions.size();
    }

    public Properties getProperties() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
