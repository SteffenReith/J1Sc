//
// Author: Steffen Reith (steffen.reith@hs-rm.de)
// Committer: Steffen Reith
//
// Creation Date:  Sat Apr 29 20:32:17 CEST 2017
// Module Name:    PLL
// Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
//

module PLL (clkIn,   
            clkOut,  
            isLocked);

   // Input ports
   input clkIn;

   // Output ports
   output clkOut;
   output isLocked;
   
   // Control signals
   wire   locked;              // The MMCM has achieved phase alignment
   wire   psDone_unused;       // Dummy signal for phase shift done
   wire   clkinstopped_unused; // Input clock has stopped (not used)
   wire   clkfbstopped_unused; // Feedback clock has stopped (not used)
   wire   drdy_unused;         // Reconfiguration ready signal

   // wire   do_unused : std_logic_vector(15 downto 0);  -- Reconfiguration data out

   // Internal clock signals
   wire   clkInI;       // Internal buffered input clock
   wire   clkI1;        // Internal output clock 1
   wire   clkOutI1;     // Internal already buffered output clock 1
   wire   clkDI_unused; // Internal delayed output clock

   // Feedback clock signals
   wire   clkfbI;    // Internal unbuffered feedback clock
   wire   clkfbIBuf; // Internal buffered feedback clock

   // Unused clock ports
   wire   clkfbb_unused; 
   wire   clk0b_unused;  
   wire   clk1b_unused;  
   wire   clk2_unused;   
   wire   clk2b_unused;  
   wire   clk3_unused;   
   wire   clk3b_unused;  
   wire   clk4_unused;   
   wire   clk5_unused;   
   wire   clk6_unused;   

  // Instantiate a input clock buffer
  IBUFG clkInBuffer (.O (clkInI),
		     .I  (clkIn));

  // Instantiate a clock buffer for the internal feedback signal
  BUFG feedbackBuffer (.O (clkfbIBuf),
		       .I (clkfbI));
 
  // Instantiate a clock manager
  MMCME2_ADV clkgen (
   #(.BANDWIDTH            ("OPTIMIZED"), // MMCM programming affecting jitter
     .CLKOUT4_CASCADE      ("FALSE"),     // Don't divide output more than 128
     .COMPENSATION         ("ZHOLD"),     // Clk input compensation for feedback
     .STARTUP_WAIT         ("FALSE"),     // Not supported yet (set to default)
     .DIVCLK_DIVIDE        (1),           // Division ratio for output clocks
     .CLKFBOUT_MULT_F      (8.000),       // Multiply feedback for 80Mhz
     .CLKFBOUT_PHASE       (0.000),       // phase of feedback output
     .CLKFBOUT_USE_FINE_PS ("FALSE"),     // Don't enable fine shift
     .CLKOUT0_DIVIDE_F     (10.000),      // Scale clock 0 to 1.0
     .CLKOUT0_PHASE        (0.000),       // Phase of clock 0 (no shift)
     .CLKOUT0_DUTY_CYCLE   (0.500),       // Duty cycle of clock 0
     .CLKOUT0_USE_FINE_PS  ("FALSE"),     // No fine shift for clock 0
     .CLKOUT1_DIVIDE_F     (10.000),      // Scale clock 1 to 1.0
     .CLKOUT1_PHASE        (0.000),       // Phase of clock 1 (no shift)
     .CLKOUT1_DUTY_CYCLE   (0.500),       // Duty cycle of clock 1
     .CLKOUT1_USE_FINE_PS  ("FALSE"),     // No fine shift for clock 1
     .CLKIN1_PERIOD        (10.0),        // 10ns input clock period -> 100Mhz
     .REF_JITTER1          (0.010))       // Set expected jitter to default
   mmcm_adv_inst
    (.CLKFBOUT  (clkfbI),
     .CLKFBOUTB (clkfbb_unused),       // Unused inverted feedback

     // Output clocks (delayed and non inverted)
     .CLKOUT0  (clkI1),
     .CLKOUT0B (clk0b_unused),
     .CLKOUT1  (clkDI_unused),
     .CLKOUT1B (clk1b_unused),

     // Unused clocks
     .CLKOUT2  (clk2_unused),
     .CLKOUT2B (clk2b_unused),
     .CLKOUT3  (clk3_unused),
     .CLKOUT3B (clk3b_unused),
     .CLKOUT4  (clk4_unused),
     .CLKOUT5  (clk5_unused),
     .CLKOUT6  (clk6_unused),

     // Input clock control
     .CLKFBIN  (clkfbIBuf),               // Buffered feedback signal
     .CLKIN1   (clkInI),                  // Input clock
     .CLKIN2   (1'b0),                    // Second input clock is not used
     .CLKINSEL (1'b1),                    // Select primary input clock 

     // Disable dynamic reconfiguration
     .DADDR (7'h0),                       // set all address bits to 0
     .DCLK  (1'b0),                       // No clock for the reconfig port
     .DEN   (1'b0),                       // Disable to reconfiguration port
     .DI    (16'h0),                      // set reconfiguration data to 0
     .DO    (do_unused),                  // Ignore MMCM reconfig data output
     .DRDY  (drdy_unused),                // Ignore the ready signal
     .DWE   (1'b0),                       // Disable the write enable

     // Don't implement dynamic phase shift
     .PSCLK    (1'b0),                    // No phase shift clock
     .PSEN     (1'b0),                    // Disable phase shift
     .PSINCDEC (1'b0),                    // No inc / dec of phase shift
     .PSDONE   (psDone_unused,            // Dummy signal for phase shift done

     // Other control and status signals
     .LOCKED       (locked,               // MMCE clock is stable
     .CLKINSTOPPED (clkinstopped_unused,  // Input clock has stopped (not used)
     .CLKFBSTOPPED (clkfbstopped_unused,  // Feedback clock has stopped (not used)
     .PWRDWN       (1'b0),                // Don't power down MMCE
     .RST          (1'b0));               // No reset after startup
		    
   // Synchron clock (not delayed) enable it when clock is stable
   BUFGCE clk1Buf (.O  (clkOutI1),
		   .CE (locked),
		   .I  (clkI1));
   clkOut = clkOutI1;

   // Provide the locked signal to the outside world
   isLocked = locked;
  
endmodule

