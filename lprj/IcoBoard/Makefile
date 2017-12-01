VERSRCPATH = ../../src/main/verilog/arch/IcoBoard
LATPATH = ../../src/main/lattice/IcoBoard
VERGENPATH = ../../gen/src/verilog
BUILDPATH = build

VERILOGSRC = $(VERGENPATH)/J1Ico.v $(VERSRCPATH)/PLL.v $(VERSRCPATH)/Board_IcoBoard.v  

TOPLEVEL = J1Sc


all: build/blinky.bin

build/blinky.bin: build/blinky.asc  
	icepack $< $@

$(BUILDPATH)/$(TOPLEVEL).asc: J1Sc.pcf $(BUILDPATH)/$(TOPLEVEL).blif  
	arachne-pnr -d 8k -p $(LATPATH)/J1Sc.pcf -o $(BUILDPATH)/$(TOPLEVEL).asc $(TOPLEVEL).blif 

$(BUILDPATH)/$(TOPLEVEL).blif: $(VERILOGSRC) 
	yosys -q J1Sc.ys 

prog: build/blinky.bin  
	iceprog build/blinky.bin

clean:  
	rm build/*

.PHONY: prog clean

