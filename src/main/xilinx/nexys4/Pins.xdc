set_property CFGBVS Vcco [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]

set_property PACKAGE_PIN E3 [get_ports clk100Mhz]
set_property IOSTANDARD LVCMOS33 [get_ports clk100Mhz]

set_property PACKAGE_PIN E16 [get_ports reset]
set_property IOSTANDARD LVCMOS33 [get_ports reset]

set_property  PACKAGE_PIN T8 [get_ports {leds[0]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[0]}]
set_property  SLEW FAST [get_ports {leds[0]}]

set_property  PACKAGE_PIN V9 [get_ports {leds[1]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[1]}]
set_property  SLEW FAST [get_ports {leds[1]}]

set_property  PACKAGE_PIN R8 [get_ports {leds[2]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[2]}]
set_property  SLEW FAST [get_ports {leds[2]}]

set_property  PACKAGE_PIN T6 [get_ports {leds[3]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[3]}]
set_property  SLEW FAST [get_ports {leds[3]}]

set_property  PACKAGE_PIN T5 [get_ports {leds[4]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[4]}]
set_property  SLEW FAST [get_ports {leds[4]}]

set_property  PACKAGE_PIN T4 [get_ports {leds[5]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[5]}]
set_property  SLEW FAST [get_ports {leds[5]}]

set_property  PACKAGE_PIN U7 [get_ports {leds[6]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[6]}]
set_property  SLEW FAST [get_ports {leds[6]}]

set_property  PACKAGE_PIN U6 [get_ports {leds[7]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[7]}]
set_property  SLEW FAST [get_ports {leds[7]}]

set_property  PACKAGE_PIN V4 [get_ports {leds[8]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[8]}]
set_property  SLEW FAST [get_ports {leds[8]}]

set_property  PACKAGE_PIN U3 [get_ports {leds[9]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[9]}]
set_property  SLEW FAST [get_ports {leds[9]}]

set_property  PACKAGE_PIN V1 [get_ports {leds[10]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[10]}]
set_property  SLEW FAST [get_ports {leds[10]}]

set_property  PACKAGE_PIN R1 [get_ports {leds[11]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[11]}]
set_property  SLEW FAST [get_ports {leds[11]}]

set_property  PACKAGE_PIN P5 [get_ports {leds[12]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[12]}]
set_property  SLEW FAST [get_ports {leds[12]}]

set_property  PACKAGE_PIN U1 [get_ports {leds[13]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[13]}]
set_property  SLEW FAST [get_ports {leds[13]}]

set_property  PACKAGE_PIN R2 [get_ports {leds[14]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[14]}]
set_property  SLEW FAST [get_ports {leds[14]}]

