  ##        ##                     
  ##        ##      ########      
  ##        ##    ##               #####
  ##        ##      ########            #
  ##        ##              ##     #####   
  ##        ##    ##        ##          #
    ########        ########      ###### 
release 0.1 - August 98
--------------------------------------------------------------
US3 - An American English male voice for the MBROLA synthesizer

Created by :  Oregon Graduate Institute of Science & Technology,
recorded by Mike Macon and Terri Lander at the Center for Spoken
Language Understanding (CSLU), USA .

The OGI diphone synthesizer and the development of the voices is described
in:

M. Macon, A. Cronk, J. Wouters, and A. Kain,
"OGIresLPC: Diphone synthesiser using residual-excited linear prediction",
Technical Report CSE-97-007, Department of Computer Science,
Oregon Graduate Institute of Science and Technology, September 1997.

--------------------------------------------------------------
Table of Contents
--------------------------------------------------------------

1.0 Description of the US3 diphone database
2.0 Installation and tests

--------------------------------------------------------------
1.0 Description of the US3 diphone database
--------------------------------------------------------------

US3   is a diphone database  for  american english, consisting of 2065
diphones, male voice.

The following  phoneme  symbols are  assumed in  our diphone  sets. It
slightly differ  for the SAMPA alphabet since  american english is not
british english.

SYMBOL  PRONOUNCED LIKE IN
p     drop proxy 
t     plot tromp
4     later (flapped allophone of t)
k     rock  crop

b     cob box
d     nod dot
g     jog gospel

f     prof fox
s     boss sonic
S     wash shop
tS    notch chop
T     cloth thomp

v     salve volley
z     was zombie
Z     garage jacques
dZ    dodge jog
D     clothe thy

m     palm mambo
n     john novel
N     bong
l     doll lockwood
l=    litle
r     star roxanne
j     yacht
w     show womble
h     harm
r=    her urgent

i     even
A     arthur
u     oodles
I     illness
E     else
{     apple
V     nut
U     good
@     about
EI    able
AI    island
OI    oyster
@U    over
aU    out
O     all

--------------------------------------------------------------
2.0 Installation and Tests
--------------------------------------------------------------

If  you  have  not copied   the MBROLA software   yet,  please consult
the MBROLA project homepage and get it.

Copy us3.zip into the mbrola directory and unzip it : 

   unzip us3.zip (or pkunzip on PC/DOS)

Try 

   mbrola us3 TEST/mike.pho test.wav

to create a sound file for a short excerpt  of Alice in Wonderland. In
this   example the  audio  file   follows  the RIFF   Wave format. But
depending  on the extension test.au,  test.aif, or test.raw other file
formats can be obtained. Listen to it with your favorite sound editor,
and try the other command files  (*.pho) to have a  better idea of the
quality of speech that  can  be synthesized with   MBROLA and the  US3
database.

On Unix  systems you can pipe  the audio ouput  to the sound player as
on a HP : mbrola us3 phone.pho - | splayer -srate 16000 -l16

Also refer  to the readme.txt file provided  with  the mbrola software
for using it. 

        
--------------------------------------------------------------
Interesting links: http://cslu.cse.ogi.edu/tts/

Contact:

Oregon Graduate Institute of Science & Technology, 
P.O. Box 91000
Portland
Oregon 97291-1000
USA

email: macon@cse.ogi.edu
