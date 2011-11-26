==========================================================================
! Welcome to Mobicents Media server (MMS) - The Open Source Media server !
==========================================================================
This mms-jboss-5.1.0.GA-2.0.0.BETA2 binary is Mobicents Media Server embedded in JBoss AS
    * Mobicents Media Server
    * Test framework built on top of MGCP to test the Media Server Performance.


This is version of Media Server is embedded in JBoss AS 5.0.1.GA

Mobicents Media Server is open source media server aimed to:

-Deliver competitive, complete, best-of-breed media gateway functionality featuring highest quality.
-Meet the demands of converged wireless, wireline, cable broadband access and fixed-mobile converged VoIP
 networks from a single media gateway platform
-Increase flexibility with a media gateway that supports wide variety of call control protocols and scales down
 to meet the demands of enterprises and small carrier providers.
-React quickly to dynamic market requirements.

Mobicents Media Server Home Page: http://www.mobicents.org/products_media_server.html
Mobicents documentation page: http://www.mobicents.org/mms/mms-docs.html
Version information: mms-jboss-5.1.0.GA-2.0.0.BETA2


To install media server
----------------------------------------------------------------------
1. Call run.bat (run.sh for linux ) from  mms-all-1.0.0.GA/jboss-5.1.0.GA/bin


To execute Test
----------------------------------------------------------------------
1. Call ant from /test directory


Highlights of 2.0.0.BETA2
----------------------------------------------------------------------
There are many new functionality addition in this release. 
1. Added a TTS component
2. Added proper logger messages to know if there are issues or can also be used to understand the media flow in MMS
3. Added a hardware player which directly plays the audio file in detected audio hardware
4. Disable hot-deployment if scanner perido set to 0 in MMS standalone.
5. NIO Socket architecture modified to use only one port for many media stream.

To know about all of the above and more fixes please look at http://code.google.com/p/mobicents/issues/list?q=label%3AComponent-Media-Server%20label%3AMilestone-Release-2.0.0.Beta2&updated=763&ts=1250269553&can=1


Download the nightly SNAPSHOT from http://hudson.qa.jboss.com/hudson/view/Mobicents/job/MobicentsMediaServerRelease/

About JBoss, a division of Red Hat
----------------------------------------------------------------------
JBoss, a division of Red Hat, is in the business of providing superior technical support to our customers. Our goal is to make Professional Open Source™ the SAFE CHOICE for you. We accomplish this by backing up our open source Java products with technical support services that are delivered by the core developers themselves. We can help you to train your staff and provide you with support at every stage of the application lifecycle - from development and integration through deployment and maintenance. Visit the JBoss Services page http://www.jboss.com/services/index for more information.
