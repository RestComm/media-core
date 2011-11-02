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
package org.mobicents.media.server.ctrl.mgcp.evt.ann;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import org.mobicents.media.Component;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.ctrl.mgcp.MgcpController;
import org.mobicents.media.server.ctrl.mgcp.Request;
import org.mobicents.media.server.ctrl.mgcp.evt.EventDetector;
import org.mobicents.media.server.ctrl.mgcp.evt.GeneratorFactory;
import org.mobicents.media.server.ctrl.mgcp.evt.MgcpPackage;
import org.mobicents.media.server.ctrl.mgcp.evt.SignalGenerator;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.resource.Player;
import org.mobicents.media.server.spi.resource.TTSEngine;

/**
 * 
 * @author kulikov
 * @author baranowb
 * @author amit bhayani
 */
public class AnnSignalFactory implements GeneratorFactory {

    private String name;
    private MgcpPackage mgcpPackage;
    private int eventID;

    public String getEventName() {
        return name;
    }

    public void setEventName(String eventName) {
        this.name = eventName;
    }

    public MgcpPackage getPackage() {
        return this.mgcpPackage;
    }

    public void setPackage(MgcpPackage mgcpPackage) {
        this.mgcpPackage = mgcpPackage;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public SignalGenerator getInstance(MgcpController controller, String parms) {

        if (parms.contains("ts(")) {
            // its TTS
            return new TTSAnnSignal(parms);
        }

        MediaType type = this.mgcpPackage.getMediaType();
        if (type == null) {
            //its audio than
            type = MediaType.AUDIO;
        }
        return new AnnSignal(parms, type);
    }

    private class TTSAnnSignal extends SignalGenerator {

        private static final String _DEFAULT_VOICE_ = "kevin";
        private TTSEngine generator = null;
        private MediaType mediaType = MediaType.AUDIO;
        private String text;
        private String voice = _DEFAULT_VOICE_;

        public TTSAnnSignal(String params) {
            super(params);
            String tokens[] = params.split(" ");
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].startsWith("ts(")) {
                    int indexofts = params.indexOf("ts(");
                    text = params.substring((indexofts + 3), params.indexOf(
                            ')', (indexofts + 3)));
                } else if (tokens[i].startsWith("vc(")) {
                    int indexofts = params.indexOf("vc(");
                    voice = params.substring((indexofts + 3), params.indexOf(
                            ')', (indexofts + 3)));
                }
            }

            if (voice == null) {
                voice = _DEFAULT_VOICE_;
            }
        }

        @Override
        protected boolean doVerify(Connection connection) {

            //MediaSource source = (MediaSource) connection.getComponent(
            //		mediaType, TTSEngine.class);
            Component source = (Component) connection.getComponent(mediaType, TTSEngine.class);
            if (source != null) {
                generator = (TTSEngine) source.getInterface(TTSEngine.class);
                return true;
            } else {
                return false;
            }

        }

        @Override
        protected boolean doVerify(Endpoint endpoint) {
            //MediaSource source = endpoint.getSource(mediaType);
            Component source = (Component) endpoint.getSource(mediaType);
            if (source != null) {
                generator = source.getInterface(TTSEngine.class);
                return generator != null;
            } else {
                return false;
            }

        }

        @Override
        public void start(Request request) {

            if (generator != null) {
                generator.setVoiceName(voice);
                generator.setText(text);
                generator.start();
            }
        }

        @Override
        public void cancel() {
            if (generator != null) {
                generator.stop();
                generator = null;
            }
        }

        @Override
        public void configureDetector(EventDetector det) {
            det.setMediaType(this.mediaType);
            det.setDetectorInterface(TTSEngine.class);

        }

        public EventName getSignalRequest() {
            MgcpEvent mgcpEvent = MgcpEvent.factory(name);

            if (this.params != null) {
                mgcpEvent = mgcpEvent.withParm(this.params);
            }

            if (this.connectionIdentifier != null) {
                return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent, connectionIdentifier);
            }
            return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent);
        }
    }

    // possibly we should have seaprate for video?
    private class AnnSignal extends SignalGenerator {

        private Player generator = null;
        private MediaType mediaType = null;
        private String url;

        public AnnSignal(String params, MediaType type) {
            super(params);
            this.url = params;
            this.mediaType = type;
        }

        @Override
        protected boolean doVerify(Connection connection) {

            MediaSource source = (MediaSource) connection.getComponent(
                    mediaType, Player.class);
            if (source != null) {
                generator = (Player) source.getInterface(Player.class);
                return true;
            } else {
                return false;
            }

        }

        @Override
        protected boolean doVerify(Endpoint endpoint) {
            MediaSource source = endpoint.getSource(mediaType);
            if (source != null) {
                generator = source.getInterface(Player.class);
                return generator != null;
            } else {
                return false;
            }

        }

        @Override
        public void start(Request request) {

            if (generator != null) {
                try {
                    generator.setURL(this.url);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                generator.start();
            }
        }

        @Override
        public void cancel() {
            if (generator != null) {
                generator.stop();
                generator = null;
            }
        }

        @Override
        public void configureDetector(EventDetector det) {
            det.setMediaType(this.mediaType);
            det.setDetectorInterface(TTSEngine.class);

        }

        public EventName getSignalRequest() {
            MgcpEvent mgcpEvent = MgcpEvent.factory(name);

            if (this.params != null) {
                mgcpEvent = mgcpEvent.withParm(this.params);
            }

            if (this.connectionIdentifier != null) {
                return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent, connectionIdentifier);
            }
            return new EventName(PackageName.factory(mgcpPackage.getName()), mgcpEvent);
        }
    }
}
