Version 3.01h mar jun  8 18:32:35 MEST 1999
M     M  BBBB    RRRR      OOO    L        A
MM   MM  B   B   R   R    O   O   L       A A
M M M M  B   B   R   R   O     O  L      A   A
M  M  M  BBBB    RRR     O     O  L     AAAAAAA
M     M  B   B   R  R    O     O  L     A     A
M     M  B    B  R   R    O   O   L     A     A
M     M  BBBBB   R    R    OOO    LLLLL A     A

--------------------------------------------------------------
Table of Contents
--------------------------------------------------------------

1.0 License
2.0 A brief description of the MBROLA software
3.0 Distribution
4.0 Installation, and Tests
5.0 Format of input and output files - Limitations
6.0 Joining the MBROLA project as a user
7.0 Joining the MBROLA project as database provider
8.0 Acknowledgments
9.0 Contacting the author

--------------------------------------------------------------
1.0 License
--------------------------------------------------------------

This program and object code is being provided to "you", the licensee,
by Thierry  Dutoit, the "author",  under  the following license, which
applies to any  program, object code or other   work which contains  a
notice placed  by  the copyright holder saying  it  may be distributed
under the terms of this license.   The "program", below, refers to any
such program, object code or work.

By obtaining,  using and/or copying  this program, you  agree that you
have   read,  understood,  and   will  comply   with  these  terms and
conditions:

Terms and conditions for the distribution of the program
--------------------------------------------------------

This program may not be sold or incorporated into any product which is
sold without prior permission from the author.

When no  charge is made, this  program  may be copied  and distributed
freely, provided that   this notice  is  copied  and distributed  with
it. Each time you redistribute  the program (or any  work based on the
program), the recipient   automatically receives  a license from   the
original  licensor to copy or distribute  the program subject to these
terms and conditions.  You may  not impose any further restrictions on
the recipients' exercise of   the rights granted  herein. You  are not
responsible for enforcing compliance by third parties to this License.

If you wish to incorporate the program  into other free programs whose
distribution conditions are different, write to  the author to ask for
permission.

If, as   a consequence of a   court judgment  or  allegation of patent
infringement or  for any other reason  (not limited to patent issues),
conditions are imposed  on you (whether  by court order,  agreement or
otherwise) that contradict the conditions of this license, they do not
excuse   you from the   conditions  of this  license.    If you cannot
distribute so as to satisfy simultaneously your obligations under this
license and any other pertinent obligations, then as a consequence you
may   not distribute the  program at  all.    For example, if a patent
license would not permit royalty-free redistribution of the program by
all those who receive copies directly or  indirectly through you, then
the only way you  could satisfy both it and  this license would be  to
refrain entirely from distribution of the program.

Terms and conditions on the use of the program
----------------------------------------------

Permission  is  granted   to use  this   software  for non-commercial,
non-military purposes,  with and  only  with  the voice   and language
databases  made available by  the author  from the  MBROLA project www
homepage:

         http://tcts.fpms.ac.be/synthesis

In return, the author asks you to mention the MBROLA reference paper:

T. DUTOIT, V. PAGEL, N. PIERRET, F.  BATAILLE, O. VAN DER VRECKEN
"The MBROLA Project: Towards a Set of High-Quality Speech
Synthesizers Free of Use for Non-Commercial Purposes"
Proc. ICSLP'96, Philadelphia, vol. 3, pp. 1393-1396.  

or,  for a more general  reference   to Text-To-Speech synthesis,  the
book:

An Introduction to Text-To-Speech Synthesis,
T. DUTOIT, Kluwer Academic Publishers, Dordrecht 
Hardbound, ISBN 0-7923-4498-7
April 1997, 312 pp. 

in any scientific publication referring to work for which this program
has been used.

Disclaimer
----------

