--------------------------------------------------------------------------------
-- Author: <AUTHORNAME> (<AUTHOREMAIL>)
-- Committer: <COMMITTERNAME>
--
-- Creation Date:  Fri Apr 7 16:00:52 GMT+2 2017
-- Creator:        Steffen Reith
-- Module Name:    J1ScNexys4DDR - Behavioral
-- Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
--
-- Remark: The pmod pins are renumberd as follows 1 -> 0, 2 -> 1, 3 -> 2,
--         4 -> 3, 7 -> 4, 8 -> 5, 9 -> 6, 10 -> 7
--
-- Hash: <COMMITHASH>
-- Date: <AUTHORDATE>
--------------------------------------------------------------------------------

library ieee;
use ieee.std_logic_1164.all;

entity J1ScNexys4DDR is

  port (reset     : in    std_logic;
        clk100Mhz : in    std_logic;
        extInt    : in    std_logic_vector(0 downto 0);
        leds      : out   std_logic_vector(15 downto 0);
        pmodA     : inout std_logic_vector(7 downto 0);
        rx        : in    std_logic;
        tx        : out   std_logic);

end J1ScNexys4DDR;

architecture Structural of J1ScNexys4DDR is

  signal pmodA_read        : std_logic_vector(7 downto 0);
  signal pmodA_write       : std_logic_vector(7 downto 0);
  signal pmodA_writeEnable : std_logic_vector(7 downto 0);
  
begin

  -- Instantiate the J1SoC core created by Spinal
  core : entity work.J1SoC
    port map (reset             => reset,
              clk100Mhz         => clk100Mhz,
              extInt            => extInt,
              leds              => leds,
              pmodA_read        => pmodA_read,
              pmodA_write       => pmodA_write,
              pmodA_writeEnable => pmodA_writeEnable,
              rx                => rx,
              tx                => tx);

  -- Connect the pmodA read port
  pmodA_read <= pmodA;

  -- generate the write port and equip it with tristate functionality
  pmodAGen : for i in pmodA'range generate
    pmodA(i) <= pmodA_write(i) when pmodA_writeEnable(i) = '1' else 'Z';
  end generate;

end architecture;
