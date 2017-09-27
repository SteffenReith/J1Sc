--------------------------------------------------------------------------------
-- Author: Steffen.Reith (Steffen.Reith@hs-rm.de)
--
-- Creation Date:  Thu Oct 13 20:44:40 GMT+2 2016
-- Creator:        Steffen Reith
-- Module Name:    J1SoC_TB - A simple testbench for the J1 SoC
-- Project Name:   J1Sc - A simple J1 implementation in scala
--
--------------------------------------------------------------------------------
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

  -- Interrupts
  signal extInt : std_logic_vector(0 downto 0) := "0";

  -- PModA-Interface
  signal pmodA_read        : std_logic_vector(7 downto 0);
  signal pmodA_write       : std_logic_vector(7 downto 0);
  signal pmodA_writeEnable : std_logic_vector(7 downto 0);

  -- UART signals
  signal rx : std_logic := '0';
  signal tx : std_logic;

  -- I/O signals 
  signal leds : std_logic_vector(15 downto 0);

  -- Clock and reset 
  signal boardClkLocked : std_logic;
  signal boardClk       : std_logic;
  signal reset          : std_logic;

begin

  uut : entity work.J1SoC
    port map (boardClk          => boardClk,
              boardClkLocked    => boardClkLocked,
              reset             => reset,
              extInt            => extInt,
              pmodA_read        => pmodA_read,
              pmodA_write       => pmodA_write,
              pmodA_writeEnable => pmodA_writeEnable,
              rx                => rx,
              tx                => tx,
              leds              => leds);

  -- Clock process definitions
  clk_process : process
  begin

    -- Tell that the clock is stable
    boardClkLocked <= '1';

    boardClk <= '0';
    wait for clk_period/2;

    boardClk <= '1';
    wait for clk_period/2;

  end process;

  reboot_proc : process
  begin

    -- Init the read port of PModA
    pmodA_read <= (others => '0');

    -- Reset the CPU (asynchronous) 
    reset <= '1';

    -- Wait 57ns
    wait for 57 ns;

    -- Revoke the the reset
    reset <= '0';

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
