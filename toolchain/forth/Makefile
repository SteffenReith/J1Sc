build/nuc.hex: cross.fs basewords.fs nuc.fs
	gforth cross.fs basewords.fs nuc.fs

build/nuc.binary: cross.fs basewords.fs nuc.fs
	gforth cross.fs basewords.fs nuc.fs

all: build/nuc.binary 

clean: 
	rm build/nuc.hex build/nuc.binary build/nuc.lst
