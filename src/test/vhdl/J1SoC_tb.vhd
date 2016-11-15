--------------------------------------------------------------------------------
-- Author: Steffen Reith (Steffen.Reith@hs-rm.de)
-- Committer: <COMMITTERNAME>
--
-- Creation Date:  Thu Oct 13 20:44:40 GMT+2 2016
-- Creator:        Steffen Reith
-- Module Name:    J1SoC_TB - A simple testbench for the J1 SoC
-- Project Name:   J1Sc - A simple J1 implementation in scala
--
-- Hash: 1b3295f774ff2c8b47708463c37a618a142d1345
-- Date: Thu Oct 13 21:09:18 2016 +0200
--------------------------------------------------------------------------------
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

library std;
use std.textio.all;

entity J1SoC_tb is
end J1SoC_tb;

architecture Behavioral of J1SoC_tb is

  -- Clock period definition (100Mhz)
  constant clk_period : time := 10 ns;

  -- I/O signals 
  signal leds : std_logic_vector(15 downto 0);

  -- Clock and reset 
  signal clk : std_logic;
  signal clr : std_logic;

begin

  uut : entity work.J1SoC
    port map (clk  => clk,
              clr  => clr,
              leds => leds);

  -- Clock process definitions
  clk_process : process
  begin
    clk <= '0';
    wait for clk_period/2;
    clk <= '1';
    wait for clk_period/2;
  end process;

  -- Stimulus process
  stim_proc : process

    -- Text I/O
    variable lineBuffer : line;

  begin

    -- Give a info message
    write(lineBuffer, string'("Reset of CPU"));
    writeline(output, lineBuffer);
    
    -- Reset the CPU
    clr <= '1';

    -- Wait 30ns
    wait for 30ns;

    -- Wait for the next rising edge
    wait until rising_edge(clk);

    -- Revoke the the reset
    clr <= '0';

    -- Simply wait forever
    wait;

  end process;

end architecture;

