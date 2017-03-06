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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.test;

import java.util.Properties;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.spi.DriverManager;

import org.restcomm.javax.media.mscontrol.spi.DriverImpl;

/**
 *
 * @author kulikov
 */
public class Tester {
    private DriverImpl driver;
    private MsControlFactory factory;
    
    public void init() throws MsControlException {
        Properties cfg = new Properties();
        cfg.put(cfg, cfg);
        
        driver = (DriverImpl) DriverManager.getDriver("org.mobicents.Driver_1.0");
        factory = driver.getFactory(null);
    }
    
    public void run() throws MsControlException, InterruptedException {
        MediaSession session = factory.createMediaSession();
        
        final NetworkConnection c1 = session.createNetworkConnection(NetworkConnection.BASIC);
        final NetworkConnection c2 = session.createNetworkConnection(NetworkConnection.BASIC);
        
        MediaGroup g1 = session.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
        MediaGroup g2 = session.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR);
        
        c1.getSdpPortManager().addListener(new MediaEventListener<SdpPortManagerEvent>() {
            public void onEvent(SdpPortManagerEvent event) {
                if (event.getEventType() == SdpPortManagerEvent.OFFER_GENERATED) {
                    try {
                         c2.getSdpPortManager().processSdpOffer(event.getMediaServerSdp());
                    } catch (MsControlException e){
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("BAD!BAD!BAD!");
                }
            }
        });

        c2.getSdpPortManager().addListener(new MediaEventListener<SdpPortManagerEvent>() {
            public void onEvent(SdpPortManagerEvent event) {
                if (event.getEventType() == SdpPortManagerEvent.ANSWER_PROCESSED) {
                    try {
                        c1.getSdpPortManager().processSdpAnswer(event.getMediaServerSdp());
                    } catch (MsControlException e){
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("BAD!BAD!BAD!");
                }
            }
        });
        
        c1.join(Direction.DUPLEX, g1);
        c1.getSdpPortManager().generateSdpOffer();
                
        c2.join(Direction.DUPLEX, g2);
        
        //Thread.sleep(100);
        
        session.release();
    }
    
    public void shutdown() {
        driver.shutdown();
    }
    
    public static void main(String args[]) throws Exception  {
        Tester tester = new Tester();
        tester.init();
        
        for (int i = 0; i < 200; i++) {
            System.out.println("Starting test " + i);
            tester.run();
            System.out.println("Completed test " + i);
            //Thread.sleep(500);
        }
        
        tester.shutdown();
    }
}
