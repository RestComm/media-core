#! /bin/bash
## Description: Stops Media Server process.
## Author     : Henrique Rosa (henrique.rosa@telestax.com)

MS_HOME=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

stopMediaServer() {
    echo 'Stopping RestComm Media Server...'
    if [ screen -ls | grep -q 'mediaserver' ]; then
        screen -S 'mediaserver' -p 0 -X 'quit'
        echo '...stopped RestComm Media Server instance running on GNU Screen session "mediaserver".'
        exit 0
    else
        echo '...could not find any active Media Server session on GNU Screen!'
        exit 1
    fi
}

stopMediaServer