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

: rotate
    leds@
    dup
    1 and if
	2/
	32767 and
	32768 or
    else
	2/ 32767 and
    then
    leds!
;

: isr rotate ;

( -- c ) \ push the base address of timer A
: tABase
    192
;

( c -- ) \ write the value on tos to the low part of timer A
: ltA!
    tABase 0 + io!
;

( c -- ) \ write the value on tos to the high part of timer A
: htA!
    tABase 1 + io!
;

( -- ) \ enable timer A
: entA
    1 tABase 2 + io!
;

( -- ) \ disable timer A
: distA
    0 tABase 2 + io!
;

( -- c ) \ push the interrupt controller base address
: iBase
    224
;

( a c -- ) \ write address a to interrupt vector c
: ivec!
    iBase + io!
;

( c -- ) \ write interrupt mask
: imask!
    iBase 4 + io!
;

( -- c ) \ get interrupt mask
: imask@
    iBase 4 + io@
;

: iDemo \ Simple interrupt demo
    \ Init leds
    32768 leds!
    \ Set timerA to 250 * 2^16 ticks
    0 ltA!
    250 htA!
    \ Use word isr as isr for interrupt 1
    ['] isr
    1 ivec!
    \ Enable interrupt 1 by setting the correct mask
    2 imask!
    \ Start timerA
    entA
;
