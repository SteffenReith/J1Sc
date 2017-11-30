VERSRCPATH = ../../src/main/verilog/arch/IcoBoard
VERGENPATH = ../../gen/src/verilog
BUILDPATH = build

VERILOGSRC = $(VERGENPATH)/J1Ico.v $(VERSRCPATH)/PLL.v $(VERSRCPATH)/Board_IcoBoard.v  

TOPLEVEL = J1Sc


all: build/blinky.bin

build/blinky.bin: build/blinky.asc  
	icepack $< $@

build/blinky.asc: blinky.pcf build/blinky.blif  
	arachne-pnr -d 1k -P tq144 -o $@ -p $^

$(BUILDPATH)/$(TOPLEVEL).blif: $(VERILOGSRC) 
	yosys -q J1Sc.ys 

prog: build/blinky.bin  
	iceprog build/blinky.bin

clean:  
	rm build/*

.PHONY: prog clean

