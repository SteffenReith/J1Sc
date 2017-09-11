\ \
\
\ Author: Steffen Reith (Steffen.Reith@hs-rm.de)
\ 
\ Creation Date:  Wed Feb 22 22:05:05 GMT+1 2017
\ Module Name:    peripheral - holds all words to work with the common peripherals
\                              provided by a Nexys4 DDR2 board from Digilent
\ Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL 
\ 
\ \

( c -- ) \ write value to LED-register
: leds!
    $40 io!
;

( -- c) \ read the current value of the LED-register
: leds@
    $40 io@
;

( c -- ) \ write to the directions register of PModA
: pmodADir!
    $60 io!
;

( -- c) \ read the directions register of PModA
: pmodADir@
    $60 io@
;

( c -- ) \ write value to PModA (read pins are ignored)
: pmodA!
    $64 io!
;

( -- c) \ read the value of PModA
: pmodA@
    $64 io@
;

( -- c ) \ push the base address of timer A
: tABase
    $c0
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
    $e0
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
