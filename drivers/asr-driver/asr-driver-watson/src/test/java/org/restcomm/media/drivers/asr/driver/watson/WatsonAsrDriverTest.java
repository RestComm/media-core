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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.restcomm.media.drivers.asr.driver.watson;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;

/**
 * @author Ricardo Limonta
 */
public class WatsonAsrDriverTest {
    
    private WatsonAsrDriver watsonAsrDriver;
    private AsrDriverEventListener eventListener;
    
    @Before
    public void setUp() {
        //create a driver instance
        watsonAsrDriver = new WatsonAsrDriver();
        
        //create a driver listener instance
        eventListener = new WatsonAsrDriverEventListener();
        
        //setup parameters
        Map<String, String> params = new HashMap<>();
        params.put("WATSON_API_USERNAME", "321965b7-b4fc-4a94-bdbe-cbf77a2bf029");
        params.put("WATSON_API_PASSWORD", "VcedeadGFMzq");
        
        //call configure method
        watsonAsrDriver.configure(params);
        
        //set event listener
        watsonAsrDriver.setListener(eventListener);
        
        //call start recognition
        watsonAsrDriver.startRecognizing("pt-BR", null);   
    }
    
    @Test
    public void transcriptionTest() throws Exception {
        // Demo audio file
        URL url = WatsonAsrDriver.class.getResource("/audio/audio_demo.wav");
	Path path = Paths.get(url.toURI());
	byte[] data = Files.readAllBytes(path);
        
        //process transcription
        watsonAsrDriver.write(data);

        //waits for 10 seconds to simulate an active call
        Thread.sleep(10000); 
    }
    
    @After
    public void tearDown() {
        watsonAsrDriver.finishRecognizing();
    }
}