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
package org.restcomm.media.drivers.asr.driver.google;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.restcomm.media.drivers.asr.AsrDriverException;
import static org.restcomm.media.drivers.asr.driver.google.GoogleDriverParameter.*;
/**
 * @author Ricardo Limonta (ricardo.limonta@gmail.com)
 */
public class GoogleAsrDriver implements AsrDriver {

    private static final Logger log = Logger.getLogger(GoogleAsrDriver.class);

    private static final int DEFAULT_RESPONSE_TIMEOUT = 2000;
    private static final int DEFAULT_HERTZ = 8000;
    private static final int DEFAULT_ALTERNATIVES = 1;
    private static final boolean DEFAULT_INTERIM_RESULTS = false;
    private static final String DEFAULT_LANGUAGE = "en-US";

    // Configuration
    private static final List<String> LANGUAGES = new ArrayList<>();;

    private int responseTimeout;
    private int hertz;
    private boolean interimResults;
    
    private AtomicBoolean running = new AtomicBoolean();

    // Result Listener
    private AsrDriverEventListener listener;

    private SpeechClient speech;
    private ApiStreamObserver requestObserver;
    private ApiStreamObserver responseObserver;

    public GoogleAsrDriver() {
        // Configuration
        GoogleAsrDriver.LANGUAGES.add("af-ZA"); //Afrikaans (South Africa)
        GoogleAsrDriver.LANGUAGES.add("am-ET"); //Amharic (Ethiopia)
        GoogleAsrDriver.LANGUAGES.add("hy-AM"); //Armenian (Armenia)
        GoogleAsrDriver.LANGUAGES.add("az-AZ"); //Azerbaijani (Azerbaijan)
        GoogleAsrDriver.LANGUAGES.add("id-ID"); //Indonesian (Indonesia)
        GoogleAsrDriver.LANGUAGES.add("ms-MY"); //Malay (Malaysia)
        GoogleAsrDriver.LANGUAGES.add("bn-BD"); //Bengali (Bangladesh)
        GoogleAsrDriver.LANGUAGES.add("bn-IN"); //Bengali (India)
        GoogleAsrDriver.LANGUAGES.add("ca-ES"); //Catalan (Spain)
        GoogleAsrDriver.LANGUAGES.add("cs-CZ"); //Czech (Czech Republic)
        GoogleAsrDriver.LANGUAGES.add("da-DK"); //Danish (Denmark)
        GoogleAsrDriver.LANGUAGES.add("de-DE"); //German (Germany)
        GoogleAsrDriver.LANGUAGES.add("en-AU"); //English (Australia)
        GoogleAsrDriver.LANGUAGES.add("en-CA"); //English (Canada)
        GoogleAsrDriver.LANGUAGES.add("en-GH"); //English (Ghana)
        GoogleAsrDriver.LANGUAGES.add("en-GB"); //English (United Kingdom)
        GoogleAsrDriver.LANGUAGES.add("en-IN"); //English (India)
        GoogleAsrDriver.LANGUAGES.add("en-IE"); //English (Ireland)
        GoogleAsrDriver.LANGUAGES.add("en-KE"); //English (Kenya)
        GoogleAsrDriver.LANGUAGES.add("en-NZ"); //English (New Zealand)
        GoogleAsrDriver.LANGUAGES.add("en-NG"); //English (Nigeria)
        GoogleAsrDriver.LANGUAGES.add("en-PH"); //English (Philippines)
        GoogleAsrDriver.LANGUAGES.add("en-ZA"); //English (South Africa)
        GoogleAsrDriver.LANGUAGES.add("en-TZ"); //English (Tanzania)
        GoogleAsrDriver.LANGUAGES.add("en-US"); //English (United States)
        GoogleAsrDriver.LANGUAGES.add("es-AR"); //Spanish (Argentina)
        GoogleAsrDriver.LANGUAGES.add("es-BO"); //Spanish (Bolivia)
        GoogleAsrDriver.LANGUAGES.add("es-CL"); //Spanish (Chile)
        GoogleAsrDriver.LANGUAGES.add("es-CO"); //Spanish (Colombia)
        GoogleAsrDriver.LANGUAGES.add("es-CR"); //Spanish (Costa Rica)
        GoogleAsrDriver.LANGUAGES.add("es-EC"); //Spanish (Ecuador)
        GoogleAsrDriver.LANGUAGES.add("es-SV"); //Spanish (El Salvador)
        GoogleAsrDriver.LANGUAGES.add("es-ES"); //Spanish (Spain)
        GoogleAsrDriver.LANGUAGES.add("es-US"); //Spanish (United States)
        GoogleAsrDriver.LANGUAGES.add("es-GT"); //Spanish (Guatemala)
        GoogleAsrDriver.LANGUAGES.add("es-HN"); //Spanish (Honduras)
        GoogleAsrDriver.LANGUAGES.add("es-MX"); //Spanish (Mexico)
        GoogleAsrDriver.LANGUAGES.add("es-NI"); //Spanish (Nicaragua)
        GoogleAsrDriver.LANGUAGES.add("es-PA"); //Spanish (Panama)
        GoogleAsrDriver.LANGUAGES.add("es-PY"); //Spanish (Paraguay)
        GoogleAsrDriver.LANGUAGES.add("es-PE"); //Spanish (Peru)
        GoogleAsrDriver.LANGUAGES.add("es-PR"); //Spanish (Puerto Rico)
        GoogleAsrDriver.LANGUAGES.add("es-DO"); //Spanish (Dominican Republic)
        GoogleAsrDriver.LANGUAGES.add("es-UY"); //Spanish (Uruguay)
        GoogleAsrDriver.LANGUAGES.add("es-VE"); //Spanish (Venezuela)
        GoogleAsrDriver.LANGUAGES.add("eu-ES"); //Basque (Spain)
        GoogleAsrDriver.LANGUAGES.add("fil-PH"); //Filipino (Philippines)
        GoogleAsrDriver.LANGUAGES.add("fr-CA"); //French (Canada)
        GoogleAsrDriver.LANGUAGES.add("fr-FR"); //French (France)
        GoogleAsrDriver.LANGUAGES.add("gl-ES"); //Galician (Spain)
        GoogleAsrDriver.LANGUAGES.add("ka-GE"); //Georgian (Georgia)
        GoogleAsrDriver.LANGUAGES.add("gu-IN"); //Gujarati (India)
        GoogleAsrDriver.LANGUAGES.add("hr-HR"); //Croatian (Croatia)
        GoogleAsrDriver.LANGUAGES.add("zu-ZA"); //Zulu (South Africa)
        GoogleAsrDriver.LANGUAGES.add("is-IS"); //Icelandic (Iceland)
        GoogleAsrDriver.LANGUAGES.add("it-IT"); //Italian (Italy)
        GoogleAsrDriver.LANGUAGES.add("jv-ID"); //Javanese (Indonesia)
        GoogleAsrDriver.LANGUAGES.add("kn-IN"); //Kannada (India)
        GoogleAsrDriver.LANGUAGES.add("km-KH"); //Khmer (Cambodia)
        GoogleAsrDriver.LANGUAGES.add("lo-LA"); //Lao (Laos)
        GoogleAsrDriver.LANGUAGES.add("lv-LV"); //Latvian (Latvia)
        GoogleAsrDriver.LANGUAGES.add("lt-LT"); //Lithuanian (Lithuania)
        GoogleAsrDriver.LANGUAGES.add("hu-HU"); //Hungarian (Hungary)
        GoogleAsrDriver.LANGUAGES.add("ml-IN"); //Malayalam (India)
        GoogleAsrDriver.LANGUAGES.add("mr-IN"); //Marathi (India)
        GoogleAsrDriver.LANGUAGES.add("nl-NL"); //Dutch (Netherlands)
        GoogleAsrDriver.LANGUAGES.add("ne-NP"); //Nepali (Nepal)
        GoogleAsrDriver.LANGUAGES.add("nb-NO"); //Norwegian Bokmål (Norway)
        GoogleAsrDriver.LANGUAGES.add("pl-PL"); //Polish (Poland)
        GoogleAsrDriver.LANGUAGES.add("pt-BR"); //Portuguese (Brazil)
        GoogleAsrDriver.LANGUAGES.add("pt-PT"); //Portuguese (Portugal)
        GoogleAsrDriver.LANGUAGES.add("ro-RO"); //Romanian (Romania)
        GoogleAsrDriver.LANGUAGES.add("si-LK"); //Sinhala (Srilanka)
        GoogleAsrDriver.LANGUAGES.add("sk-SK"); //Slovak (Slovakia)
        GoogleAsrDriver.LANGUAGES.add("sl-SI"); //Slovenian (Slovenia)
        GoogleAsrDriver.LANGUAGES.add("su-ID"); //Sundanese (Indonesia)
        GoogleAsrDriver.LANGUAGES.add("sw-TZ"); //Swahili (Tanzania)
        GoogleAsrDriver.LANGUAGES.add("sw-KE"); //Swahili (Kenya)
        GoogleAsrDriver.LANGUAGES.add("fi-FI"); //Finnish (Finland)
        GoogleAsrDriver.LANGUAGES.add("sv-SE"); //Swedish (Sweden)
        GoogleAsrDriver.LANGUAGES.add("ta-IN"); //Tamil (India)
        GoogleAsrDriver.LANGUAGES.add("ta-SG"); //Tamil (Singapore)
        GoogleAsrDriver.LANGUAGES.add("ta-LK"); //Tamil (Sri Lanka)
        GoogleAsrDriver.LANGUAGES.add("ta-MY"); //Tamil (Malaysia)
        GoogleAsrDriver.LANGUAGES.add("te-IN"); //Telugu (India)
        GoogleAsrDriver.LANGUAGES.add("vi-VN"); //Vietnamese (Vietnam)
        GoogleAsrDriver.LANGUAGES.add("tr-TR"); //Turkish (Turkey)
        GoogleAsrDriver.LANGUAGES.add("ur-PK"); //Urdu (Pakistan)
        GoogleAsrDriver.LANGUAGES.add("ur-IN"); //Urdu (India)
        GoogleAsrDriver.LANGUAGES.add("el-GR"); //Greek (Greece)
        GoogleAsrDriver.LANGUAGES.add("bg-BG"); //Bulgarian (Bulgaria)
        GoogleAsrDriver.LANGUAGES.add("ru-RU"); //Russian (Russia)
        GoogleAsrDriver.LANGUAGES.add("sr-RS"); //Serbian (Serbia)
        GoogleAsrDriver.LANGUAGES.add("uk-UA"); //Ukrainian (Ukraine)
        GoogleAsrDriver.LANGUAGES.add("he-IL"); //Hebrew (Israel)
        GoogleAsrDriver.LANGUAGES.add("ar-IL"); //Arabic (Israel)
        GoogleAsrDriver.LANGUAGES.add("ar-JO"); //Arabic (Jordan)
        GoogleAsrDriver.LANGUAGES.add("ar-AE"); //Arabic (United Arab Emirates)
        GoogleAsrDriver.LANGUAGES.add("ar-BH"); //Arabic (Bahrain)
        GoogleAsrDriver.LANGUAGES.add("ar-DZ"); //Arabic (Algeria)
        GoogleAsrDriver.LANGUAGES.add("ar-SA"); //Arabic (Saudi Arabia)
        GoogleAsrDriver.LANGUAGES.add("ar-IQ"); //Arabic (Iraq)
        GoogleAsrDriver.LANGUAGES.add("ar-KW"); //Arabic (Kuwait)
        GoogleAsrDriver.LANGUAGES.add("ar-MA"); //Arabic (Morocco)
        GoogleAsrDriver.LANGUAGES.add("ar-TN"); //Arabic (Tunisia)
        GoogleAsrDriver.LANGUAGES.add("ar-OM"); //Arabic (Oman)
        GoogleAsrDriver.LANGUAGES.add("ar-PS"); //Arabic (State of Palestine)
        GoogleAsrDriver.LANGUAGES.add("ar-QA"); //Arabic (Qatar)
        GoogleAsrDriver.LANGUAGES.add("ar-LB"); //Arabic (Lebanon)
        GoogleAsrDriver.LANGUAGES.add("ar-EG"); //Arabic (Egypt)
        GoogleAsrDriver.LANGUAGES.add("fa-IR"); //Persian (Iran)
        GoogleAsrDriver.LANGUAGES.add("hi-IN"); //Hindi (India)
        GoogleAsrDriver.LANGUAGES.add("th-TH"); //Thai (Thailand)
        GoogleAsrDriver.LANGUAGES.add("ko-KR"); //Korean (South Korea)
        GoogleAsrDriver.LANGUAGES.add("cmn-Hant-TW"); //Chinese, Mandarin (Traditional, Taiwan)
        GoogleAsrDriver.LANGUAGES.add("yue-Hant-HK"); //Chinese, Cantonese (Traditional, Hong Kong)
        GoogleAsrDriver.LANGUAGES.add("ja-JP"); //Japanese (Japan)
        GoogleAsrDriver.LANGUAGES.add("cmn-Hans-HK"); //Chinese, Mandarin (Simplified, Hong Kong)
        GoogleAsrDriver.LANGUAGES.add("cmn-Hans-CN"); //Chinese, Mandarin (Simplified, China)

        this.responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
        this.hertz = DEFAULT_HERTZ;
        this.interimResults = DEFAULT_INTERIM_RESULTS;
    }

