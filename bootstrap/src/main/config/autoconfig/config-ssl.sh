#!/bin/bash
## Description: Configures the SSL aspects of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configSsl() {
    local config_file=$MS_HOME/bin/run.sh
    local ssl_enabled=${1-false}
    local ssl_keystore=$2
    local ssl_password=$3

    # Enable or disable SSL
    if [[ "$ssl_enabled" == "true" ]]; then
        sed -e "/# SSL Configuration/ {
            n; s|^#\(.*\)|\1|
        }" $config_file > $config_file.bak
        mv $config_file.bak $config_file
    else
        sed -e "/# SSL Configuration/ {
            N; s|JAVA_OPTS=|#JAVA_OPTS=|
        }" $config_file > $config_file.bak
        mv $config_file.bak $config_file
    fi

    # Configure keystore and password
    sed -e "s|trustStore=[a-zA-Z0-9_\/\.\-]*|trustStore=$ssl_keystore|g" \
        -e "s|trustStorePassword=[a-zA-Z0-9_]*|trustStorePassword=$ssl_password|g" \
        $config_file > $config_file.bak
    mv $config_file.bak $config_file

}

patchPath() {
    if [[ "$1" = /* ]]; then
        echo "$1"
    else
        echo $MS_HOME/$1
    fi
}

echo "Configuring SSL [Enabled=$SSL_ENABLED, KeyStore=$SSL_KEYSTORE, Password=$SSL_PASSWORD]"
configSsl $SSL_ENABLED $SSL_KEYSTORE $SSL_PASSWORD
