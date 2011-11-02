/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.javax.media.mscontrol.networkconnection;


import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.networkconnection.CodecPolicy;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.networkconnection.SdpPortManagerException;
import javax.sdp.Attribute;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.mobicents.fsm.UnknownTransitionException;
import org.mobicents.javax.media.mscontrol.networkconnection.fsm.ConnectionTransition;
import org.mobicents.mscontrol.sdp.AVProfile;
import org.mobicents.mscontrol.sdp.SessionDescriptor;

/**
 * 
 * @author kulikov
 */
public class SdpPortManagerImpl implements SdpPortManager {

    private SdpFactory sdpFactory;
    protected NetworkConnectionImpl connection;
    
    private SessionDescription localSdp;
    protected SessionDescription remoteSdp;
    
    private CopyOnWriteArrayList<MediaEventListener> listeners = new CopyOnWriteArrayList();
    private CodecPolicy codecPolicy = new CodecPolicy();

    private SdpProcessor sdpProcessor;
    private final static String[] bogus = new String[] {"fmtp", "AS", "sendrecv", "audio", "IP4"};
    
    public SdpPortManagerImpl(NetworkConnectionImpl connection) {
        this.connection = connection;
        this.sdpProcessor = new SdpProcessor();
        sdpFactory = SdpFactory.getInstance();
    }
    
    public synchronized void generateSdpOffer() throws SdpPortManagerException {
               
        if (connection.getEndpoint().concreteNameExpectedSoon()) {
            try {
                connection.getEndpoint().await();
            } catch (InterruptedException e) {
                throw new SdpPortManagerException(e.getMessage());
            }
        }

        connection.debug("Generating SDP offer");
        try {
            connection.fsm.signal(ConnectionTransition.OPEN);
        } catch (UnknownTransitionException e) {
            throw new SdpPortManagerException(e.getMessage());
        }

    }

    public void processSdpOffer(byte[] sdp) throws SdpPortManagerException {
        if (connection.getEndpoint().concreteNameExpectedSoon()) {
            try {
                connection.getEndpoint().await();
            } catch (InterruptedException e) {
                throw new SdpPortManagerException(e.getMessage());
            }
        }

        if (!connection.getEndpoint().hasConcreteName()) {
            connection.getEndpoint().expectingConcreteName();
        }
        connection.debug("Processing SDP offer");
        
        try {
            this.remoteSdp = sdpFactory.createSessionDescription(new String(sdp));
        } catch (Exception e) {
            e.printStackTrace();
            throw new SdpPortManagerException(e.getMessage());
        }
        
        for (String f : codecPolicy.getExcludedCodecs()) {
            try {
                sdpProcessor.exclude(f, remoteSdp);
            } catch (Exception e) {
                throw new SdpPortManagerException(e.getMessage());
            }
        }
        
        boolean minOffer = false;
        try {
            minOffer = sdpProcessor.checkForMinimalOffer(remoteSdp);
        } catch (Exception e) {
            throw new javax.media.mscontrol.networkconnection.SdpException("Minimal offer");
        }
        
        if (!minOffer) {
            throw new javax.media.mscontrol.networkconnection.SdpException("Minimal offer");
        }
        
        
        try {
            connection.fsm.signal(ConnectionTransition.OPEN);
        } catch (UnknownTransitionException e) {
            throw new SdpPortManagerException("state= "
                    + connection.fsm.getState().getName() + " transition: "
                    + e.getMessage());
        }
    }

    public void processSdpAnswer(byte[] sdp) throws SdpPortManagerException {
        connection.debug("Processing SDP answer");
        
        try {
            this.remoteSdp = sdpFactory.createSessionDescription(new String(sdp));
        } catch (Exception e) {
            throw new SdpPortManagerException(e.getMessage());
        }
        
        try {
            connection.fsm.signal(ConnectionTransition.MODIFY);
        } catch (UnknownTransitionException e) {
            throw new SdpPortManagerException(e.getMessage());
        }
    }

    public void rejectSdpOffer() throws SdpPortManagerException {
        try {
            connection.fsm.signal(ConnectionTransition.CLOSE);
        } catch (UnknownTransitionException e) {
            throw new SdpPortManagerException(e.getMessage());
        }
    }

    public byte[] getMediaServerSessionDescription()
            throws SdpPortManagerException {
        return localSdp != null ? localSdp.toString().getBytes() : null;
    }

    public byte[] getUserAgentSessionDescription()
            throws SdpPortManagerException {
        return remoteSdp != null ? remoteSdp.toString().getBytes() : null;
    }

    public void setCodecPolicy(CodecPolicy codecPolicy)
            throws SdpPortManagerException {
        // checking codec policy: required should not be excluded
        String[] required = codecPolicy.getRequiredCodecs();
        for (String codec : required) {
            if (lookup(codec, codecPolicy.getExcludedCodecs())) {
                throw new SdpPortManagerException("Codec " + codec
                        + " is excluded");
            }
        }

        // checking codec policy: excluded should not be in capabilities or
        // preferences
        String[] excluded = codecPolicy.getExcludedCodecs();
        for (String codec : excluded) {
            if (lookup(codec, codecPolicy.getCodecCapabilities())) {
                throw new SdpPortManagerException("Codec " + codec
                        + " is excluded but in capabilities");
            }
            if (lookup(codec, codecPolicy.getCodecPreferences())) {
                throw new SdpPortManagerException("Codec " + codec
                        + " is excluded but in preferences");
            }
        }

        // assign specified policy
        this.codecPolicy = codecPolicy;
    }

