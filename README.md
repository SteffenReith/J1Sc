# J1Sc - A simple reimplementation of the [J1 CPU](http://www.excamera.com/sphinx/fpga-j1.html) in Scala using Spinal HDL

## How to build J1Sc

1. Clone the latest version of SpinalHDL 
   (see https://github.com/SpinalHDL/SpinalHDL)

2. Setup Spinal HDL
   (see http://spinalhdl.github.io/SpinalDoc/spinal_getting_started/)

3. Change directory to the new clone of SpinalHDL and run
   `sbt publish-local` inside

4. Change directory to J1Sc

5. Install gforth (e.g. `sudo apt-get install gforth`)

6. Run `cd toolchain/forth && make && cd ../..` to build the forth core
   system

7. Build J1Sc (either using the VHDL or the Verilog version) by "sbt run". The
   generated files can be found in "gen/src/vhdl/J1SoC.vhd" and
   "gen/src/verilog/J1SoC.v". You need "Board_<BOARDNAME>.vhd" and "PLL.vhd" in 
   "src/main/vhdl/arch" or the corresponding Verilog versions in "src/main/verilog/arch" 
   as toplevel for synthesis.
   
   A Xilinx Vivado project file "J1Sc.xpr" for the VHDL version can be
   found in "vprj/vhdl/J1Sc" and the Verilog version is in "vprj/verilog/J1Sc".
   Note that J1Sc runs fine with a 100Mhz Clock on a Nexys4 DDR from
   Digilent. Constraint files for the Nexys4 DDR can be found in
   "/src/main/xilinx/nexys4ddr" the corresponding files for the Nexys4 can be found
   in "/src/main/xilinx/nexys4".

8. Build J1Sc (see "gen/src/vhdl" or "gen/src/verilog") and send the .bit
   file to your FPGA/board (use either
   "src/main/vhdl/arch/Nexys4DDR/BoardNexys4DDR.vhd" or 
   "src/main/verilog/arch/Nexys4DDR/BoardNexys4DDR.v" as toplevel module)

9. `cd toolchain/forth`

10. Become root and run "bin/confs" (note that you have to set the serial
   port appropriately)

11. Press the reset button (default is BTNC on the Nexys4 DDR). You should
    see something like:
    `Contacting... established`
    `Loaded 142 words`

12. Type `#include SwapForth.fs` to load the complete FORTH system

13. Turn the leds on by `hex ffff leds!`

14. Have fun with a working forth system
