#!/bin/bash
## Description: Verifies if all dependencies are installed.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

verifyScreen() {
    if [ -z "$(which tmux)" ]; then
        echo "tmux dependency is missing."
        echo "CentOS/RHEL: yum install tmux"
        echo "Debian/Ubuntu: apt-get install tmux"
        echo "macOS: brew install tmux"
        exit 1
    fi
}

verifyXmlstarlet() {
    if [ -z "$(which xmlstarlet)" ]; then
        echo "XML Starlet dependency is missing."
        echo "CentOS/RHEL: yum install xmlstarlet"
        echo "Debian/Ubuntu: apt-get install xmlstarlet"
        echo "macOS: brew install xmlstarlet"
        exit 1
    fi
}

verifyScreen
verifyXmlstarlet
