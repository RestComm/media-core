@echo off
set CLASSPATH=.;tck.jar;deps/jain-mgcp-ri-1.0.jar;deps/mgcp-impl-2.0.0.GA.jar;deps/mscontrol.jar;c:/projects/mobicents/trunk/servers/media/jsr-309/core/target/mobicents-jsr309-impl-2.0.0.BETA5-SNAPSHOT.jar;deps/junit.jar;deps/jain-sip-ri-1.2.146.jar;deps/log4j-1.2.14.jar;

rem java -classpath %CLASSPATH% org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.mandatory.functional.networkconnection.CodecPolicyTest

rem JOIN
rem java -classpath %CLASSPATH% org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.mandatory.functional.join.InitJoinTest

rem PLAYER
rem java -classpath %CLASSPATH% org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.mandatory.functional.mediagroup.PlayerTest
java -classpath %CLASSPATH% org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.mandatory.functional.mediagroup.PromptAndCollectTest

rem SIGNAL DETECTOR
rem java -classpath %CLASSPATH% org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.mandatory.functional.mediagroup.signals.SignalDetectorTest
rem java -classpath %CLASSPATH% org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.mandatory.functional.mediagroup.signals.DTMFBufferingTest



rem java -classpath %CLASSPATH% org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.mandatory.functional.AllTests