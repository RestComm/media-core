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
import javax.media.mscontrol.MediaObject;
import javax.media.mscontrol.Parameter;
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
public class SupportedFeaturesImplTest {

    private SupportedFeaturesImpl features;
    
    public SupportedFeaturesImplTest() {
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
        parameters.add(MediaObject.MEDIAOBJECT_ID);
        
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
        
        features = new SupportedFeaturesImpl(parameters, actions, eventTypes, qualifiers, triggers, values);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSupportedParameters method, of class SupportedFeaturesImpl.
     */
    @Test
    public void testGetSupportedParameters() {
        assertEquals(1, features.getSupportedParameters().size());
    }

    /**
     * Test of getSupportedActions method, of class SupportedFeaturesImpl.
     */
    @Test
    public void testGetSupportedActions() {
        assertEquals(0, features.getSupportedActions().size());
    }

    /**
     * Test of getSupportedTriggers method, of class SupportedFeaturesImpl.
     */
    @Test
    public void testGetSupportedTriggers() {
        assertEquals(0, features.getSupportedTriggers().size());
    }

    /**
     * Test of getSupportedEventTypes method, of class SupportedFeaturesImpl.
     */
    @Test
    public void testGetSupportedEventTypes() {
        assertEquals(4, features.getSupportedEventTypes().size());
    }

    /**
     * Test of getSupportedQualifiers method, of class SupportedFeaturesImpl.
     */
    @Test
    public void testGetSupportedQualifiers() {
        assertEquals(0, features.getSupportedQualifiers().size());
    }

    /**
     * Test of getSupportedValues method, of class SupportedFeaturesImpl.
     */
    @Test
    public void testGetSupportedValues() {
        assertEquals(0, features.getSupportedValues().size());
    }

}