THIS  SOFTWARE CARRIES NO   WARRANTY, EXPRESSED OR IMPLIED.  THE  USER
ASSUMES ALL RISKS, KNOWN OR UNKNOWN, DIRECT OR INDIRECT, WHICH INVOLVE
THIS SOFTWARE IN ANY WAY. IN PARTICULAR, THE  AUTHOR DOES NOT TAKE ANY
COMMITMENT IN VIEW OF ANY POSSIBLE THIRD PARTY RIGHTS.

--------------------------------------------------------------
2.0 A brief description of MBROLA
--------------------------------------------------------------

MBROLA is a  speech synthesizer  based  on the concatenation  of
diphones. It takes a list of phonemes as input, together with prosodic
information  (duration of phonemes  and a piecewise linear description
of  pitch), and produces  speech samples  on 16  bits (linear), at the
sampling frequency of the diphone database. 

It is therefore NOT a Text-To-Speech  (TTS) synthesizer, since it does
not accept raw text as input.  In  order to obtain  a full TTS system,
you need to use this synthesizer in combination with a text processing
system that produces phonetic and prosodic commands.

We maintain a web page with pointers to such freely available systems:

http://tcts.fpms.ac.be/synthesis/mbrtts.html

This software is the heart of the MBROLA project,  the aim of which is
to   obtain  a set a   speech  synthesizers for as   many languages as
possible, free of use for non-commercial applications.

The terms of this project can be summarized as follows :

After some official agreement between the  author of this software and
the  owner of  a diphone database,   the database is  processed by the
author and  adapted  to the mbrola format,  for  free.  The  resulting
mbrola diphone database  is made available  for non-commercial  use as
part of  the MBROLA project.  Commercial rights on the mbrola database
remain with  the database provider, for exclusive  use with the mbrola
software.

The ultimate goal of this project is to boost  up academic research on
speech synthesis, and particularly on prosody generation, known as one
of the biggest challenges taken  up by Text-To-Speech synthesizers for
the years to come.  If you want to  provide  a database to  the mbrola
project, write first to mbrola@tcts.fpms.ac.be

More details can be found at the MBROLA project homepage :

http://tcts.fpms.ac.be/synthesis

The synthesizer uses a synthesis method known itself as MBROLA.

--------------------------------------------------------------
3.0 Distribution
--------------------------------------------------------------

This distribution of mbrola contains the following files :

mbrola.exe  or mbrola: An  executable  file of the synthesizer  itself
(depends on the computer supposed to run it) 
readme.txt : This file

As   such,  it  requires an  MBROLA    language/voice database  to run
properly.  American English,   Arabic, Brazilian Portuguese,   Breton,
British English,  Croatian,  Dutch, Estonian, French,   German, Greek,
Mexican    Spanish,  Romanian, Spanish  and  Swedish   voices are made
available. Additional languages and voices   will be available in  the
context of the MBROLA project.

Please consult the MBROLA project homepage to get the voices:

http://tcts.fpms.ac.be/synthesis

--------------------------------------------------------------
4.0 Installation and Tests
--------------------------------------------------------------

The following computers/OS are currently supported :

