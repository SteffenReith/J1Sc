import scala.sys.process.Process

// The scala version needed for Spinal
scalaVersion := "2.11.12"

val spinalVersion = "1.9.4"

// Added the spinal libraries and a UART-driver
libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % spinalVersion,
  "com.github.spinalhdl" % "spinalhdl-lib_2.11"  % spinalVersion,
  "com.github.spinalhdl" % "spinalhdl-sim_2.11"  % spinalVersion, compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.11" % spinalVersion),
  "io.github.java-native" % "jssc" % "2.9.4",
  "com.github.scopt" %% "scopt" % "4.1.0"
)

fork := true

// Settings for the build process
lazy val iceToplevel    = settingKey[String]("Name of the toplevel module used for an IceBreaker board")
lazy val asicToplevel   = settingKey[String]("Name of the toplevel module used for an ASIC")
lazy val nexysToplevel  = settingKey[String]("Name of the toplevel module used for a Digilent Nexys4 / Nexys4 DDR board")

lazy val genDir        = settingKey[String]("Output directory for RTL")

lazy val nexys4Gen = taskKey[Unit]("Generate Verilog and VHDL code using SpinalHDL for Digilent Nexys4/Nexys4DDR")

lazy val iceGen    = taskKey[Unit]("Generate Verilog and VHDL code using SpinalHDL for an IceBreaker board")
lazy val iceSynth  = taskKey[Unit]("Synthesize Verilog with yosys")
lazy val icePnr    = taskKey[Unit]("Place and route with nextpnr")
lazy val iceProg   = taskKey[Unit]("Send the bit-file to an attached IceBreaker board")

lazy val asicGen    = taskKey[Unit]("Generate Verilog and VHDL code using SpinalHDL for a tiny ASIC")

lazy val cleanAll  = taskKey[Unit]("Delete all files produced by the build (sources, classes, caches, etc)")

// Clean all generated files
cleanAll := {

  val baseDir       = baseDirectory.value
  val iceTop        = iceToplevel.value
  val asicTop       = asicToplevel.value
  val nexysTop      = nexysToplevel.value
  val verilogGenDir = "gen/src/verilog"
  val vhdlGenDir    = "gen/src/vhdl"

  // Print a debug message
  println("[sbt-info] Remove all generated files")

  // Start to remove the files
  Process("rm" :: "-f" :: s"${verilogGenDir}/${nexysTop}.v" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${iceTop}.v" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${iceTop}.asc" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${iceTop}.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${iceTop}.blif" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${iceTop}.json" :: Nil, baseDir) !; 
  Process("rm" :: "-f" :: s"${verilogGenDir}/${asicTop}.v" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: "cpu0.yaml" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${iceTop}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${iceTop}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${asicTop}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${asicTop}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${nexysTop}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${nexysTop}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${vhdlGenDir}/${iceTop}.vhd" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${vhdlGenDir}/${nexysTop}.vhd" :: Nil, baseDir) !;
  Process("rm" :: "-fR" :: s"target/project/target" :: Nil, baseDir) !;

}

// Run the scala program to generate the Verilog and VHDL files for a Digilent Nexys4 / Nexys4DDR
nexys4Gen := {

  // Specify main class
  val generator = "J1Nexys4X"

  // Run the main-class
  Def.taskDyn[Unit] {

    // Print a debug message
    println("[sbt-info] Generate Verilog and VHDL code for a Digilent Nexys4/Nexys4 DDR board")

    // Run the Scala binary
    (runMain in Compile).toTask(s" ${generator}")

  }

}.value

// Run the scala program to generate the Verilog and VHDL files for an IceBreaker board
iceGen := {

  // Specify main class
  val generator = "J1Ice"

  // Run the main-class
  Def.taskDyn[Unit] {

    // Print a debug message
    println("[sbt-info] Generate Verilog and VHDL code for an IceBreaker board")

    // Run the Scala binary
    (runMain in Compile).toTask(s" ${generator}")

  }

}.value

// Do synthesize step
iceSynth :=  {

  // Generate the project first
  iceGen.value

  val toplevel = iceToplevel.value
  val baseDir  = baseDirectory.value
  val outDir   = baseDirectory.value / genDir.value
  val yosysDir = "src/main/lattice/IceBreaker"

  // Print a debug message
  println("[sbt-info] Synthesize netlist using yosys")

  // Extremely ugly workaround because yosys don't supports a path for $readmemb
  Process("cp" :: s"${outDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: s"${baseDir}" :: Nil, baseDir) !;
  Process("cp" :: s"${outDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: s"${baseDir}" :: Nil, baseDir) !;

  // Do the synthesis using yosys to generate a netlist out of the verilog code
  Process("yosys" :: "-q" :: s"${baseDir}/${yosysDir}/${toplevel}.ys" :: Nil, baseDir) !;

  // Remove the temporarily copied files copied by the ugly workaround
  Process("rm" :: s"${baseDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: s"${baseDir}/${toplevel}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;

}

// Do place, route and check using the IceStorm toolchain
icePnr := {

  // Synthesize first
  iceSynth.value

  val baseDir     = baseDirectory.value 
  val toplevel    = iceToplevel.value
  val outDir      = baseDirectory.value / genDir.value
  val latticePath = baseDirectory.value / "src/main/lattice/IceBreaker"
  val clockPath   = "src/main/lattice/IceBreaker"

  // Print a debug message
  println("[sbt-info] Do place, route, check and generate a bit-file using the IceStorm toolchain")

  // Do the place and route, check the result and generate a bit file
  Process("nextpnr-ice40" :: "--pre-pack" :: s"${baseDir}/${clockPath}/clocks" :: "--pcf" :: s"${latticePath}/${toplevel}.pcf" :: "--up5k" :: "--json" :: s"${outDir}/${toplevel}.json" :: "--asc" :: s"${toplevel}.asc" :: Nil, outDir) !;
  Process("icepack" :: s"${toplevel}.asc" :: s"${toplevel}.bin" :: Nil, outDir) !;

}

// Send the bit-file to an attached IceBreaker board
iceProg := {

  // Generate the bit-file first
  icePnr.value

  val toplevel = iceToplevel.value
  val outDir   = baseDirectory.value / genDir.value

  // Print a debug message
  println("[sbt-info] Send bit-file to attached IceBreaker board")

  // Send the file
  Process("iceprog" :: s"${outDir}/${toplevel}.bin" :: Nil, outDir) !;

}

// Run the scala program to generate the Verilog and VHDL files for an IcoBoard
asicGen := {

  // Specify main class
  val generator = "J1Asic"

  // Run the main-class
  Def.taskDyn[Unit] {

    // Print a debug message
    println("[sbt-info] Generate Verilog code for an ASIC")

    // Run the Scala binary
    (runMain in Compile).toTask(s" ${generator}")

  }

}.value

lazy val J1Sc = RootProject(uri("git://github.com/SteffenReith/J1Sc.git"))

// Default settings
lazy val root = (project in file(".")).settings(organization     := "com.gitHub.J1Sc",
                                                scalaVersion     := "2.9.4",
                                                version          := "0.1",
                                                name             := "J1Sc",
                                                iceToplevel      := "J1Ice",
                                                asicToplevel     := "J1Asic",
                                                nexysToplevel    := "J1Nexys4X",
                                                genDir           := "gen/src/verilog")