    @Override
    public void configure(Map<String, String> parameters) {
        try {
            speech = SpeechClient.create();
        } catch (IOException ex) {
            log.warn("Error on create Google SpeechClient", ex);
        }

        //Configure response timeout
        this.responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
        
        String responseTimeoutParam = null;
        if (parameters.containsKey(RESPONSE_TIMEOUT.symbol())) {
            try {
                responseTimeoutParam = parameters.get(RESPONSE_TIMEOUT.symbol());
                this.responseTimeout = Integer.parseInt(responseTimeoutParam);
            } catch (NumberFormatException e) {
                log.warn("Could not apply " + RESPONSE_TIMEOUT.symbol() + " parameter: " + responseTimeoutParam + ". Defaulting to " + DEFAULT_RESPONSE_TIMEOUT);
            }
        }

        //Configure interim results
        this.interimResults = DEFAULT_INTERIM_RESULTS;
        
        if (parameters.containsKey(INTERIM_RESULTS.symbol())) {
            this.interimResults = Boolean.parseBoolean(parameters.get(INTERIM_RESULTS.symbol()));
        }
        
        //Configure media hertz
        this.hertz = DEFAULT_HERTZ;

        String hertzParameter = null;
        if (parameters.containsKey(HERTZ.symbol())) {
            try {
                hertzParameter = parameters.get(HERTZ.symbol());
                this.hertz = Integer.parseInt(hertzParameter);
            } catch (NumberFormatException e) {
                log.warn("Could not apply " + HERTZ.symbol() + " parameter: " + hertzParameter + ". Defaulting to " + DEFAULT_HERTZ);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("responseTimeout: " + this.responseTimeout);
            log.debug("interimResults: " + this.interimResults);
            log.debug("mediaHertz: " + this.hertz);
        }
        
    }

    @Override
    public void startRecognizing(String lang, List<String> hints) {
        
        if (this.running.get()) {
            throw new IllegalStateException("Driver is already running.");
        }
        
        //Verify if language is supported
        if (!GoogleAsrDriver.LANGUAGES.contains(lang)) {
            //if not supported, stop the recognition process
            final AsrDriverException e = new AsrDriverException("Language " + lang + " not supported");
            this.listener.onError(e);
            return;
        }
        
        if (log.isDebugEnabled()) {
            log.debug("lang: " + lang);
        }
        
        //Start execution
        this.running.set(true);
        
        //Configure request with AudioEncoding LINEAR16, LanguageCode and Sample Rate Hertz
        RecognitionConfig recConfig = RecognitionConfig.newBuilder()
                .setEncoding(AudioEncoding.LINEAR16)
                .setLanguageCode(lang)
                .setSampleRateHertz(this.hertz)
                .build();

        //Create Stream Recognition Config
        StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
                .setConfig(recConfig)
                .setInterimResults(this.interimResults)
                .build();

        class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
            @Override
            public void onNext(T message) {                        
                StreamingRecognitionResult result = ((StreamingRecognizeResponse)message).getResultsList().get(0);
                if (listener != null) {
                    listener.onSpeechRecognized(result.getAlternativesList().get(0).getTranscript(), result.getIsFinal());
                    if (log.isDebugEnabled()) {
                        log.debug("Transcript: " + result.getAlternativesList().get(0).getTranscript() + ", isFinal: " + result.getIsFinal());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (listener != null) {
                    listener.onError(new AsrDriverException("Unexpected driver error.", t));
                }
            }

            @Override
            public void onCompleted() {}
        }

        //Create a bi-directional streaming channel
        BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speech.streamingRecognizeCallable();

        //Create Response Observer
        responseObserver = new ResponseApiStreamingObserver<>();

        //Create Resquest Observer
        requestObserver = callable.bidiStreamingCall(responseObserver);

        //The first request must only contain the audio configuration
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(config).build());        
    }

    @Override
    public void write(byte[] data) {
        this.write(data, 0, data.length);
    }

    @Override
    public void write(byte[] data, int offset, int len) {
        if(!this.running.get()) {
            return;
        }
        
        //Send audio data
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data, offset, len)).build());
    }

    @Override
    public void finishRecognizing() {
        try {
            speech.close();
        } catch (Exception ex) {
            log.warn("Error on close Google SpeechClient", ex);
        }
    }

    @Override
    public void setListener(AsrDriverEventListener listener) {
        this.listener = listener;
    }

    @Override
    public int getResponseTimeoutInMilliseconds() {
        return responseTimeout;
    }
}