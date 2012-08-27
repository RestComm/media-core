MGCP TCK
------------------------------------------
MGCP TCK is available from http://jcp.org/aboutJava/communityprocess/final/jsr023/index.html

It is recommended to download the TCK from above mentioned link as it also contains information
on what TCK is doing.


How to execte TCK :
-------------------------------------------
TCK is divided in two parts 1) Call Agent - CA and 2) Residential Gateway - RGW

To start RGW, execute 'mvn exec:exec' from this folder. The RGW will start and bind to IP 0.0.0.0 and listen at port 2427. This is default profile in pom.xml

To start CA, execute 'mvn exec:exec -P CA' from this folder. The CA will start and bind to IP 0.0.0.0 and listen at port 2727


In case if you are executing the test on two different machine, make corresponding changes to the IP's in pom.xml


All the test's passes, however you will still see 'BUILD ERROR' in the end, that is because TCK test uses System.exit(1); at the end of TCK and
nonzero status code indicates abnormal termination.

Note : If you start RGW before CA, the last test of CA will fail at end or vice-a-versa . The flow of message for last test is

   RGW              command/response                    CA
    |---------------------AUCX-------------------------->
     <-------------------AUCX Res -----------------------|

    |---------------------AUEP-------------------------->
     <-------------------AUEP Res -----------------------|

    |---------------------CRCX-------------------------->
     <-------------------CRCX Res -----------------------|

    |---------------------DLCX-------------------------->
     <-------------------DLCX Res -----------------------|

    |---------------------EPCF-------------------------->
     <-------------------EPCF Res -----------------------|

    |---------------------MDCX-------------------------->
     <-------------------MDCX Res -----------------------|

    |---------------------RQNT-------------------------->
     <-------------------RQNT Res -----------------------|


    |---------------------NTFY-------------------------->
     <-------------------NTFY Res -----------------------|


    |---------------------RSIP-------------------------->
     <-------------------RSIP Res -----------------------|

Test is over once RSIP response is received. However as soon as RGW or CA is started, both starts with sending the AUCX and hence both are acting 
as CA at one time and RGW at other time. And one of them will receive the RSIP Resp before other and MGCP stack will exit causing the other 
end to fail. 

In case you have any queries please drop a mail to mobicents-public@googlegroups.com