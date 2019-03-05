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
   SB_PLL40_PAD #(
       .FEEDBACK_PATH("SIMPLE"),
       .DIVR(4'b0000),         // DIVR =  0
       .DIVF(7'b0110111),      // DIVF = 55
       .DIVQ(3'b100),          // DIVQ =  4
       .FILTER_RANGE(3'b001)   // FILTER_RANGE = 1
   ) global_pll_inst (
       .LOCK(isLocked),
       .RESETB(1'b1),
       .BYPASS(1'b0),
       .PACKAGEPIN(clkIn),
       .PLLOUTGLOBAL(clkOut)
   );
endmodule
