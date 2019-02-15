//
// Author: Steffen Reith (steffen.reith@hs-rm.de)
// Committer: Steffen Reith
//
// Creation Date:  Fri Feb 15 21:00:38 CET 2019 
// Module Name:    PLL for an IceBreaker board (make 42 Mhz out of 12 Mhz)
// Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
//
module PLL(clkIn, clkOut, isLocked);

   // Input ports
   input clkIn;

   // Output ports
   output clkOut;
   output isLocked;

   // In: 12Mhz / Out: 42Mhz
   SB_PLL40_CORE #(
       .FEEDBACK_PATH("SIMPLE"),
       .PLLOUT_SELECT("GENCLK"),
       .DIVR(4'b0000),
       .DIVF(7'b0110111),
       .DIVQ(3'b100),
       .FILTER_RANGE(3'b001)
   ) uut (
       .LOCK(isLocked),
       .RESETB(1'b1),
       .BYPASS(1'b0),
       .REFERENCECLK(clkIn),
       .PLLOUTCORE(clkOut)
   );
endmodule

