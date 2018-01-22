VERSRCPATH = ../../src/main/verilog/arch/IcoBoard
LATPATH = ../../src/main/lattice/IcoBoard
YOPATH =  ../../../src/main/lattice/IcoBoard
VERGENPATH = ../../gen/src/verilog
BUILDPATH = build

VERILOGGENSRC = $(VERGENPATH)/J1Ico.v  
VERILOGSRC = $(VERILOGGENSRC) $(VERSRCPATH)/PLL.v $(VERSRCPATH)/Board_IcoBoard.v

TOPLEVEL = J1Ico

export ROOT_DIR=${PWD}

all: $(BUILDPATH)/$(TOPLEVEL).bin

$(BUILDPATH)/$(TOPLEVEL).bin: $(BUILDPATH)/$(TOPLEVEL).asc  
	icepack $(BUILDPATH)/$(TOPLEVEL).asc $(BUILDPATH)/$(TOPLEVEL).bin
	icetime -d hx8k -c 40 $(BUILDPATH)/J1Ico.asc

$(BUILDPATH)/$(TOPLEVEL).asc: $(LATPATH)/J1Sc.pcf $(BUILDPATH)/$(TOPLEVEL).blif  
	arachne-pnr -m 400 -d 8k -p $(LATPATH)/J1Sc.pcf -o $(BUILDPATH)/$(TOPLEVEL).asc $(BUILDPATH)/$(TOPLEVEL).blif 

fixit:
	sed -i -E "s/(ramList_[0-9]\[.+\])( = )/\1 <= /" $(VERILOGGENSRC)

$(BUILDPATH)/$(TOPLEVEL).blif: $(VERILOGSRC)
	mkdir -p $(BUILDPATH)
	cd $(VERGENPATH) && yosys -q $(YOPATH)/J1Sc.ys

time: $(BUILDPATH)/$(TOPLEVEL).asc
	icetime -d hx8k $(BUILDPATH)/J1Ico.asc

prog: $(BUILDPATH)/$(TOPLEVEL).bin 
	icoprog -p < $(BUILDPATH)/$(TOPLEVEL).bin

clean:  
	rm -f $(BUILDPATH)/*

.PHONY: prog clean time fixit

