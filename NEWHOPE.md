# Fork J1Sc dla newhope (FPGA-snake)

Fork https://github.com/SteffenReith/J1Sc (BSD-3, copyright Steffen Reith — patrz `LICENSE`).
Gałąź robocza: `newhope`. Upstream: `git remote add upstream https://github.com/SteffenReith/J1Sc`.

## Co się zmieniło względem upstreamu (`master`, 7a0056e, 2020-06)

| | upstream | newhope |
| --- | --- | --- |
| Scala / SpinalHDL / sbt | 2.11.8 / 1.4.2 / 0.13.16 | 2.13.14 / 1.12.3 / 1.12.4 (jak fpga-hw-lib) |
| pakiet | domyślny | `j1sc` (rdzeń), `j1sc.boards` (płytki) |
| źródła | `src/main/scala`, `src/test/scala` | `j1/hw/spinal/{main,test}`, `boards/hw/spinal/{main,test}` |
| projekty sbt | jeden + taski iceGen/icePnr/icoProg/… | `j1`, `boards`, root `j1sc` |
| jssc | org.scream3r 2.8.0 | io.github.java-native 2.9.6 |

Poprawki (osobne commity):
- `J1RStack`: wskaźnik stosu powrotu miał szerokość z `dataStackIdxWidth`;
  wszystkie konfiguracje z różnymi głębokościami stosów (`default16`, `debug16`, …)
  nie elaborowały się. `forth16` (5/5) to maskował.
- `*.ys` (iCE40): `attrmap -remove ram_style` — Spinal ≥ 1.5 oznacza stosy
  (`readAsync`) jako `ram_style = "distributed"`, a iCE40 nie ma LUTRAM.

Logika poza tym bez zmian (API: `Bool()`, `FlowCCUnsafeByToggle`, `lazyZip`, …).

## Budowanie
```
cd toolchain/forth && make && cd ../..          # gforth: build/16bit/nuc.binary (forth16)
sbt j1/test                                     # J1IsaTest (Verilator)
sbt "boards/runMain j1sc.boards.J1Ice"          # -> gen/src/{verilog,vhdl}/J1Ice.*
yosys src/main/lattice/IceBreaker/J1Ice.ys      # synteza jak w upstreamie
```
Taski sbt iceSynth/icePnr/iceProg upstreamu zostały usunięte — flow z yosysa
i nextpnr przejdzie do Makefile'i (jak w FPGA-snake).

Zajętość (yosys + nextpnr-ice40, UP5K): cały SoC `J1Ice` (CPU, 4 k słów RAM z
Forthem, UART, timery, PWM, LED-y, przyciski, JTAG) — 2 009 / 5 280 LC, 18 / 30 EBR.

## Jako submodule
```
git submodule add -b newhope https://github.com/TomekBarabasz/J1Sc hw/cpu/j1
```
`hw/build.sbt` konsumenta:
```scala
lazy val j1 = ProjectRef(file("cpu/j1"), "j1")
lazy val snake = (...).dependsOn(j1)
```
scalaVersion w obu buildach musi być ta sama (`build.sbt` tutaj vs `HwBuild.scala`).

## Do zrobienia
- ROM z firmware przez `inlineRom` zamiast `$readmemb` (pliki `.bin` obok `.v`,
  workaround z kopiowaniem w upstreamie).
- `bootCode` z pliku firmware bez `sys.exit` przy braku pliku (dziś `forth16`
  zabija JVM, gdy `nuc.binary` nie ma).
- Top-levele dla Tang Nano 9K i iCESugar (w FPGA-snake albo w `boards/`).