set_property  PACKAGE_PIN P2 [get_ports {leds[15]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {leds[15]}]
set_property  SLEW FAST [get_ports {leds[15]}]

set_property  PACKAGE_PIN K6 [get_ports {rgbLeds[0]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {rgbLeds[0]}]
set_property  SLEW FAST [get_ports {rgbLeds[0]}]

set_property  PACKAGE_PIN H6 [get_ports {rgbLeds[1]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {rgbLeds[1]}]
set_property  SLEW FAST [get_ports {rgbLeds[1]}]

set_property  PACKAGE_PIN L16 [get_ports {rgbLeds[2]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {rgbLeds[2]}]
set_property  SLEW FAST [get_ports {rgbLeds[2]}]

set_property  PACKAGE_PIN K5 [get_ports {rgbLeds[3]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {rgbLeds[3]}]
set_property  SLEW FAST [get_ports {rgbLeds[3]}]

set_property  PACKAGE_PIN F13 [get_ports {rgbLeds[4]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {rgbLeds[4]}]
set_property  SLEW FAST [get_ports {rgbLeds[4]}]

set_property  PACKAGE_PIN F6 [get_ports {rgbLeds[5]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {rgbLeds[5]}]
set_property  SLEW FAST [get_ports {rgbLeds[5]}]

set_property  PACKAGE_PIN C4 [get_ports {rx}]
set_property  IOSTANDARD LVCMOS33 [get_ports {rx}]

set_property  PACKAGE_PIN D4 [get_ports {tx}]
set_property  IOSTANDARD LVCMOS33 [get_ports {tx}]

set_property  PACKAGE_PIN F15 [get_ports {extInt[0]}]
set_property  IOSTANDARD LVCMOS33 [get_ports {extInt[0]}]

##Pmod Header JA

set_property PACKAGE_PIN B13 [get_ports {pmodA[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[0]}]

set_property PACKAGE_PIN F14 [get_ports {pmodA[1]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[1]}]

set_property PACKAGE_PIN D17 [get_ports {pmodA[2]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[2]}]

set_property PACKAGE_PIN E17 [get_ports {pmodA[3]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[3]}]

set_property PACKAGE_PIN G13 [get_ports {pmodA[4]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[4]}]

set_property PACKAGE_PIN C17 [get_ports {pmodA[5]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[5]}]

set_property PACKAGE_PIN D18 [get_ports {pmodA[6]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[6]}]

set_property PACKAGE_PIN E18 [get_ports {pmodA[7]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[7]}]


#set_property  PACKAGE_PIN M1 [get_ports {sel[7]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[7]}]
#set_property  SLEW FAST [get_ports {sel[7]}]

#set_property  PACKAGE_PIN L1 [get_ports {sel[6]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[6]}]
#set_property  SLEW FAST [get_ports {sel[6]}]

#set_property  PACKAGE_PIN N4 [get_ports {sel[5]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[5]}]
#set_property  SLEW FAST [get_ports {sel[5]}]

#set_property  PACKAGE_PIN N2 [get_ports {sel[4]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[4]}]
#set_property  SLEW FAST [get_ports {sel[4]}]

#set_property  PACKAGE_PIN N5 [get_ports {sel[3]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[3]}]
#set_property  SLEW FAST [get_ports {sel[3]}]

#set_property  PACKAGE_PIN M3 [get_ports {sel[2]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[2]}]
#set_property  SLEW FAST [get_ports {sel[2]}]

#set_property  PACKAGE_PIN M6 [get_ports {sel[1]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[1]}]
#set_property  SLEW FAST [get_ports {sel[1]}]

#set_property  PACKAGE_PIN N6 [get_ports {sel[0]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[0]}]
#set_property  SLEW FAST [get_ports {sel[0]}]


#set_property  PACKAGE_PIN M4 [get_ports {segs[7]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[7]}]
#set_property  SLEW FAST [get_ports {segs[7]}]

#set_property  PACKAGE_PIN L3 [get_ports {segs[0]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[0]}]
#set_property  SLEW FAST [get_ports {segs[0]}]

#set_property  PACKAGE_PIN N1 [get_ports {segs[1]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[1]}]
#set_property  SLEW FAST [get_ports {segs[1]}]

#set_property  PACKAGE_PIN L5 [get_ports {segs[2]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[2]}]
#set_property  SLEW FAST [get_ports {segs[2]}]

#set_property  PACKAGE_PIN L4 [get_ports {segs[3]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[3]}]
#set_property  SLEW FAST [get_ports {segs[3]}]

#set_property  PACKAGE_PIN K3 [get_ports {segs[4]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[4]}]
#set_property  SLEW FAST [get_ports {segs[4]}]

#set_property  PACKAGE_PIN M2 [get_ports {segs[5]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[5]}]
#set_property  SLEW FAST [get_ports {segs[5]}]

#set_property  PACKAGE_PIN L6 [get_ports {segs[6]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[6]}]
#set_property  SLEW FAST [get_ports {segs[6]}]


#set_property  PACKAGE_PIN U9 [get_ports {btn[0]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[0]}]
#set_property  SLEW FAST [get_ports {btn[0]}]

#set_property  PACKAGE_PIN U8 [get_ports {btn[1]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[1]}]
#set_property  SLEW FAST [get_ports {btn[1]}]

#set_property  PACKAGE_PIN R7 [get_ports {btn[2]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[2]}]
#set_property  SLEW FAST [get_ports {btn[2]}]

#set_property  PACKAGE_PIN R6 [get_ports {btn[3]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[3]}]
#set_property  SLEW FAST [get_ports {btn[3]}]

#set_property  PACKAGE_PIN R5 [get_ports {btn[4]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[4]}]
#set_property  SLEW FAST [get_ports {btn[4]}]

#set_property  PACKAGE_PIN V7 [get_ports {btn[5]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[5]}]
#set_property  SLEW FAST [get_ports {btn[5]}]

#set_property  PACKAGE_PIN V6 [get_ports {btn[6]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[6]}]
#set_property  SLEW FAST [get_ports {btn[6]}]

#set_property  PACKAGE_PIN V5 [get_ports {btn[7]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[7]}]
#set_property  SLEW FAST [get_ports {btn[7]}]

#set_property  PACKAGE_PIN U4 [get_ports {btn[8]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[8]}]
#set_property  SLEW FAST [get_ports {btn[8]}]

#set_property  PACKAGE_PIN V2 [get_ports {btn[9]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[9]}]
#set_property  SLEW FAST [get_ports {btn[9]}]

#set_property  PACKAGE_PIN U2 [get_ports {btn[10]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[10]}]
#set_property  SLEW FAST [get_ports {btn[10]}]

#set_property  PACKAGE_PIN T3 [get_ports {btn[11]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[11]}]
#set_property  SLEW FAST [get_ports {btn[11]}]

#set_property  PACKAGE_PIN T1 [get_ports {btn[12]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[12]}]
#set_property  SLEW FAST [get_ports {btn[12]}]

#set_property  PACKAGE_PIN R3 [get_ports {btn[13]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[13]}]
#set_property  SLEW FAST [get_ports {btn[13]}]

#set_property  PACKAGE_PIN P3 [get_ports {btn[14]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[14]}]
#set_property  SLEW FAST [get_ports {btn[14]}]

#set_property  PACKAGE_PIN P4 [get_ports {btn[15]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[15]}]
#set_property  SLEW FAST [get_ports {btn[15]}]

#set_property  PACKAGE_PIN F15 [get_ports {btn[16]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[16]}]
#set_property  SLEW FAST [get_ports {btn[16]}]

#set_property  PACKAGE_PIN T16 [get_ports {btn[17]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[17]}]
#set_property  SLEW FAST [get_ports {btn[17]}]

#set_property  PACKAGE_PIN R10 [get_ports {btn[18]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[18]}]
#set_property  SLEW FAST [get_ports {btn[18]}]

#set_property  PACKAGE_PIN V10 [get_ports {btn[19]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[19]}]
#set_property  SLEW FAST [get_ports {btn[19]}]

