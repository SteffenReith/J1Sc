// The scala version needed for Spinal
scalaVersion := "2.11.8"

// Added the spinal libraries and a UART-driver
libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "latest.release",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "latest.release",
  "com.github.spinalhdl" % "spinalhdl-sim_2.11" % "latest.release",
  "org.scream3r" % "jssc" % "2.8.0"
)

// Add the plugin for SpinalSim
addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.11.6" % "1.0.2")
scalacOptions += "-P:continuations:enable"
fork := true

// Setting for the build process
lazy val icoToplevel   = settingKey[String]("Name of the toplevel module used for an IcoBoard")
lazy val nexysToplevel = settingKey[String]("Name of the toplevel module used for a Digilent Nexys4 / Nexys4 DDR board")
lazy val genDir        = settingKey[String]("Output directory for RTL")

lazy val icoGen    = taskKey[Unit]("Generate Verilog and VHDL code using SpinalHDL for Digilent Nexys4/Nexys4DDR")
lazy val nexys4Gen = taskKey[Unit]("Generate Verilog code using SpinalHDL for an IcoBoard")
lazy val icoSynth  = taskKey[Unit]("Synthesize Verilog with yosys")
lazy val icoPnr    = taskKey[Unit]("Place and route with Icestorm toolchain")
lazy val cleanAll  = taskKey[Unit]("Delete all files produced by the build (sources, classes, caches, etc)")
lazy val icoProg   = taskKey[Unit]("Send the bit-file to an attached ico-board")

// Clean all generated files
cleanAll := {

  val baseDir       = baseDirectory.value
  val icoTop        = icoToplevel.value
  val nexysTop      = nexysToplevel.value
  val verilogGenDir = "gen/src/verilog"
  val vhdlGenDir    = "gen/src/vhdl"

  // Print a debug message
  println("[sbt-info] Remove all generated files")

  // Start to remove the files
  Process("rm" :: "-f" :: s"${verilogGenDir}/${icoTop}.v" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${nexysTop}.v" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${icoTop}.asc" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${icoTop}.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${icoTop}.blif" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: "cpu0.yaml" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${icoTop}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${icoTop}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${nexysTop}.v_toplevel_coreArea_cpu_mainMem_ramList_0.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${verilogGenDir}/${nexysTop}.v_toplevel_coreArea_cpu_mainMem_ramList_1.bin" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${vhdlGenDir}/${icoTop}.vhd" :: Nil, baseDir) !;
  Process("rm" :: "-f" :: s"${vhdlGenDir}/${nexysTop}.vhd" :: Nil, baseDir) !;
  Process("rm" :: "-fR" :: s"target/ project/target" :: Nil, baseDir) !;

}

// Run the scala program to generate the Verilog and VHDL files for a Digilent Nexys4 / Nexys4DDR
nexys4Gen := {

  // Specify main class
  val generator = "J1Nexys4X"

  // Print a debug message
  println("[sbt-info] Generate VHDL code for a Digilent Nexys4/Nexys4 DDR board")

  // Run the main-class
  Def.taskDyn[Unit] {

    // Run the Scala binary
    (runMain in Compile).toTask(s" ${generator}")

  }

}.value

// Run the scala program to generate the Verilog and VHDL files for an IcoBoard
icoGen := {

  // Specify main class
  val generator = "J1Ico"

  // Print a debug message
  println("[sbt-info] Generate Verilog code for an IcoBoard")

  // Run the main-class
  Def.taskDyn[Unit] {

    // Run the Scala binary
    (runMain in Compile).toTask(s" ${generator}")

  }

}.value

// Do synthesize step
icoSynth :=  {

  // Generate the project first
  icoGen.value

  val toplevel = icoToplevel.value
  val baseDir  = baseDirectory.value
  val outDir   = baseDirectory.value / genDir.value
  val yosysDir = "src/main/lattice/IcoBoard"

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
icoPnr := {

  // Synthesize first
  icoSynth.value

  val toplevel    = icoToplevel.value
  val outDir      = baseDirectory.value / genDir.value
  val latticePath = baseDirectory.value / "src/main/lattice/IcoBoard"

  // Print a debug message
  println("[sbt-info] Do place, route, check and generate a bit-file using the IceStorm toolchain")

  // Do the place and route, check the result and generate a bit file
  Process("arachne-pnr" :: "-p" :: s"${latticePath}/${toplevel}.pcf" :: "-d" :: "8k" :: "--max-passes" :: "600" :: s"${toplevel}.blif" :: "-o" :: s"${toplevel}.asc" :: Nil, outDir) !;
  Process("icetime" :: "-tmd" :: "hx8k" :: "-c 40" :: s"${toplevel}.asc" :: Nil, outDir) !;
  Process("icepack" :: s"${toplevel}.asc" :: s"${toplevel}.bin" :: Nil, outDir) !;

}

// Send the bit-file to an attached IcoBoard
icoProg := {

  // Generate the bit-file first
  icoPnr.value

  val toplevel = icoToplevel.value
  val outDir   = baseDirectory.value / genDir.value

  // Print a debug message
  println("[sbt-info] Send bit-file to attached IcoBoard")

  // Send the file
  //"icoprog -p" #< s"${outDir}/${toplevel).bin" !;
  Process("icoprog" :: "-p" :: Nil, outDir) #< file(s"${outDir}/${toplevel}.bin") !;

}

lazy val J1Sc = RootProject(uri("git://github.com/SteffenReith/J1Sc.git"))

// Default settings
lazy val root = (project in file(".")).settings( organization     := "com.gitHub.J1Sc",
                                                 scalaVersion     := "2.11.8",
                                                 version          := "0.1",
                                                 name             := "J1Sc",
                                                 icoToplevel      := "J1Ico",
                                                 nexysToplevel    := "J1Nexys4X",
                                                 genDir           := "gen/src/verilog")
