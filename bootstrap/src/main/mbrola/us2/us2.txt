  ##        ##                     
  ##        ##      ########      ######
  ##        ##    ##             #     ##
  ##        ##      ########           ##
  ##        ##              ##    ######   
  ##        ##    ##        ##   ##   
    ########        ########     ########  
release 0.1 - August 98
--------------------------------------------------------------
US2 - An American English male voice for the MBROLA synthesizer

Created by :  Babel Technology SA
              Speech Synthesis Group

--------------------------------------------------------------
Table of Contents
--------------------------------------------------------------

1.0 Description of the US2 diphone database
2.0 Installation and tests
3.0 Announcement
4.0 Acknowledgements

--------------------------------------------------------------
1.0 Description of the US2 diphone database
--------------------------------------------------------------

US2   is a diphone database  for  american english, consisting of 2065
diphones, male voice.

The following  phoneme  symbols are  assumed in  our diphone  sets. It
slightly differ  for the SAMPA alphabet since  american english is not
british english.

SYMBOL  PRONOUNCED LIKE IN
p     drop proxy 
p_h   pod (aspirated allophone of p)

t     plot tromp
t_h   top (aspirated allophone of t)
4     later (flapped allophone of t)

k     rock  crop
k_h   cot (aspirated allophone of k)

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
Or Allophone of O in front of /r/ like in 'sort'

The diphone matrix is square, no diphone lacking... of course diphone
like t-N may not sound optimal :-)

--------------------------------------------------------------
2.0 Installation and Tests
--------------------------------------------------------------

If  you  have  not copied   the MBROLA software   yet,  please consult
the MBROLA project homepage and get it.

Copy us2.zip into the mbrola directory and unzip it : 

   unzip us2.zip (or pkunzip on PC/DOS)

Try 

   mbrola us2 TEST/alice.pho test.wav

to create a sound file for a short excerpt  of Alice in Wonderland. In
this   example the  audio  file   follows  the RIFF   Wave format. But
depending  on the extension test.au,  test.aif, or test.raw other file
formats can be obtained. Listen to it with your favorite sound editor,
and try the other command files  (*.pho) to have a  better idea of the
quality of speech that  can  be synthesized with   MBROLA and the  US2
database.

On Unix  systems you can pipe  the audio ouput  to the sound player as
on a HP : mbrola us2 phone.pho - | splayer -srate 16000 -l16

Also refer  to the readme.txt file provided  with  the mbrola software
for using it. 

--------------------------------------------------------------
3.0 Announcement
--------------------------------------------------------------

This database is a beta  release, and we  will take your comments into
account for    future  updates. Updates    will be  announced  on  the
mbrola-news mailing list.

For more  information and demos,  consult the Babel Technology SA home
page at: 

     http://www.babeltech.com

--------------------------------------------------------------
4.0 Acknowledgments
--------------------------------------------------------------

We would like to thank David Pullen for his steady pitch

--------------------------------------------------------------

Babel Technology SA
Speech Synthesis Group
33 bvd Dolez
B7000 MONS
Belgium

Tel: +32 65 37 42 75
Fax: +32 65 37 42 76
Email: pagel@babeltech.com
WWW: http://www.babeltech.com
        
--------------------------------------------------------------

