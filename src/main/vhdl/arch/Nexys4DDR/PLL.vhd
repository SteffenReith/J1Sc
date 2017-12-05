--------------------------------------------------------------------------------
--
-- Creation Date:  Tue Jan 17 19:29:25 GMT+1 2017
-- Creator:        Steffen Reith
-- Module Name:    PLL - Structural
-- Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
--
--------------------------------------------------------------------------------
library ieee;
use ieee.std_logic_1164.all;

library unisim;
use unisim.vcomponents.all;

entity PLL is

  port (clkIn    : in  std_logic;
        clkOut   : out std_logic;
        isLocked : out std_logic);

end PLL;

architecture Structural of PLL is

  -- Control signals
  signal locked              : std_logic;  -- The MMCM has achieved phase alignment
  signal psDone_unused       : std_logic;  -- Dummy signal for phase shift done
  signal clkinstopped_unused : std_logic;  -- Input clock has stopped (not used)
  signal clkfbstopped_unused : std_logic;  -- Feedback clock has stopped (not used)
  signal drdy_unused         : std_logic;  -- Reconfiguration ready signal

  signal do_unused : std_logic_vector(15 downto 0);  -- Reconfiguration data out

  -- Internal clock signals
  signal clkInI       : std_logic;      -- Internal buffered input clock
  signal clkI1        : std_logic;      -- Internal output clock 1
  signal clkOutI1     : std_logic;      -- Internal already buffered output clock 1
  signal clkDI_unused : std_logic;      -- Internal delayed output clock

  -- Feedback clock signals
  signal clkfbI    : std_logic;         -- Internal unbuffered feedback clock
  signal clkfbIBuf : std_logic;         -- Internal buffered feedback clock

  -- Unused clock ports
  signal clkfbb_unused : std_logic;
  signal clk0b_unused  : std_logic;
  signal clk1b_unused  : std_logic;
  signal clk2_unused   : std_logic;
  signal clk2b_unused  : std_logic;
  signal clk3_unused   : std_logic;
  signal clk3b_unused  : std_logic;
  signal clk4_unused   : std_logic;
  signal clk5_unused   : std_logic;
  signal clk6_unused   : std_logic;

begin

  -- Instantiate a input clock buffer
  clkInBuffer : IBUFG
    port map (O => clkInI,
              I => clkIn);

  -- Instantiate a clock buffer for the internal feedback signal
  feedbackBuffer : BUFG
    port map (O => clkfbIBuf,
              I => clkfbI);

  -- Instantiate a clock manager
  clkgen : MMCME2_ADV

    generic map (
      BANDWIDTH => "OPTIMIZED",         -- MMCM programming affecting jitter

      CLKOUT4_CASCADE => false,         -- don't divide output more than 128

      COMPENSATION => "ZHOLD",          -- Clk input compensation for feedback 

      STARTUP_WAIT => false,            -- not supported yet (set to default)

      DIVCLK_DIVIDE => 1,               -- Division ratio for output clocks

      CLKFBOUT_MULT_F      => 10.000,   -- set feedback base
      CLKFBOUT_PHASE       => 0.000,    -- phase of feedback output
      CLKFBOUT_USE_FINE_PS => false,    -- Don't enable fine shift

      --CLKOUT0_DIVIDE_F    => 12.500,    -- Scale clock to 80Mhz
      CLKOUT0_DIVIDE_F    => 10.000,    -- Scale to 100Mhz
      --CLKOUT0_DIVIDE_F    => 8.333,     -- Scale to 120Mhz
      --CLKOUT0_DIVIDE_F    => 8.000,     -- Scale to 125Mhz
      CLKOUT0_PHASE       => 0.000,     -- Phase of clock 0 (no shift)
      CLKOUT0_DUTY_CYCLE  => 0.500,     -- Duty cycle of clock 0
      CLKOUT0_USE_FINE_PS => false,     -- No fine shift for clock 0

      CLKOUT1_DIVIDE      => 10,        -- Scale clock 1 to 1.0
      CLKOUT1_PHASE       => 270.000,   -- Phase of clock 1 (delayed)
      CLKOUT1_DUTY_CYCLE  => 0.500,     -- Duty cycle of clock 1
      CLKOUT1_USE_FINE_PS => false,     -- No fine shift for clock 1

      CLKIN1_PERIOD => 10.000,          -- 10ns input clock period -> 100Mhz

      REF_JITTER1 => 0.010)             -- Set expected jitter to default

    port map (
      CLKFBOUT  => clkfbI,
      CLKFBOUTB => clkfbb_unused,       -- Unused inverted feedback

      -- Output clocks (delayed and non inverted)
      CLKOUT0  => clkI1,
      CLKOUT0B => clk0b_unused,
      CLKOUT1  => clkDI_unused,
      CLKOUT1B => clk1b_unused,

      -- Unused clocks
      CLKOUT2  => clk2_unused,
      CLKOUT2B => clk2b_unused,
      CLKOUT3  => clk3_unused,
      CLKOUT3B => clk3b_unused,
      CLKOUT4  => clk4_unused,
      CLKOUT5  => clk5_unused,
      CLKOUT6  => clk6_unused,

      -- Input clock control
      CLKFBIN  => clkfbIBuf,            -- Buffered feedback signal
      CLKIN1   => clkInI,               -- Input clock
      CLKIN2   => '0',                  -- Second input clock is not used
      CLKINSEL => '1',                  -- Select primary input clock 

      -- Disable dynamic reconfiguration
      DADDR => (others => '0'),         -- set all address bits to 0
      DCLK  => '0',                     -- No clock for the reconfig port
      DEN   => '0',                     -- Disable to reconfiguration port
      DI    => (others => '0'),         -- set reconfiguration data to 0
      DO    => do_unused,               -- Ignore MMCM reconfig data output
      DRDY  => drdy_unused,             -- Ignore the ready signal
      DWE   => '0',                     -- Disable the write enable

      -- Don't implement dynamic phase shift
      PSCLK    => '0',                  -- No phase shift clock
      PSEN     => '0',                  -- Disable phase shift
      PSINCDEC => '0',                  -- No inc / dec of phase shift
      PSDONE   => psDone_unused,        -- Dummy signal for phase shift done

      -- Other control and status signals
      LOCKED       => locked,               -- MMCE clock is stable
      CLKINSTOPPED => clkinstopped_unused,  -- Input clock has stopped (not used)
      CLKFBSTOPPED => clkfbstopped_unused,  -- Feedback clock has stopped (not used)
      PWRDWN       => '0',                  -- Don't power down MMCE
      RST          => '0');                 -- No reset after startup

  -- Scaled clock
  clk1Buf : BUFGCE
    port map (O  => clkOutI1,
              CE => locked,
              I  => clkI1);
  clkOut <= clkOutI1;

  -- Provide the locked signal to the outside world
  isLocked <= locked;
  
end architecture;

