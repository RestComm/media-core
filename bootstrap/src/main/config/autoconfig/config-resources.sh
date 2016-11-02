#!/bin/bash
## Description: Configures the Resource Pooling of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configResourcePooling() {
    readonly expected_load=${1-50}
    readonly local_connection_count=$(($expected_load * 2))
    readonly remote_connection_count=$expected_load
    readonly player_count=$expected_load
    readonly recorder_count=$expected_load
    readonly dtmf_detector_count=$expected_load
    readonly dtmf_generator_count=$expected_load

    echo "Configuring Resource Pools [Local Connections=$local_connection_count, Remote Connections=$remote_connection_count, Players=$player_count, Recorders=$recorder_count, DTMF Detectors=$dtmf_detector_count, DTMF Generators=$dtmf_generator_count]"

    xmlstarlet ed --inplace --pf \
        -u "/mediaserver/resources/localConnection/@poolSize" -v "$local_connection_count" \
        -u "/mediaserver/resources/remoteConnection/@poolSize" -v "$remote_connection_count" \
        -u "/mediaserver/resources/player/@poolSize" -v "$player_count" \
        -u "/mediaserver/resources/recorder/@poolSize" -v "$recorder_count" \
        -u "/mediaserver/resources/dtmfDetector/@poolSize" -v "$dtmf_detector_count" \
        -u "/mediaserver/resources/dtmfGenerator/@poolSize" -v "$dtmf_generator_count" \
        $MS_HOME/conf/mediaserver.xml
}

configAudioCache() {
    readonly cache_enabled=${1-false}
    readonly cache_size=${2-100}

    echo "Configuring Audio Cache [Enabled=$cache_enabled, Size=$cache_size]"

    xmlstarlet ed --inplace --pf \
        -u "/mediaserver/resources/player/cache/cacheSize" -v "$cache_size" \
        -u "/mediaserver/resources/player/cache/cacheEnabled" -v "$cache_enabled" \
        $MS_HOME/conf/mediaserver.xml
}

configDtmfDetector() {
    readonly dbi=${1--30}

    echo "Configuring DTMF Detector [Dbi=$dbi]"

    xmlstarlet ed --inplace --pf -u "/mediaserver/resources/dtmfDetector/@dbi" -v "$dbi" $MS_HOME/conf/mediaserver.xml
}

configResourcePooling $EXPECTED_LOAD
configAudioCache $AUDIO_CACHE_ENABLED $AUDIO_CACHE_SIZE
configDtmfDetector $DTMF_DETECTOR_DBI