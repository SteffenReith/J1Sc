create_clock -period 10.000 -name clk100Mhz -waveform {0.000 5.000} [get_ports clk100Mhz]
create_clock -period 1000.000 -name tck -waveform {0.000 500.000} [get_ports tck]

