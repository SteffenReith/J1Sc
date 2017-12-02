VERSRCPATH = ../../src/main/verilog/arch/IcoBoard
LATPATH = ../../src/main/lattice/IcoBoard
VERGENPATH = ../../gen/src/verilog
BUILDPATH = build

VERILOGSRC = $(VERGENPATH)/J1Ico.v $(VERSRCPATH)/PLL.v $(VERSRCPATH)/Board_IcoBoard.v  

TOPLEVEL = J1Ico


all: $(BUILDPATH)/$(TOPLEVEL).bin

$(BUILDPATH)/$(TOPLEVEL).bin: $(BUILDPATH)/$(TOPLEVEL).asc  
	icepack $(BUILDPATH)/$(TOPLEVEL).asc $(BUILDPATH)/$(TOPLEVEL).bin
	icetime -d hx8k -c 35 $(BUILDPATH)/J1Ico.asc

$(BUILDPATH)/$(TOPLEVEL).asc: $(LATPATH)/J1Sc.pcf $(BUILDPATH)/$(TOPLEVEL).blif  
	arachne-pnr -d 8k -p $(LATPATH)/J1Sc.pcf -o $(BUILDPATH)/$(TOPLEVEL).asc $(BUILDPATH)/$(TOPLEVEL).blif 

$(BUILDPATH)/$(TOPLEVEL).blif: $(VERILOGSRC)
	mkdir -p $(BUILDPATH) 
	yosys -q J1Sc.ys

time: $(BUILDPATH)/$(TOPLEVEL).asc
	icetime -d hx8k $(BUILDPATH)/J1Ico.asc

prog: $(BUILDPATH)/$(TOPLEVEL).bin 
	iceprog $(BUILDPATH)/$(TOPLEVEL).bin

clean:  
	rm -f $(BUILDPATH)/*

.PHONY: prog clean time
