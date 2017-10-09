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

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import org.restcomm.media.drivers.asr.AsrDriverException;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * @author Ricardo Limonta
 */
@Ignore
public class WatsonAsrDriverTest {

    private static final Logger log = Logger.getLogger(WatsonAsrDriverTest.class);

    private WatsonAsrDriver driver;

    @After
    public void after() {
        if (this.driver != null) {
            this.driver.setListener(null);
            this.driver.finishRecognizing();
            this.driver = null;
        }
    }

    @Test
    public void transcriptionTest() throws Exception {
        // given
        Map<String, String> params = new HashMap<>();
        params.put("hertz", "8000");
        params.put("interimResults", "true");
        params.put("responseTimeout", "1000");
        params.put("apiUsername", "[watson api username]");
        params.put("apiPassword", "[watson api password]");

        final int duration = 6000;
        final URL url = WatsonAsrDriver.class.getResource("/audio/audio_demo.wav");
        final Path path = Paths.get(url.toURI());
        final byte[] data = Files.readAllBytes(path);

        final AsrDriverEventListener listener = spy(new AsrDriverEventListener() {

            @Override
            public void onSpeechRecognized(String text, boolean isFinal) {
                log.info("Transcription: " + text + ", isFinal: " + isFinal);
            }

            @Override
            public void onError(AsrDriverException error) {
                log.error("Unexpected Error", error);
                fail();
            }

        });

        this.driver = new WatsonAsrDriver();
        this.driver.configure(params);
        this.driver.setListener(listener);

        // when
        this.driver.startRecognizing("pt-BR", Collections.<String>emptyList());
        this.driver.write(data);

        // then
        verify(listener, timeout(duration)).onSpeechRecognized(any(String.class), any(Boolean.class));
        verify(listener, never()).onError(any(AsrDriverException.class));
    }

}