#! /bin/bash
## Description: Stops Media Server process.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

MS_HOME=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

stopMediaServer() {
    echo 'Stopping RestComm Media Server...'
    if tmux ls | grep -q 'mediaserver'; then
        tmux kill-session -t mediaserver
        echo '...stopped RestComm Media Server instance running on session "mediaserver".'
        exit 0
    else
        echo '...could not find any active "mediaserver" session!'
        exit 1
    fi
}

stopMediaServer