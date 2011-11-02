This is testool for MMS which allows to generate traffic and make
MMS do its hard work and send/receive RTP
Itention behind testtoll is to provide means of easy testing deployment of mms,
create constant load condition, etc.
To get knowledge on how to use it visit: XXX add urls XXX, or follow this file.


1. Supported run modes
 a) CLI
 b) GUI

2. Supported configuration options
 a) cps - calls per second
 b) max calls - limit of calls that tool will generate, default is turned off
 c) max failed calls - limit of failed calls, upon reaching it tool wont generate more traffic
                       , default is turned off.
 d) max concurrent calls - limits concurrent calls that tool will generate, regardless of cps and call length
                            for instance setting it to 5 wont generate more than 5-6 calls at the same time,
                            default turned off.
 e) call duration - duration of call after which tool will terminate it explicitly. Default is 2500 miliseconds                           
 f) local address - points local address to which tool will bind and put in SDP content, default is 127.0.0.1
 g) remote address - points to server address to be used, default is 127.0.0.1
 h) local port - local port which will be used to receive control protocol answer/requests
 i) remote port - port on remote machine used by mms to receive control protocol requests/answers
 k) codec - explicitly sets codec to be used in SDP for sessions, should match file
 l) test type - type of test/call to perform, currently there is only one supported, Announcement.
 
3. How to run
 a) GUI - GUI mode offers best insight into process. It allows to change CPS and call duration on the fly.
          To run in this mode simply type: mvn install -Prun
          GUI has predefined values for configuration options, "-1" is valid value indicating that 
          option is turned off/unbound.
 b) CLI - this mode offers capability to run on machine which dont have GUI/XServer/Desktop etc.
          It allows only to configure options and terminate on demand.
          There are several ways to run it:
          I)  sh script - script is located in core/bin folder. If ran without parameters it will print usage 
                          information - how to configure, example configuration, etc
          II) mvn script - its less encouraged since mvn does not allow tool to receive input from user.
                           However it still can be configured to terminate on certain condition via properties.
                           To see list of configuration options run: mvn install -Prun-cli-usage
                           To run testtool type: mvn install -Prun-cli, to ocnfigure specific options
                           one has to modify pom profile with id: run-cli
                           
4. Jitter graphs.
 Tool allows to graph in two modes:
  - GUI - simply opens new window with jiiter graph
  - CLI - after series of tests tool creates txt files with some data, after executing last test with command line option
          "--collectivegraph" will produce graph.txt file which agregates present tests into one file allowing to easy import
          to Calc or Excel(it should be also fairly easy to plot with tools like GNU plot or even matlab.)
          Tool takes one call from test and writes data into agregation file, each call takes one collumn, fields in collumn
          can be described as: call duration, cps, max concurrent calls, max fail calls, failed calls, max calls, total performed calls
          , sequence, avg jitter, peak jitter.

 