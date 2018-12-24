all: nucleus
	mv build/nuc.binary build/16bit/nuc.binary
	mv build/nuc.hex    build/16bit/nuc.hex
	mv build/nuc.lst    build/16bit/nuc.lst

nucleus: cross.fs basewords.fs nuc.fs
	gforth cross.fs basewords.fs nuc.fs

clean: 
	rm build/16bit/nuc.hex build/16bit/nuc.binary build/16bit/nuc.lst
