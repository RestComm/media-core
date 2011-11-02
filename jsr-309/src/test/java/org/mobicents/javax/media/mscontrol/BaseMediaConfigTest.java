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

package org.mobicents.javax.media.mscontrol;

import java.util.HashSet;
import java.util.Set;
import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaConfig;
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.resource.Trigger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class BaseMediaConfigTest {

    private MediaConfigImpl cfg;
    
    public BaseMediaConfigTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        Set<Parameter> parameters = new HashSet();
        
        //specify event types
        Set<EventType> eventTypes = new HashSet();
        eventTypes.add(SdpPortManagerEvent.OFFER_GENERATED);
        eventTypes.add(SdpPortManagerEvent.ANSWER_GENERATED);
        eventTypes.add(SdpPortManagerEvent.ANSWER_PROCESSED);
        eventTypes.add(SdpPortManagerEvent.NETWORK_STREAM_FAILURE);
        
        //Define actions
        Set<Action> actions = new HashSet();
        
        //Define qualifiers
        Set<Qualifier> qualifiers = new HashSet();
        
        //Define triggers
        Set<Trigger> triggers = new HashSet();
        
        //Define values
        Set<Value> values = new HashSet();
        SupportedFeaturesImpl features = new SupportedFeaturesImpl(parameters, actions, eventTypes, qualifiers, triggers, values);
        
        cfg = new MediaConfigImpl(features, new ParametersImpl());
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of hasStream method, of class MediaConfigImpl.
     */
    @Test
    public void testHasStream() {
        assertEquals(0, cfg.getSupportedFeatures().getSupportedParameters().size());
    }

    /**
     * Test of createCustomizedClone method, of class MediaConfigImpl.
     */
    @Test
    public void testCreateCustomizedClone() throws Exception {
        Parameters params = new ParametersImpl();
        params.put(MediaObject.MEDIAOBJECT_ID, "/mobicents/media/packetrelay/$");
        
        MediaConfig c = cfg.createCustomizedClone(params);
        
        assertEquals(1, c.getSupportedFeatures().getSupportedParameters().size());
    }


}