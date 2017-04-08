\ \
\ Author: <AUTHORNAME> (<AUTHOREMAIL>)
\ Committer: <COMMITTERNAME>
\ 
\ Creation Date:  Wed Feb 22 22:05:05 GMT+1 2017
\ Module Name:    peripheral - holds all words to work with the common peripherals
\                              provided by a Nexys4 DDR2 board from Digilent
\ Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL 
\ 
\ Hash: <COMMITHASH>
\ Date: <AUTHORDATE>
\ \

( c -- ) \ write value to LED-register
: leds!
    64 io!
;

( -- c) \ read the current value of the LED-register
: leds@
    64 io@
;

( c -- ) \ write to the directions register of PModA
: pmodADir!
    96 io!
;

( -- c) \ read the directions register of PModA
: pmodADir@
    96 io@
;

( c -- ) \ write value to PModA (read pins are ignored)
: pmodA!
    100 io!
;

( -- c) \ read the value of PModA
: pmodA@
    100 io@
;

