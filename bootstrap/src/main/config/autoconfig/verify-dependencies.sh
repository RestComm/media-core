#!/bin/bash
## Description: Verifies if all dependencies are installed.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

verifyJava() {
    if [ -z "$(which java)" ]; then
        echo "Java dependency is missing."
        echo "CentOS/RHEL: java-1.7.0-openjdk-devel.x86_64"
        echo "Debian/Ubuntu:"
        echo "    add-apt-repository ppa:openjdk-r/ppa"
        echo "    apt-get update"
        echo "    apt-get install openjdk-7-jdk"
        echo "macOS: brew cask install java7"
        exit 1
    fi
}

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

verifyJava
verifyScreen
verifyXmlstarlet
