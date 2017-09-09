\ \
\ Author: <AUTHORNAME> (<AUTHOREMAIL>)
\ Committer: <COMMITTERNAME>
\ 
\ Creation Date:  Sat Sep 9 16:21:25 GMT+2 2017 
\ Module Name:    irqdemo - A simple demo of the interruptsystem of J1Sc
\ Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL 
\ 
\ Hash: <COMMITHASH>
\ Date: <AUTHORDATE>
\ \

: blink leds@ invert leds! ;

: isr blink ;
 
