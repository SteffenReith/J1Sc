/*
 * Author: <AUTHORNAME> (<AUTHOREMAIL>)
 * Committer: <COMMITTERNAME>
 *
 * Creation Date:  Tue Nov 15 17:04:09 GMT+1 2016
 * Module Name:    J1Config - Holds a complete CPU configuration
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: <COMMITHASH>
 * Date: <AUTHORDATE>
 */

import spinal.core._

// The configuration set of a J1-CPU
case class J1Config (wordSize            : Int = 16,
                     dataStackIdxWidth   : Int =  8,
                     returnStackIdxWidth : Int =  6,
                     addrWidth           : Int = 13,
                     startAddress        : Int =  0 )
