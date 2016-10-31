#!/bin/bash
## Description: Verifies if all dependencies are installed.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

verifyScreen() {
    if [ -z "$(which screen)" ]; then
        echo "GNU Screen dependency is missing."
        echo "CentOS/RHEL: yum install screen"
        echo "Debian/Ubuntu: apt-get install screen"
        echo "macOS: brew install homebrew/dupes/screen"
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
