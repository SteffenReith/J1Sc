\ \
\
\ Author: Steffen Reith (Steffen.Reith@hs-rm.de)
\ 
\ Creation Date:  Sat Sep 9 16:21:25 GMT+2 2017 
\ Module Name:    irqdemo - A simple demo of the interruptsystem of J1Sc
\ Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL 
\ 
\ \

: blink leds@ invert leds! ;

: rotate16
    leds@
    dup
    1 and if
	2/
	$7fff and
	$8000 or
    else
	2/
	$7fff and
    then
    leds!
;

: rotate8
    leds@
    dup
    1 and if
	2/
	$7f and
	$80 or
    else
	2/ $7f and
    then
    leds!
;

: isr rotate8 ;

: iDemo \ Simple interrupt demo
    \ Init leds
    $80 leds!
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
