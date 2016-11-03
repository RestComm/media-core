#! /bin/bash
## Description: Starts Media Server with auto-configuration.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

export MS_HOME=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

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
    if tmux ls | grep -q 'mediaserver'; then
        echo '... already running a session named "mediaserver"! Aborted.'
        exit 1
    else
        tmux new -s mediaserver -d bin/run.sh
        echo '...RestComm Media Server started running on session named "mediaserver"!'
    fi
}

verifyDependencies
loadConfigurationParams
configureMediaServer
startMediaServer