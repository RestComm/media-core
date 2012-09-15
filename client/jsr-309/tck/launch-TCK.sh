JSR309TCK_HOME=$(dirname $0)
# Keep "." first, so that properties are loaded from there
# Change "mscontrolRI.jar" for your implementation jar
CP=".:mscontrol.jar:mobicents-jsr309-impl-2.2.0.CR2.jar:tck.jar"
for jar in deps/*.jar
do
  CP=$CP:$jar
done
# echo $CP

if [ "$1" != "" ]
then
  ARG="$1"
else
  ARG="AllTests"
fi
$JAVA_HOME/bin/java -cp $CP org.junit.runner.JUnitCore com.hp.opencall.jmsc.test.$ARG
