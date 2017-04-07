set_property CFGBVS Vcco [current_design]
set_property CONFIG_VOLTAGE 3.3 [current_design]

set_property PACKAGE_PIN E3 [get_ports clk100Mhz]
set_property IOSTANDARD LVCMOS33 [get_ports clk100Mhz]

set_property PACKAGE_PIN N17 [get_ports reset]
set_property IOSTANDARD LVCMOS33 [get_ports reset]

set_property PACKAGE_PIN H17 [get_ports {leds[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[0]}]
set_property SLEW FAST [get_ports {leds[0]}]

set_property PACKAGE_PIN K15 [get_ports {leds[1]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[1]}]
set_property SLEW FAST [get_ports {leds[1]}]

set_property PACKAGE_PIN J13 [get_ports {leds[2]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[2]}]
set_property SLEW FAST [get_ports {leds[2]}]

set_property PACKAGE_PIN N14 [get_ports {leds[3]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[3]}]
set_property SLEW FAST [get_ports {leds[3]}]

set_property PACKAGE_PIN R18 [get_ports {leds[4]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[4]}]
set_property SLEW FAST [get_ports {leds[4]}]

set_property PACKAGE_PIN V17 [get_ports {leds[5]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[5]}]
set_property SLEW FAST [get_ports {leds[5]}]

set_property PACKAGE_PIN U17 [get_ports {leds[6]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[6]}]
set_property SLEW FAST [get_ports {leds[6]}]

set_property PACKAGE_PIN U16 [get_ports {leds[7]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[7]}]
set_property SLEW FAST [get_ports {leds[7]}]

set_property PACKAGE_PIN V16 [get_ports {leds[8]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[8]}]
set_property SLEW FAST [get_ports {leds[8]}]

set_property PACKAGE_PIN T15 [get_ports {leds[9]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[9]}]
set_property SLEW FAST [get_ports {leds[9]}]

set_property PACKAGE_PIN U14 [get_ports {leds[10]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[10]}]
set_property SLEW FAST [get_ports {leds[10]}]

set_property PACKAGE_PIN T16 [get_ports {leds[11]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[11]}]
set_property SLEW FAST [get_ports {leds[11]}]

set_property PACKAGE_PIN V15 [get_ports {leds[12]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[12]}]
set_property SLEW FAST [get_ports {leds[12]}]

set_property PACKAGE_PIN V14 [get_ports {leds[13]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[13]}]
set_property SLEW FAST [get_ports {leds[13]}]

set_property PACKAGE_PIN V12 [get_ports {leds[14]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[14]}]
set_property SLEW FAST [get_ports {leds[14]}]

set_property PACKAGE_PIN V11 [get_ports {leds[15]}]
set_property IOSTANDARD LVCMOS33 [get_ports {leds[15]}]
set_property SLEW FAST [get_ports {leds[15]}]

set_property PACKAGE_PIN C4 [get_ports rx]
set_property IOSTANDARD LVCMOS33 [get_ports rx]

set_property PACKAGE_PIN D4 [get_ports tx]
set_property IOSTANDARD LVCMOS33 [get_ports tx]

set_property PACKAGE_PIN P17 [get_ports {extInt[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {extInt[0]}]

##Pmod Header JA

set_property PACKAGE_PIN C17 [get_ports {pmodA[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[0]}]

set_property PACKAGE_PIN D18 [get_ports {pmodA[1]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[1]}]

set_property PACKAGE_PIN E18 [get_ports {pmodA[2]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[2]}]

set_property PACKAGE_PIN G17 [get_ports {pmodA[3]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[3]}]

set_property PACKAGE_PIN D17 [get_ports {pmodA[4]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[4]}]

set_property PACKAGE_PIN E17 [get_ports {pmodA[5]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[5]}]

set_property PACKAGE_PIN F18 [get_ports {pmodA[6]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[6]}]

set_property PACKAGE_PIN G18 [get_ports {pmodA[7]}]
set_property IOSTANDARD LVCMOS33 [get_ports {pmodA[7]}]

#set_property  PACKAGE_PIN U13 [get_ports {sel[7]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[7]}]
#set_property  SLEW FAST [get_ports {sel[7]}]

#set_property  PACKAGE_PIN K2 [get_ports {sel[6]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[6]}]
#set_property  SLEW FAST [get_ports {sel[6]}]

#set_property  PACKAGE_PIN T14 [get_ports {sel[5]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[5]}]
#set_property  SLEW FAST [get_ports {sel[5]}]

#set_property  PACKAGE_PIN P14 [get_ports {sel[4]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[4]}]
#set_property  SLEW FAST [get_ports {sel[4]}]

#set_property  PACKAGE_PIN J14 [get_ports {sel[3]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[3]}]
#set_property  SLEW FAST [get_ports {sel[3]}]

#set_property  PACKAGE_PIN T9 [get_ports {sel[2]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[2]}]
#set_property  SLEW FAST [get_ports {sel[2]}]

#set_property  PACKAGE_PIN J18 [get_ports {sel[1]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[1]}]
#set_property  SLEW FAST [get_ports {sel[1]}]

#set_property  PACKAGE_PIN J17 [get_ports {sel[0]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {sel[0]}]
#set_property  SLEW FAST [get_ports {sel[0]}]


#set_property  PACKAGE_PIN H15 [get_ports {segs[7]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[7]}]
#set_property  SLEW FAST [get_ports {segs[7]}]

#set_property  PACKAGE_PIN T10 [get_ports {segs[0]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[0]}]
#set_property  SLEW FAST [get_ports {segs[0]}]

#set_property  PACKAGE_PIN R10 [get_ports {segs[1]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[1]}]
#set_property  SLEW FAST [get_ports {segs[1]}]

#set_property  PACKAGE_PIN K16 [get_ports {segs[2]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[2]}]
#set_property  SLEW FAST [get_ports {segs[2]}]

#set_property  PACKAGE_PIN K13 [get_ports {segs[3]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[3]}]
#set_property  SLEW FAST [get_ports {segs[3]}]

#set_property  PACKAGE_PIN P15 [get_ports {segs[4]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[4]}]
#set_property  SLEW FAST [get_ports {segs[4]}]

#set_property  PACKAGE_PIN T11 [get_ports {segs[5]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[5]}]
#set_property  SLEW FAST [get_ports {segs[5]}]

#set_property  PACKAGE_PIN L18 [get_ports {segs[6]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {segs[6]}]
#set_property  SLEW FAST [get_ports {segs[6]}]


#set_property  PACKAGE_PIN J15 [get_ports {btn[0]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[0]}]
#set_property  SLEW FAST [get_ports {btn[0]}]

#set_property  PACKAGE_PIN L16 [get_ports {btn[1]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[1]}]
#set_property  SLEW FAST [get_ports {btn[1]}]

#set_property  PACKAGE_PIN M13 [get_ports {btn[2]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[2]}]
#set_property  SLEW FAST [get_ports {btn[2]}]

#set_property  PACKAGE_PIN R15 [get_ports {btn[3]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[3]}]
#set_property  SLEW FAST [get_ports {btn[3]}]

#set_property  PACKAGE_PIN R17 [get_ports {btn[4]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[4]}]
#set_property  SLEW FAST [get_ports {btn[4]}]

#set_property  PACKAGE_PIN T18 [get_ports {btn[5]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[5]}]
#set_property  SLEW FAST [get_ports {btn[5]}]

#set_property  PACKAGE_PIN U18 [get_ports {btn[6]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[6]}]
#set_property  SLEW FAST [get_ports {btn[6]}]

#set_property  PACKAGE_PIN R13 [get_ports {btn[7]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[7]}]
#set_property  SLEW FAST [get_ports {btn[7]}]

#set_property  PACKAGE_PIN T8 [get_ports {btn[8]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[8]}]
#set_property  SLEW FAST [get_ports {btn[8]}]

#set_property  PACKAGE_PIN U8 [get_ports {btn[9]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[9]}]
#set_property  SLEW FAST [get_ports {btn[9]}]

#set_property  PACKAGE_PIN R16 [get_ports {btn[10]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[10]}]
#set_property  SLEW FAST [get_ports {btn[10]}]

#set_property  PACKAGE_PIN T13 [get_ports {btn[11]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[11]}]
#set_property  SLEW FAST [get_ports {btn[11]}]

#set_property  PACKAGE_PIN H6 [get_ports {btn[12]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[12]}]
#set_property  SLEW FAST [get_ports {btn[12]}]

#set_property  PACKAGE_PIN U12 [get_ports {btn[13]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[13]}]
#set_property  SLEW FAST [get_ports {btn[13]}]

#set_property  PACKAGE_PIN U11 [get_ports {btn[14]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[14]}]
#set_property  SLEW FAST [get_ports {btn[14]}]

#set_property  PACKAGE_PIN V10 [get_ports {btn[15]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[15]}]
#set_property  SLEW FAST [get_ports {btn[15]}]

#set_property  PACKAGE_PIN M18 [get_ports {btn[16]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[16]}]
#set_property  SLEW FAST [get_ports {btn[16]}]

#set_property  PACKAGE_PIN P17 [get_ports {btn[17]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[17]}]
#set_property  SLEW FAST [get_ports {btn[17]}]

#set_property  PACKAGE_PIN P18 [get_ports {btn[18]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[18]}]
#set_property  SLEW FAST [get_ports {btn[18]}]

#set_property  PACKAGE_PIN M17 [get_ports {btn[19]}]
#set_property  IOSTANDARD LVCMOS33 [get_ports {btn[19]}]
#set_property  SLEW FAST [get_ports {btn[19]}]
