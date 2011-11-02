#!/bin/sh

# In case we need it.
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;
        
    Linux)
        linux=true
        ;;
esac

DIRNAME=`dirname $0`
PROGNAME=`basename $0`

# Force IPv4 on Linux systems since IPv6 doesn't work correctly with jdk5 and lower
if [ "$linux" = "true" ]; then
   JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup TEST_CORE
if [ "x$TEST_CORE" = "x" ]; then
    # get the full path (without any relative bits)
    TEST_CORE=`cd $DIRNAME/..; pwd`
fi
export TEST_CORE



#Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
	JAVA="$JAVA_HOME/bin/java"
    else
	JAVA="java"
    fi
fi




RUN_CLASSPATH="$TEST_CORE/target/classes:$TEST_CORE/target/appframework.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/concurrent.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/fmj.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/jain-mgcp-ri.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/java-getopt.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/jboss-common.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/jcommon.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/jfreechart.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/jspeex.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/log4j.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/mgcp-stack.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/mobicents-media-server-impl.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/mobicents-media-server-spi.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/mp3spi.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/opencsv.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/swing-layout.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/swing-worker.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/tritonus_share.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/vorbisspi.jar"
RUN_CLASSPATH="$RUN_CLASSPATH:$TEST_CORE/target/jain-sip-ri.jar"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    TEST_CORE=`cygpath --path --windows "$TEST_CORE"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    RUN_CLASSPATH=`cygpath --path --windows "$RUN_CLASSPATH"`
fi




#
warn() {
    echo "${PROGNAME}: $*"
}

#
# Helper to puke.
#
die() {
    warn $*
    exit 1
}


usage(){

echo "Usage:"
echo "bin.sh  [OPTIONS] --testtype TestType"
echo "Where options can be:"
echo "--localaddr               : local address, default is 127.0.0.1"
echo "--remoteaddr              : remote address, default is 127.0.0.1"
echo "--localpport              : local port, default is 2428"
echo "--remoteport              : remote port, default is 2427"
echo "--concurrentcalls         : concurrent calls, default is -1, which means unbound"
echo "--maxcalls                : max calls, default is -1, which means unbound"
echo "--datadir                 : data dump directory, default is ./datadump. IN case of offline runs, it must point to valid test, like: /datadump/1231463412"
echo "--audiofile               : audio file url, if requried, default is file:/...../target/audio/ulaw_13s.wav"
echo "--audiocodec              : audio codec to be used if requried, default is \'0 pcmu/8000\', value should be specifiedd in \'\'"
echo "--testtype                : test type, currently there is only one available: AnnTest"
echo "--maxfail                 : specifies how many calls may fail until testtool will stop sending requests to server, default is -1, which means unbound"
echo "--graph                   : It makes test case produce graphic files and present some txt output. Graphic files depend on testcase, some do not support graphic. If there is no value, test case picks one call arbitrary, otherwise it performs this operation for specific call. Argumetn must match call sequence."
echo "--callduration            : specifies how long test runs(in milliseconds), default is 2500 "
echo "--cps                     : specifies calls per second, default is 1 "
echo "--usage                   : print cli usage directly from CLI runner"
echo "example options part: --localaddress=127.0.0.1 --localport=2499 --concurentcalls=12 --audiocodec=\'8 pcma/8000\' --testtype=AnnTest"

}


executeTest(){
      # Display our environment
      echo "========================================================================="
      echo ""
      echo "  JBoss Bootstrap Environment"
      echo ""
      echo "  RUN_HOME : $TEST_CORE"
      echo ""
      echo "  JAVA     : $JAVA"
      echo ""
      echo "  JAVA_OPTS: $JAVA_OPTS"
      echo ""
      echo "  CLASSPATH: $RUN_CLASSPATH"
      echo ""
      echo "  MAVEN    : mvn"
      echo ""
      echo "  OPTS     : $*"
      echo ""
      echo "========================================================================="
      echo ""


      echo "Preparing test tool jar..."
      #mvn -f $TEST_CORE/pom.xml clean install
      mvn -f $TEST_CORE/pom.xml install

      echo ""
      echo "========================================================================="
      echo ""
      echo "JAR and dependencies are ready, executing test session"
      echo ""
      echo "========================================================================="





      "$JAVA" $JAVA_OPTS \
	-classpath "$RUN_CLASSPATH" \
	org.mobicents.media.server.testsuite.cli.CLIRunner $*





}






if [ "$#" = "0" ]; then
   usage
   die
fi

executeTest $*