    private void print(String label, String[] list) {
        System.out.println(label);
        for (String s : list) {
            System.out.println(s);
        }
    }

    public CodecPolicy getCodecPolicy() {
        return codecPolicy;
    }

    public NetworkConnection getContainer() {
        return connection;
    }

    public void addListener(MediaEventListener<SdpPortManagerEvent> listener) {
        listeners.add(listener);
    }

    public void removeListener(MediaEventListener<SdpPortManagerEvent> listener) {
        listeners.remove(listener);
    }

    public MediaSession getMediaSession() {
        return connection.getMediaSession();
    }

    private boolean isSDPGenerated(SdpPortManagerEvent evt) {
        return evt.getEventType() == SdpPortManagerEvent.ANSWER_GENERATED
                || evt.getEventType() == SdpPortManagerEvent.OFFER_GENERATED;
    }

    private boolean applyCodecPolicy(SessionDescription sdp) throws SdpException {
        for (String f : codecPolicy.getRequiredCodecs()) {
            if (!sdpProcessor.containsFormat(f, sdp)) {
                if (f.equalsIgnoreCase("AMR")) {
                    Vector<Attribute> attributes = ((MediaDescription)sdp.getMediaDescriptions(false).get(0)).getAttributes(false);
                    attributes.add(sdpFactory.createAttribute("rtpmap", "99 AMR/8000"));

                    connection.error = null;
                    connection.errorMsg = "";
                    return true;
                } else {
                    connection.error = SdpPortManagerEvent.SDP_NOT_ACCEPTABLE;
                    connection.errorMsg = "";
                    return false;
                }
            }
        }

        for (String f : codecPolicy.getCodecCapabilities()) {
            if (!isBogus(f) && !sdpProcessor.containsFormat(f, localSdp)) {
                connection.error = SdpPortManagerEvent.SDP_NOT_ACCEPTABLE;
                connection.errorMsg = "";
                return false;
            }
        }

        for (String m : codecPolicy.getMediaTypeCapabilities()) {
            if (!sdpProcessor.containsMedia(m, localSdp)) {
                connection.error = SdpPortManagerEvent.SDP_NOT_ACCEPTABLE;
                connection.errorMsg = "";
                return false;
            }
        }

        return true;
    }

    protected void fireEvent(SdpPortManagerEvent evt)  {
        boolean res = false;
        try {
            res = !applyCodecPolicy(this.localSdp);
        } catch (Exception e) {
        }
        
        // apply codec policy for generated sdp
        if (this.isSDPGenerated(evt) && codecPolicy != null && res) {
            fireEvent(new SdpPortManagerEventImpl(this, SdpPortManagerEvent.NETWORK_STREAM_FAILURE));
            try {
                connection.fsm.signal(ConnectionTransition.CLOSE);
            } catch (Exception e) {
            }
            return;
        }

        // deliver event to listeners
        new Thread(new EventProcessor(evt)).start();
//        for (MediaEventListener listener : listeners) {
//            connection.debug(String.format("Event=%s", evt.toString()));
//            listener.onEvent(evt);
//        }
    }

    private boolean lookup(String s, String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (s.equals(list[i])) {
                return true;
            }
        }
        return false;
    }

    protected void setLocalDescriptor(String sdp) throws SdpException {
        this.localSdp = sdpFactory.createSessionDescription(sdp);
    }

    protected String getLocalDescriptor() {
        return localSdp == null ? "" : localSdp.toString();
    }

    private boolean isBogus(String f) {
        for (int i = 0; i < bogus.length; i++) {
            if (f.equalsIgnoreCase(bogus[i])) {
                return true;
            }
        }
        return false;
    }
    
    private class Signal implements Runnable {

        private String t;

        public Signal(String t) {
            this.t = t;
            new Thread(this).start();
        }

        public void run() {
            try {
                connection.fsm.signal(t);
            } catch (Exception e) {
            }
        }
    }

    private String convertSDP(String sdpString) throws SdpException {
        SdpFactory factory = javax.sdp.SdpFactory.getInstance();
        SessionDescription sd = factory.createSessionDescription(sdpString);
        Vector<?> mediaDescriptions = sd.getMediaDescriptions(false);
        for (Object object : mediaDescriptions) {

            MediaDescription md = (MediaDescription) object;
            Vector<?> attributes = md.getAttributes(false);
            for (Object object2 : attributes) {
                Attribute attribute = (Attribute) object2;
                if (attribute.getName().compareToIgnoreCase("rtpmap") == 0) {
                    attribute.setValue(attribute.getValue().toUpperCase());
                }
            }
        }
        return sd.toString();
    }
    
    
    private class EventProcessor implements Runnable {
        private SdpPortManagerEvent evt;
        
        
        public EventProcessor(SdpPortManagerEvent evt) {
            this.evt = evt;
        }
        
        public void run() {
            // deliver event to listeners
            for (MediaEventListener listener : listeners) {
                connection.debug(String.format("Event=%s", evt.toString()));
                listener.onEvent(evt);
            }
        }
    }
}
