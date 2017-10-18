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

: ledsbase ( -- c) \ push the base address of the LED array
    $40
;

( c -- ) \ write value to LED-register
: leds!
    ledsbase io!
;

( -- c) \ read the current value of the LED-register
: leds@
    ledsbase io@
;

: rgbbase ( -- c) \ push the base address of RGB leds
    $50
;
    
: rgbled! ( r g b n -- ) \ write the rgb value to the n-th rgb-led
    dup 2* + rgbbase +   \ calculate the base address of n-th rgb-led
    dup -rot             \ duplicate base address and save it for later use
    2 + io!              \ write blue 
    dup -rot             \ duplicate base address and save it for later use
    1 + io!              \ write green
    io!                  \ write red
;

: rgbled@ ( n -- r g b ) \ read the rgb value of the n-th rgb-led
    dup 2* + rgbbase +   \ calculate the base addresse of the n-th rgb-led
    dup io@              \ read the red value
    swap                 \ get the base address again
    dup 1 + io@          \ read the green value
    swap                 \ get the base address again
    2 + io@              \ read the blue value
;

: pmodAbase ( -- c) \ push the base address of the PMODA port
    $70
;


( c -- ) \ write to the directions register of PModA
: pmodADir!
    pmodAbase io!
;

( -- c) \ read the directions register of PModA
: pmodADir@
    pmodAbase io@
;

( c -- ) \ write value to PModA (read pins are ignored)
: pmodA!
    pmodAbase 4 + io!
;

( -- c) \ read the value of PModA
: pmodA@
    pmodAbase 4 + io@
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
