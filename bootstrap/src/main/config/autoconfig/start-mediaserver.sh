#! /bin/bash
## Description: Starts Media Server with auto-configuration.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

MS_HOME=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

verifyDependencies() {
    source $MS_HOME/.autoconfig/verify-dependencies.sh
}

loadConfigurationParams() {
    source $MS_HOME/mediaserver.conf
}

configureMediaServer() {
    source $MS_HOME/.autoconfig/autoconfigure.sh
}

startMediaServer() {
    echo 'Starting RestComm Media Server...'
    if [ screen -ls | grep -q 'mediaserver' ]; then
        echo '... already running a GNU Screen session named "mediaserver"! Aborted.'
        exit 1
    else
        screen -dmS 'mediaserver' $MS_HOME/bin/run.sh
        echo '...RestComm Media Server started running on GNU Screen session name "mediaserver"!'
    fi
}

verifyDependencies
loadConfigurationParams
configureMediaServer
startMediaServer