#!/bin/bash
## Description: Configures the DTLS aspects of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configDtls() {
    readonly min_version=${1-1.0}
    readonly max_version=${2-1.2}
    readonly cipher_suite=${3-TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA}
    readonly certificate=${4-../conf/dtls/x509-server-ecdsa.pem}
    readonly key=${5-../conf/dtls/x509-server-key-ecdsa.pem}
    readonly algorithm=${6-ecdsa}

    echo "Configuring DTLS [Min Version=$min_version, Max Version=$max_version, Certificate=$certificate, Key=$key, Algorithm=$algorithm, Cipher Suite=$cipher_suite]"

    xmlstarlet ed --inplace --pf \
        -u "/mediaserver/dtls/minVersion" -v "$min_version" \
        -u "/mediaserver/dtls/maxVersion" -v "$max_version" \
        -u "/mediaserver/dtls/cipherSuites" -v "$cipher_suite" \
        -u "/mediaserver/dtls/certificate/@path" -v "$certificate" \
        -u "/mediaserver/dtls/certificate/@key" -v "$key" \
        -u "/mediaserver/dtls/certificate/@algorithm" -v "$algorithm" \
        $MS_HOME/conf/mediaserver.xml
}

configDtls $DTLS_MIN_VERSION $DTLS_MAX_VERSION $DTLS_CIPHER_SUITE $DTLS_CERTIFICATE $DTLS_KEY $DTLS_ALGORITHM
