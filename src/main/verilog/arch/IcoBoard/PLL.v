//
// Author: Steffen Reith (steffen.reith@hs-rm.de)
// Committer: Steffen Reith
//
// Creation Date:  Mon Nov 20 10:37:12 CET 2017
// Module Name:    PLL for an IcoBoard (make 25 Mhz out of 100 Mhz)
// Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
//
module PLL(clkIn, clkOut, isLocked);

   // Input ports
   input clkIn;

   // Output ports
   output clkOut;
   output isLocked;

   // In: 100Mhz / Out: 35Mhz
   SB_PLL40_CORE #(
       .FEEDBACK_PATH("SIMPLE"),
       .PLLOUT_SELECT("GENCLK"),
       .DIVR(4'b0100),
       .DIVF(7'b0011011),
       .DIVQ(3'b100),
       .FILTER_RANGE(3'b010)
   ) uut (
       .LOCK(isLocked),
       .RESETB(1'b1),
       .BYPASS(1'b0),
       .REFERENCECLK(clkIn),
       .PLLOUTCORE(clkOut)
   );
endmodule

