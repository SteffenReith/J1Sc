--------------------------------------------------------------------------------
-- Author: <AUTHORNAME> (<AUTHOREMAIL>)
-- Committer: <COMMITTERNAME>
--
-- Creation Date:  Sun Dec 11 11:46:48 GMT+1 2016
-- Creator:        Steffen Reith
-- Module Name:    J1SoC_IRQ_TB - A simple testbench for testing the interrupts
--                                of the J1 SoC
-- Project Name:   J1Sc - A simple J1 implementation in scala
--
-- Hash: <COMMITHASH>
-- Date: <AUTHORDATE>
--------------------------------------------------------------------------------
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

library std;
use std.textio.all;

entity J1SoC_IRQ_tb is
end J1SoC_IRQ_tb;

architecture Behavioral of J1SoC_IRQ_tb is

  -- Clock period definition (100Mhz)
  constant clk_period : time := 10 ns;

  -- Interrupts
  signal extInt : std_logic_vector(0 downto 0) := "0";

  -- UART signals
  signal rx : std_logic := '0';
  signal tx : std_logic;

  -- I/O signals 
  signal leds : std_logic_vector(15 downto 0);

  -- Clock and reset 
  signal clk : std_logic;
  signal clr : std_logic;

begin

  uut : entity work.J1SoC
    port map (clk    => clk,
              clr    => clr,
              extInt => extInt,
              rx     => rx,
              tx     => tx,
              leds   => leds);

  -- Clock process definitions
  clk_process : process
  begin
    clk <= '0';
    wait for clk_period/2;
    clk <= '1';
    wait for clk_period/2;
  end process;

  interrup_proc : process
  begin

    --Wait 95ns
    wait for 95 ns;

    -- Activate an interrupt (asynchronous)
    extInt(0) <= '1';

    --Wait some clocks
    wait for 33 ns;
    
    -- Revoke the the interrupt (asynchronous)
    extInt(0) <= '0';

    -- wait forever
    wait;

  end process;

  reboot_proc : process
  begin

    -- Reset the CPU (asynchron)
    clr <= '1';

    -- Wait 400ns
    wait for 407 ns;

    -- Revoke the the reset
    clr <= '0';

    -- Wait forever  
    wait;

  end process;


  -- Stimulus process
  stim_proc : process

    -- Text I/O
    variable lineBuffer : line;

  begin

    -- Give a info message
    write(lineBuffer, string'("Start the simulation of the CPU"));
    writeline(output, lineBuffer);

    -- Simply wait forever
    wait;

  end process;

end architecture;
