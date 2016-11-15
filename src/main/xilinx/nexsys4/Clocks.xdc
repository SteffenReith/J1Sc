#create_clock -period 20.000 -name clk -waveform {0.000 10.000} [get_ports clk] # 50 Mhz Clock
create_clock -period 10.000 -name clk -waveform {0.000 5.000} [get_ports clk] # 100 Mhz Clock
#create_clock -period 7.5000 -name clk -waveform {0.000 3.7500} [get_ports clk]
#create_clock -period 8.3333 -name clk -waveform {0.000 4.1666} [get_ports clk]
