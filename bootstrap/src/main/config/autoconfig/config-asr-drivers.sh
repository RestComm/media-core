#!/bin/bash
## Description: Configures the ASR Drivers.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configureDriver() {
    local driverName=$1
    local driverClass=$2
    xmlstarlet ed --inplace -s "/mediaserver/subsystems/subsystem[@name='asr']" -t elem -n driver \
        -i "/mediaserver/subsystems/subsystem[@name='asr']/driver[last()]" -t attr -n "name" -v $driverName \
        -i "/mediaserver/subsystems/subsystem[@name='asr']/driver[last()]" -t attr -n "class" -v $driverClass \
        $MEDIA_XML
}

configureProperties() {
    local propertyName=$1
    local propertyValue=$2
    xmlstarlet ed --inplace -s "/mediaserver/subsystems/subsystem[@name='asr']/driver[last()]" -t elem -n parameter -v $propertyValue \
        -i "/mediaserver/subsystems/subsystem[@name='asr']/driver[last()]/parameter[last()]" -t attr -n "name" -v $propertyName \
        $MEDIA_XML
}

configure() {
    result=`xmlstarlet sel -t -v "count(/mediaserver/subsystems)" $MEDIA_XML`
    if [ "$result" -eq 0 ]; then
        xmlstarlet ed --inplace -s /mediaserver -t elem  -n subsystems $MEDIA_XML
    fi

    xmlstarlet ed --inplace -d "/mediaserver/subsystems/subsystem[@name='asr']" \
        -s "/mediaserver/subsystems" -t elem  -n subsystem \
        -i "/mediaserver/subsystems/subsystem[last()]" -t attr -n "name" -v "asr" \
        $MEDIA_XML

    for envVariable in $asrVariables
    do
        apiProviderName=`echo ${envVariable} | awk -F"ASR_DRIVER_" '{print $2}' | awk -F"_CLASS" '{print $1}'`
        driverXmlName=`eval echo '$'ASR_DRIVER_${apiProviderName}`
        if [ -z $driverXmlName ]; then
            echo "No XML name for ApiProvider: $apiProviderName has been found, skipping configuration..."
            continue
        fi
        driverXmlClass=`echo ${envVariable} | cut -d = -f2`
        configureDriver $driverXmlName $driverXmlClass
        for property in $(set -o posix; set | grep "^ASR_DRIVER_${apiProviderName}_PROPERTY_.*")
        do
            propVar=`echo $property | cut -d = -f1`
            propValue=`echo $property | cut -d = -f2`
            propSuffix=`echo $propVar | awk -F"PROPERTY_" '{print $2}'`
            if [[ $propSuffix == *"_VALUE"* ]]
            then
                propertyName=`echo $propSuffix | awk -F"_VALUE" '{print $1}'`
                propertyXmlName=$(set -o posix; set |grep "^ASR_DRIVER_${apiProviderName}_PROPERTY_${propertyName}=" |cut -d = -f2)
                if [ -z $propertyXmlName ];then
                    echo "No XML name for property: $propertyName has been found, skipping"
                    continue
                fi
                configureProperties $propertyXmlName $propValue
            fi
        done
    done
}


if [ -z "$MS_HOME" ]; then
    echo "Error during asr driver configuration: MS_HOME env variable not set"
    exit 1
fi

MEDIA_XML=${MS_HOME}/conf/mediaserver.xml

if [ ! -f $MEDIA_XML ]; then
    echo "Error during asr driver configuration: mediaserver.xml file not found!"
    exit 1
fi

asrVariables=`set -o posix; set | grep "^ASR_DRIVER_.*_CLASS="`

if [ ! -z "$asrVariables" ]; then
    configure
fi
