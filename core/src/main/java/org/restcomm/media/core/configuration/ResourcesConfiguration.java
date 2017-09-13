/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.core.configuration;

/**
 * Configuration related to Resources Pools.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class ResourcesConfiguration {

    public static final int DTMF_DETECTOR_DBI = -35;
    public static final int DTMF_DETECTOR_TONE_DURATION = 100;
    public static final int DTMF_DETECTOR_TONE_INTERVAL = 400;
    public static final int DTMF_GENERATOR_TONE_VOLUME = -20;
    public static final int DTMF_GENERATOR_TONE_DURATION = 80;
    public static final int SPEECH_DETECTOR_SILENCE_LEVEL = 10;
    public static final int PLAYER_CACHE_SIZE = 0;
    public static final boolean PLAYER_CACHE_ENABLED = false;

    private int dtmfDetectorDbi;
    private int dtmfDetectorToneDuration;
    private int dtmfGeneratorToneVolume;
    private int dtmfGeneratorToneDuration;
    private int dtmfDetectorToneInterval;
    private int speechDetectorSilenceLevel;
    private int playerCacheSize;

    public ResourcesConfiguration() {
        this.dtmfDetectorDbi = DTMF_DETECTOR_DBI;
        this.dtmfDetectorToneDuration = DTMF_DETECTOR_TONE_DURATION;
        this.dtmfGeneratorToneVolume = DTMF_GENERATOR_TONE_VOLUME;
        this.dtmfGeneratorToneDuration = DTMF_GENERATOR_TONE_DURATION;
        this.dtmfDetectorToneInterval = DTMF_DETECTOR_TONE_INTERVAL;
        this.speechDetectorSilenceLevel = SPEECH_DETECTOR_SILENCE_LEVEL;
        this.playerCacheSize = PLAYER_CACHE_SIZE;
    }

    public int getDtmfDetectorDbi() {
        return dtmfDetectorDbi;
    }

    public void setDtmfDetectorDbi(int dtmfDetectorDbi) {
        if (dtmfDetectorDbi > 0 || dtmfDetectorDbi < -36) {
            throw new IllegalArgumentException("DTMF Detector Dbi must be negative and greater than -36");
        }
        this.dtmfDetectorDbi = dtmfDetectorDbi;
    }
    
    public int getDtmfDetectorToneDuration() {
        return dtmfDetectorToneDuration;
    }

    public void setDtmfDetectorToneDuration(int dtmfDetectorToneDuration) {
        if (dtmfDetectorToneDuration < 0) {
            throw new IllegalArgumentException("DTMF Detector Tone Duration cannot be negative");
        }
        this.dtmfDetectorToneDuration = dtmfDetectorToneDuration;
    }
    
    public int getDtmfDetectorToneInterval() {
            return dtmfDetectorToneInterval;
    }

    public void setDtmfDetectorToneInterval(int dtmfDetectorToneInterval) {
        if (dtmfDetectorToneInterval < 0) {
            throw new IllegalArgumentException("DTMF Detector Tone Interval cannot be negative");
        }
        this.dtmfDetectorToneInterval = dtmfDetectorToneInterval;
    }

    public int getDtmfGeneratorToneVolume() {
        return dtmfGeneratorToneVolume;
    }

    public void setDtmfGeneratorToneVolume(int dtmfGeneratorToneVolume) {
        if (dtmfGeneratorToneVolume > 0) {
            throw new IllegalArgumentException("DTMF Generator Tone Volume cannot be positive");
        }
        this.dtmfGeneratorToneVolume = dtmfGeneratorToneVolume;
    }

    public int getDtmfGeneratorToneDuration() {
        return dtmfGeneratorToneDuration;
    }

    public void setDtmfGeneratorToneDuration(int dtmfGeneratorToneDuration) {
        if (dtmfGeneratorToneDuration < 0) {
            throw new IllegalArgumentException("DTMF Generator Tone Duration cannot be negative");
        }
        this.dtmfGeneratorToneDuration = dtmfGeneratorToneDuration;
    }
    
    public int getSpeechDetectorSilenceLevel() {
        return speechDetectorSilenceLevel;
    }
    
    public void setSpeechDetectorSilenceLevel(int speechDetectorSilenceLevel) {
        if (speechDetectorSilenceLevel < 0) {
            throw new IllegalArgumentException("Speech detector silence level cannot be negative");
        }
        this.speechDetectorSilenceLevel = speechDetectorSilenceLevel;
    }

    public void setPlayerCache(boolean playerCacheEnabled, int playerCacheSize) {
        if (!playerCacheEnabled) {
            this.playerCacheSize = 0;
            return;
        }
        if (playerCacheSize <= 0) {
            throw new IllegalArgumentException("Player cache size cannot be negative");
        }
        this.playerCacheSize = playerCacheSize;
    }

    public int getPlayerCacheSize() {
        return playerCacheSize;
    }

    public boolean getPlayerCacheEnabled() {
        return this.playerCacheSize != 0;
    }

}
