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

: rgbled@ ( n -- r g b) \ read the rgb value of the n-th rgb-led
    dup 2* + rgbbase +  \ calculate the base addresse of the n-th rgb-led
    dup io@             \ read the red value
    swap                \ get the base address again
    dup 1 + io@         \ read the green value
    swap                \ get the base address again
    2 + io@             \ read the blue value
;

: ssdbase ( -- c) \ push the base address for the seven segment displays
    $60
;

: ssd@ ( n -- v) \ read the value of the nth display
    ssdbase +    \ calulate the address of the nth display
    io@          \ push the value
;

: ssd! ( v n -- ) \ write the value v to the n-th display
    ssdbase +     \ calculate address of n-th display
    io!           \ write the value
;

: ssdMask@ ( -- v) \ read the current ssd mask
    ssdbase 8 +    \ get address of ssd mask register
    io@            \ push the value
;

: ssdMask! ( v -- ) \ write value to the ssd mask register
    ssdbase 8 +     \ get address of ssd mask register
    io!             \ write the value
;

: setDot ( n -- ) \ set dot of n-th display
    dup           \ save display number
    ssd@          \ get value of display
    $8000 or      \ set the dot
    swap          \ get display number on stack top
    ssd!          \ write the new value
;

: clearDot ( n -- ) \ clear dot of n-th display
    dup             \ save display number
    ssd@            \ get value of display
    $7fff and       \ clear the dot
    swap            \ get display number on stack top
    ssd!            \ write the new value
;

: pmodAbase ( -- c) \ push the base address of port A
    $70
;

: pmodADir! ( c -- ) \ write to the directions register of port A
    pmodAbase io!
;

: pmodADir@ ( -- c) \ read from the directions register of port A
    pmodAbase io@
;

: pmodA! ( c -- ) \ write value to port A
    pmodAbase 4 + io!
;

: pmodA@ ( -- c) \ read the value from port A
    pmodAbase 4 + io@
;

: sSwitchBase ( -- c) \ push the base address of slider switch array
    $80
;

: sSw@ ( -- c) \ read the debounced value from the slider switch array
    sSwitchBase io@
;

: pBtnBase ( -- c ) \ push the base address of push button array
    $90
;

: pBtn@ ( -- c) \ read the debounced value from the push button array
    pBtnBase io@
;

: tABase ( -- c ) \ push the base address of timer A
    $c0
;

: ltA! ( c -- ) \ write the value on tos to the low part of timer A
    tABase 0 + io!
;

: htA! ( c -- ) \ write the value on tos to the high part of timer A
    tABase 1 + io!
;

: entA ( -- ) \ enable timer A
    1 tABase 2 + io!
;

: distA ( -- ) \ disable timer A
    0 tABase 2 + io!
;

: iBase ( -- c ) \ push the interrupt controller base address
    $e0
;

: ivec! ( a c -- ) \ write address a to interrupt vector c
    iBase + io!
;

: imask! ( c -- ) \ write interrupt mask
    iBase 4 + io!
;

: imask@ ( -- c) \ get interrupt mask
    iBase 4 + io@
;
