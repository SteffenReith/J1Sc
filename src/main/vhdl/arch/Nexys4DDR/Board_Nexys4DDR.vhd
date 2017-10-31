--------------------------------------------------------------------------------
--
-- Creation Date:  Fri Apr 7 16:00:52 GMT+2 2017
-- Creator:        Steffen Reith
-- Module Name:    Board_Nexys4DDR - Behavioral
-- Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
--
-- Remark: The pmod pins are renumberd as follows 1 -> 0, 2 -> 1, 3 -> 2,
--         4 -> 3, 7 -> 4, 8 -> 5, 9 -> 6, 10 -> 7
--
--------------------------------------------------------------------------------

library ieee;
use ieee.std_logic_1164.all;

entity Board_Nexys4DDR is

  port (nreset     : in    std_logic;
        clk100Mhz  : in    std_logic;
        extInt     : in    std_logic_vector(0 downto 0);
        leds       : out   std_logic_vector(15 downto 0);
        rgbLeds    : out   std_logic_vector(5 downto 0);
        segments_a : out   std_logic;
        segments_b : out   std_logic;
        segments_c : out   std_logic;
        segments_d : out   std_logic;
        segments_e : out   std_logic;
        segments_f : out   std_logic;
        segments_g : out   std_logic;
        dot        : out   std_logic;
        selector   : out   std_logic_vector(7 downto 0);
        pmodA      : inout std_logic_vector(7 downto 0);
        sSwitches  : in    std_logic_vector(15 downto 0);
        pButtons   : in    std_logic_vector(4 downto 0);
        rx         : in    std_logic;
        tx         : out   std_logic);

end Board_Nexys4DDR;

architecture Structural of Board_Nexys4DDR is

  -- Positive reset signal
  signal reset : std_logic;

  -- Signals related to the board clk
  signal boardClk       : std_logic;
  signal boardClkLocked : std_logic;

  -- Interface for PModA
  signal pmodA_read        : std_logic_vector(7 downto 0);
  signal pmodA_write       : std_logic_vector(7 downto 0);
  signal pmodA_writeEnable : std_logic_vector(7 downto 0);

begin

  -- Instantiate a PLL/MMCM (makes a 80Mhz clock)
  makeClk : entity work.PLL(Structural)
    port map (clkIn    => clk100Mhz,
              clkOut   => boardClk,
              isLocked => boardClkLocked);

 -- Make the reset positive
 reset <= not nreset;

  -- Instantiate the J1SoC core created by Spinal
  core : entity work.J1SoC
    port map (reset             => reset,
              boardClk          => boardClk,
              boardClkLocked    => boardClkLocked,
              extInt            => extInt,
              leds              => leds,
              rgbLeds           => rgbLeds,
              segments_a        => segments_a,
              segments_b        => segments_b,
              segments_c        => segments_c,
              segments_d        => segments_d,
              segments_e        => segments_e,
              segments_f        => segments_f,
              segments_g        => segments_g,
              dot               => dot,
              selector          => selector,
              pmodA_read        => pmodA_read,
              pmodA_write       => pmodA_write,
              pmodA_writeEnable => pmodA_writeEnable,
              sSwitches         => sSwitches,
              pButtons          => pButtons,
              rx                => rx,
              tx                => tx);

  -- Connect the pmodA read port
  pmodA_read <= pmodA;

  -- generate the write port and equip it with tristate functionality
  pmodAGen : for i in pmodA'range generate
    pmodA(i) <= pmodA_write(i) when pmodA_writeEnable(i) = '1' else 'Z';
  end generate;

end architecture;