SUN Sparc 5/S5R4 (Solaris2.4)
HPUX9.0 and HPUX10.0 
VAX/VMS V6.2 (V5.5-2 won't work)
DECALPHA(AXP)/VMS 6.2
AlphaStation 200 4/233
AlphaStation 200 4/166
IBM RS6000 Aix 4.12
PC486/DOS6 (but other PCs/DOSs should do, too)
PC486/Windows 3.1
PC486/Windows 95 
PC-Pentium/Windows 98
PC-Pentium/Windows NT
PC/LINUX 1.2.11
PC/LINUX Redhat6.2
PCPentium120/Solaris2.4
OS/2
BeBox
BeOs (PPC,i386)
Macintosh
Sun Ultra1/SuSE Linux 7.0

Please send acknowledgement when mbrola works  on a machine not listed
here. A special  DLL version is  distributed for  PC/Windows to  allow
direct audio output; check on the Mbrola site the Mbrolatools package.

See the MBROLA Homepage if your computer or OS is not supported yet.

Assuming you have  copied  the right .zip   file,  create a  directory
mbrola (although this is not  critical), copy the mbrXXX.zip file into
it (in which XXX stands for a version number), and unzip the file:

unzip mbrXXX.zip (or pkunzip on PC/DOS)

You are now ready to synthesize your first words....

First try: mbrola

to see the terms and conditions on the use of this software. 

Then try: mbrola -h 

to get some help on how to use the software:

> USAGE: ./synth [COMMAND LINE OPTIONS] database pho_file+ output_file
>
>A - instead of pho_file or output_file means stdin or stdout
>Extension of output_file ( raw, au, wav, aiff ) tells the wanted audio format
>
> Options can be any of the following:
> -i    = display the database information if any
> -e    = IGNORE fatal errors on unkown diphone
> -c CC = set COMMENT char (escape sequence in pho files)
> -F FC = set FLUSH command name
> -v VR = VOLUME ratio, float ratio applied to ouput samples
> -f FR = FREQ ratio, float ratio applied to pitch points
> -t TR = TIME ratio, float ratio applied to phone durations
> -l VF = VOICE freq, target freq for voice quality
> -R RL = Phoneme RENAME list of the form a A b B ...
> -C CL = Phoneme CLONE list of the form a A b B ...
> 
> -I IF = Initialization file containing one command per line
>         CLONE, RENAME, VOICE, TIME, FREQ, VOLUME, FLUSH, COMMENT,
>         and IGNORE are available

Now  in order to go   further, you need to get  a version of an MBROLA
language/voice database  from  the  MBROLA  project homepage.  Let  us
assume  you   have  copied   the   FR1   database  and    referred  to
the accompanying fr1.txt file for its installation. 

Then try: mbrola fr1/fr1 fr1/TEST/bonjour.pho bonjour.wav

it uses the format:

mbrola diphone_database command_file1 command_file2 ... output_file

and creates a sound file for the word 'bonjour' ( Hello !).

Basically the output file is composed of signed  integer numbers on 16
bits, corresponding to samples at the sampling frequency of the MBROLA
voice/language  database (16 kHz for  the diphone database supplied by
the author of MBROLA : Fr1).  MBROLA  can produce different audio file
formats:  .au, .wav, .aiff, .aif,  and   .raw files  depending  on the
ouput_file extension. If the  extension is not recognized,  the format
is RAW (no  header). We recommand  .wav for Windows,  and .au for Unix
platforms.

To display information  about the phoneme  set  used by the  database,
type:
		  mbrola -i fr1/fr1

It displays the phonetic  alphabet  as well as copyright   information
about the database.

Option  -e makes  Mbrola  ignore  wrong or  missing  diphone sequences
(replaced  by silence)  which can  be quite useful when debugging your
TTS. Equivallent to "IGNORE" directive in the initialization file (N.B
replace the obsolete ;;E=OFF , unsupported in .pho file).

Optional parameters let you shorten  or lengthen synthetic speech  and
transpose it by providing optional time and frequency ratios:

mbrola -t 1.2 -f 0.8 fr1/fr1 TEST/bonjour.pho bonjour.wav

or its equivalent in the initialization file:

TIME 1.2
FREQ 0.8

for instance,  will result in  a  RIFF Wav file  bonjour.wav 1.2 times
longer than  the previous one (slower  rate), and containing speech in
which all  fundamental frequency  values  have been multiplied  by 0.8
(sounds lower).

You can also set  the values of these  coefficients directly in a .pho
file by adding special escape sequences like : 

;; F=0.8
;; T=1.2

You can change the voice characteristics with the -l parameter. If the
sampling rate of your database   is 16000, indicating -l 18000  allows
you to shorten the vocal tract by a ratio of 16/18 (children voice, or
women voice  depending   on the voice you're   working  on).  With  -l
10000, you  can lengthen the vocal  tract by a  ratio of 16/10 (namely
the voice  of  a Troll).  The  same command  in an initialization file
becomes "VOICE 10000".

Option  "-v"  specifies a  VolumeRatio which   multiplies each  output
sample. In  the example below, each sample  is multipliead by 0.7 (the
loudness goes down). Warning: setting  VolumeRatio too high  generates
saturation. 

		 mbrola -v 0.7 fr1/fr1 TEST/bonjour.pho bonjour.wav

       or add "VOLUME 0.7" in an initialization file

The -c option lets you specify which symbol  will be used as an escape
sequence for comments and commands in .pho files. The default value is
the semi-colon ';', but  you may want  to change this if your phonetic
alphabet uses this symbol, like in:

mbrola -c ! fr1/fr1 TEST/test1.pho test2.pho test.wav

equivalent to "COMMENT !" in an initialization file

The -F option lets you specify which symbol will be used to Flush the
audio output. The default value is #, you may want to change the
symbol like in:

mbrola -F FLUSH_COMMAND fr1/fr1 test.pho test.wav

equivalent to "FLUSH FLUSH_COMMAND" in the initialization file.


Using Pipes
-----------

A - instead  of command_file or  output_file means stdin or stdout. On
multitasking machines, it is easy to run  the synthesizer in real time
to obtain audio output from the audio device, by using pipes.

Renaming and Cloning phonemes
-----------------------------

It may happen that the language  processing module connected to MBROLA
doesn't use the same phonemic alphabet as the voice used. The Renaming
and  Cloning mechanisms   help you  to   quickly solve  such  problems
(without adding extra  CPU  load). The  only  limitation about phoneme
names is that they can't contain blank characters.

If, for instance, phoneme  "a" in the mbrola voice  you use  is called
"my_a"  in your alphabet,  and phoneme "b" is  called "my_b", then the
following command solves the problem:

mbrola -R "a my_a   b my_b" fr1/fr1 test.pho test.wav

You can give as  many renaming pairs as  you want. Circular definition
are  not a problem -> "a b  b c" will rename original [a] into [b] and
original [b] into [c] independantly ([a] won't be renamed to [c]).

LIMITATION: you    can't rename a phoneme    into another that already
exists.

The cloning  mechanism does  exactly the same  thing,  though the  old
phoneme  still exists  after renaming. This  is usefull if  you have 2
allophones in your alphabet, but the Mbrola voice only provides one.

Imagine  for  instance, that  you  make  the disctinction  between the
voiced [r] and its unvoiced counterpart [r0] and that  you are using a
syllabic version [r=]. If as a first approximation  using [r] for both
is OK, then you may use an Mbrola voice that only provides one version
of [r] by running:

mbrola -C "r r0  r r=" fr1/fr1 test.pho test.wav

which   tells  the synthesizer  that   [r0]  and [r=]   should be both
synthesized as [r].  You  can write a  long  cloning list of   phoneme
pairs to fit your needs. 

Renaming  and cloning eats CPU since  the  complete diphone hash table
has to be rebuilt, but once the renaming or cloning has occurred there
is  absolutely NO  RELATED  PERFORMANCE DROP.  So  using this  feature
is more efficient   than  a  pre-processor, though   incompatibilities
cannot always be solved by a simple phoneme mapping.

Before renaming anything as #, check paragraph 5.4

When you  have long cloning and  renaming lists, you  can conveniently
write them into  an  initialization file  according to the   following 
format:

RENAME a my_a
RENAME b my_b
CLONE  r r0
CLONE  r r=

The obsolete ";; RENAME  a my_a" can't be  used in .pho file  anymore,
but is correctly parsed in initialization files. 

Note to Festival and EN1 users: the consequence of the change above is
that you must change the previous call format "mbrola en1 en1mrpa ..."  
into "mbrola -I en1mrpa en1 ...".


BELOW ARE A NUMBER OF MACHINE DEPENDANT HINTS FOR BEST USING MBROLA

On MSDOS/Windows or OS/2
------------------------

Type: mbrola fr1/fr1 TEST/bonjour.pho bonjour.wav

Then you can play the RIFF Wav file with windows sound utility On OS/2
pipes may be used just like below.

REMARK: MbrolaTools provide an excellent DLL  and graphical pho player
called Mbroli. We  advise you to  use them  instead of mbrola.exe  for
Windows.

On modern Unix systems such as Solaris or HPUX or Linux
-------------------------------------------------------

mbrola fr1/fr1 TEST/bonjour.pho -.au | audioplay

where  audioplay is your audio  file player (*  the name vary with the
platform, e.g. splayer for HPUX *)

If  your audioplayer  has problems with sun .AU  files, try  with .raw
Never use .wav format when you pipe the ouput (mbrola can't rewind the 
file to write   the audio  size in   the header). Wav   format was not
developped for Unix (on the contrary Au format let  you specify in the
header "we're on a pipe, read until end of file").

NOTE FOR LINUX: you can use the GPL rawplay program provided at
      ftp://tcts.fpms.ac.be/pub/mbrola/pclinux/

On Sun4 or with machines with an  old audio interface
-----------------------------------------------------

Those machines are   now quite  old and  only   provide a mulaw   8Khz
output. A hack is:

mbrola fr1/fr1 input.pho - | sox -t raw  -sw -r 16000  - -t raw -Ub -r 8000 - > /dev/audio

(providing you have the public domain sox utility developed by Ircam).
You should  hear  'bonjour' without the   need  to create intermediate
files. Note  that we strongly recommend  that you DON'T use SOX, since
its resampling  method (linear interpolation) will  permanently damage
the sound.

Other   solution:  The UTILITY.ZIP  file   available  from the  MBROLA
homepage provides RAW2SUN which does this conversion.

On VAX or AXP workstations
--------------------------

To  make it  easier for   users to find   MBROLA,  you should add  the
following command to your system startup procedure:

$ DEFINE/SYSTEM/EXEC MBROLA_DIR disk:[dir]

where "disk:[dir]"  is the name  of the directory  you created for the
MBROLA_DIR files.  You could also  add  the following command to  your
system login command procedure:

$ MBROLA :== $MBROLA_DIR:MBROLA.EXE
$ RAW2SUN :== $MBROLA_DIR:RAW2SUN.EXE

to use the decsound device:

$ MCR DECSOUND - volume 40 -play sound.au 

See also the MBR_OLA.COM batch file in  the UTILITY.ZIP file available
from the MBROLA  Homepage if you cannot   play 16 bits sound files  on
your machine.

--------------------------------------------------------------
5.0 Format of input and output files - Limitations
--------------------------------------------------------------

5.1 Phoneme commands
--------------------

The input file bonjour.pho in the above example simply contains :

; bonjour 
_ 51 25 114
b 62 
o~ 127 48 170.42 
Z 110 53.5 116 
u 211 
R 150 50 91 
_ 91

This shows the format of the input data  required by MBROLA. Each line
contains  a phoneme name, a duration  (in ms),  and a series (possibly
none)  of pitch  targets  composed of  two   float numbers each  : the
position  of  the  pitch target   within   the phoneme  (in  %  of its
total duration), and the pitch value (in Hz) at this position.

In order to increase readability, it is also possible to enclose pitch
target  in parentheses. Hence, the   first  line of bonjour.pho  could
be written :

_ 51 (25,114)

it tells the synthesizer to produce a  silence of 51  ms, and to put a
pitch target of  114  Hz at 25%  of  51  ms.  Pitch targets  define  a
piecewise linear  pitch curve.  Notice  that the intonation curve they
define  is continuous,  since  the  program automatically drops  pitch
information when synthesizing unvoiced phones.

The  data on each  line  is  separated  by  blank characters or  tabs.
Comments can optionally be introduced  in command files, starting with
a  semi-colon ';'.  This default  can be  overrun  with the -c  option 
of the command line.

Another  special  escape sequence ';;'   allows  the user to introduce
commands in the middle of  .pho files as  described below. This escape 
sequence is also affected by the -c option.

5.2 Changing the Freq Ratio or Time Ratio
-----------------------------------------

A command escape  sequence containing a  line like "T=xx" modifies the
time  ratio to  xx,  the same result   is obtained on the  fundamental
frequency by replacing T with F, like in:

;; T = 1.2
;;F=0.8


5.3 Flush the output stream
---------------------------

Note, finally, that the synthesizer outputs chunks of synthetic speech
determined as   sections of the piecewise   linear pitch curve. Phones
inside a section of  this curve are synthesized  in one go.  The  last
phone of each chunk, however, cannot be properly synthesized while the
next phone is   not known (since the program   uses diphones  as  base
speech   units). When  using    mbrola  with pipes,    this  may be  a
problem. Imagine,  for instance,  that  mbrola is  used to   create  a
pipe-based speaking clock on an HP:

speaking_clock | mbrola - -.au | splayer

which tells the time,  say, every 30 seconds.  The  last phone of each
time announcement will only be synthesized  when the next announcement
starts.    To bypass this problem,   mbrola accepts  a special command
phone, which flushes the synthesis buffer : "#"

This default character can be replaced by another symbol thanks to the
command:

;; FLUSH new_flush_symbol

Another important issue with piping under  UNIX, is the possibility to
prematurely end the audio output, if  for example the user presses the
stop button   of  your application. Since release 3.01, Mbrola handles
signals.

If in the  previous example the user wants  to  interrupt the speaking
clock message, the application just needs to send the USR1 signal. You
can send such a signal from the console with:

	 kill -SIGUSR1 mbrola_process_number

Once mbrola catches  the signal,  it reads its  input stream  until it
gets EOF or a FLUSH command (hence, surrounding sections with flush is
a good habit).

Limitations of the program
--------------------------

Phones can be synthesized with  a maximum duration which depends on
the fundamental frequency with which they are produced. The higher the
frequency, the lower  the duration.  For a  frequency  of 133  Hz, the
maximum duration is 7.5 sec. For a frequency of 66.5 Hz, it is 15 sec.
For a frequency of 266 Hz, it is 3.75 sec.

--------------------------------------------------------------
6.0 Joining the MBROLA project as a user 
--------------------------------------------------------------

For convenience, we have defined two mailing lists :

* mbrola-interest@tcts.fpms.ac.be :  a forum for  MBROLA questions and
issues. It is   used  by the   maintainers of  the mbrola  project  to
announce new releases, bug fixes,  new voices and languages, and other
information of interest to all MBROLA  users.  Users who want to share
.pho files  or free applications running on  top of mbrola should send
mail to mbrola-interest.

It  is your interest, as a  user,  to subscribe to the mbrola-interest
mailing list, by sending an e-mail to :

          mbrola-interest-request@tcts.fpms.ac.be

with the word  'subscribe' in either the header  or the main  text. To
unsubscribe, just send another mail with 'unsubscribe'.

BUGS
----

If you detect a bug, or if you find an input  for which the quality of
the speech provided by mbrola is  not as good  as usual, first consult
the  FAQ  file  from  the  MBROLA Project  homepage,   which will   be
frequently updated.

If this is  of no help, send  a kind mail to mbrola@tcts.fpms.ac.be in
which you include  the .pho file with  which  the problem  appears and
mention your machine architecture.

NEW DATABASES
-------------

If you want   to participate to  the  mbrola  project by  providing  a
diphone database (i.e. a set of sample files  with one example of each
diphone in your  language), refer to the mbrola  WWW homepage, or send
an email to: mbrola@tcts.fpms.ac.be.

APPLICATIONS
------------

If  you have used mbrola   to build speaking apps  on  top of it (like
talking  clocks,  talking    agendas, talking  tools   for handicapped
persons, etc., and  want to  make it  available to  the community (for
free, of course, and for non-commercial, non-military applications, as
imposed by the mbrola license agreement), just make an announcement to
the mbrola mailing list:

          mbrola-interest@tcts.fpms.ac.be. 

COMMERCIAL VERSION
------------------

If you are interested in the commercial version of mbrola (source code
available), send an email to: mbrola@tcts.fpms.ac.be 

FEEDBACK
--------

If you simply find  this initiative useful, please drop  us a note  at
mbrola@tcts.fpms.ac.be. We have spent a lot of our time to provide you
with this program, and we would like to get some feedback in return.

Don't forget, either, to mention the MBROLA reference paper :

T. DUTOIT, V. PAGEL, N. PIERRET, F. BATAILLE, O. VAN DER VRECKEN
"The MBROLA Project: Towards a Set of High-Quality Speech
Synthesizers Free of Use for Non-Commercial Purposes" 
Proc. ICSLP 96, Philadelphia, vol. 3, pp. 1393-1396

or,  for  a more  general reference  to  Text-To-Speech synthesis, the
book:

An Introduction to Text-To-Speech Synthesis,
T. DUTOIT, Kluwer Academic Publishers, Dordrecht 
Hardbound, ISBN 0-7923-4498-7
April 1997, 312 pp. 

in any scientific publication referring to work for which this program
has been used.

--------------------------------------------------------------
7.0 Joining the MBROLA project as a database provider
--------------------------------------------------------------

One of the biggest interests of the MBROLA project (and definitely its
most  original aspect) lies in its  ability to provide an ever growing
set of languages/voices to users.

To achieve this goal, the MBROLA project has  itself been organized so
as to incite  other research labs  or companies to share their diphone
databases.

The terms of this sharing policy can be summarized as follows :

1. We shall only  use your database to  adapt it to the mbrola format,
and destroy the copy when this is done.

2.   The resulting mbrola diphone  database will  be copyright Faculte
Polytechnique de Mons.  Non-commercial   use  of the database in   the
framework of  the MBROLA project    will be automatically  granted  to
Internet users. In return, we shall send you a license agreement which
will  transfer  all our  commercial  rights on  the  database  to you,
provided the database is used with and only with the MBROLA program.

3. All these  details will be fixed by  some official agreement before
you send us anything.

If you want to create a database from scratch
---------------------------------------------

First, you should be aware that recording a  diphone database is not a
trivial operation. If it is not performed carefully, the result can be
deceiving. FR1, for  instance, required about  one month of  work, yet
with the help of some efficient laboratory  tools for signal recording
and  editing.  What is more,  some  phonetic knowledge of the targeted
language is necessary to create the initial corpus.

So if you just  think of designing a new  diphone database as a  game,
forget it.

If, on the contrary, you are willing to spend some time to provide the
MBROLA community with a new language or voice, or  if you already have
a diphone database and wish to share it  in mbrola format (and receive
in return the rights  for  any commercial  exploitation of the  mbrola
diphone database we will create for you), welcome here.

If you want to build a new diphone database, please contact the author
first.  He will help   you as much  as  he can, by providing  phonetic
information if available for instance.

In all cases, make a first dummy  trial : create  a small corpus for a
few diphones, record them, segment them, equalize them if you can, and
send the result directly to the author.  He will  test your data, tell
you how good it is, and what should be done to make it better.

If you want to share an existing database 
-----------------------------------------

contact the authors (see below).

--------------------------------------------------------------
8.0 Acknowledgments
--------------------------------------------------------------

I  would like to thank   Vincent Pagel (Mons  / BE)  for his intensive
programming, testing, and debugging of this program, and for all sorts
of fruitful discussions. Vincent  also wrote MBRDICO a general purpose
trainable phonetizer. Not to  forget  Nicolas Pierret and Olivier  Van
der Vreken, for their contribution to the Mbrola coder.

Then let's greet  our  pioneer database  providers: 
     Alejandro Barbosa  (MX1),
	  Aggelos Bletsas    (GR1), 
	  Marian Boldea		(RO1), 
	  Gösta Bruce			(SW1), 
	  Alistair Conkie	   (EN1 ES1), 
	  Denis Costa			(BR1), 
	  Arthur Dirksen		(NL1 NL2), 
	  Thierry Dutoit		(FR1), 
	  Céline Egéa			(FR2), 
	  Fred Englert		   (DE1 DE2), 
	  Nikolaj Lazic		(CR1),
	  Mike Macon         (US3 MX1),
	  Einar Meister		(EE1), 
	  Yann-Ber Messager  (BZ1), 
	  Vincent Pagel		(FR3 FR4), 
	  Marcus Philipson	(SW1), 
	  George Sergiadis	(GR1), 
	  Nawfal Tounsi		(AR1), 
	  Raymond Veldhuis	(NL3), 
	  Gordon Tischer		(ES2),
     Johan Wouters      (US3)
	  and the team at University Autonoma of Barcelona (ES1)!

May they be thanked for their work.

Sam  Przyswa (NEXT  Paris/FR),  Fred Englert (IBMRS600  Frankfurt/DE),
Arnaud   Gaudinat (VAX-VMS    University   of  Geneva,  CH),   Cyrille
Mastchenko (BeOS  Montreal/CA), Michael C.  Thornburgh (SCO-Unix USA),
Bruno  Langlois (Java   port  Quebec/CA), Christophe M.   Vallat  (OS2
Domerat/FR),   Cristiano Verondini  (Mac  Bologna/Italy), Gerald Kerma
(Mac  G'K2  Vaugrigneuse/FR),  David  Woodman (SUN4 Berkshire/England)
Gary Thomas   (Linux-PPC Grenoble/France), Thomas Fletcher (QNX-OS CA),
Philippe Devallois(Mac DLL),Thomas Agopian (BeOs), Stephen Isard(Linux
PC Redhat6.2), Matthias Nutt (Ultra 1)for their help in the compilation
of MBROLA on many platforms.

Arnaud  Gaudinat (Lausanne/CH),  Thierry Gartiser (Nancy/France), Alec
Epting  (Summer    Institure of  Linguistics/USA),   Michael  M. Cohen
(University of California - Santa  Cruz), and Patrick Bouffer (France)
have arranged mirror sites.

David Haubensack  has written a French  TTS in PERL, Stephen Isard and
Alistair Conkie  have  provided the Freespeech  British  English TTS!! 
Alan Black and Paul Taylor have supported  the Mbrola Project in their
great Festival multilingual TTS Project. 

Fabrice Malfrere (Mons/BE)   who  has developped an  efficient  speech
alignment program for Windows (distributed on the mbrola site).

Alain Ruelle  (Mons/BE) who  has developped the  MBRPlay  dll and  the 
Mbroli interactive pho file player for Windows.

Nawfal Tounsi(Mons/BE) who has developped the W project aimed at
helping disable people talk with the help of Mbrola. 

Last but not  least, I am  also greatly indebted to Francois  Bataille
(Mons/BE) for having supported the creation of this internet project.

--------------------------------------------------------------
9.0 Contacting the author
--------------------------------------------------------------

Dr Thierry Dutoit

Faculte Polytechnique de Mons, TCTS Lab,
31, bvd Dolez, B-7000 Mons, Belgium. 
tel : /32/65/374133
fax : /32/65/374129
e-mail: mbrola@tcts.fpms.ac.be, for general information, 
questions on the installation of software and databases.

