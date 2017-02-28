\ ---------------------------------------------
\ -- Module: AES 128 in ANS-FORTH -------------
\ -- Author: Thorsten Knoll -------------------
\ -- Date:   Feb 2017 -------------------------
\ -- Written for J1Sc CPU ---------------------
\ ---------------------------------------------

hex
VARIABLE STATE 10 ALLOT \ -------- Actual State
VARIABLE KEY 10 ALLOT \ ------------------- Key
VARIABLE RKEY 10 ALLOT \ ------------ Round Key
VARIABLE BT \ ----------------------- Byte Temp
VARIABLE WT 4 ALLOT \ ----------- AES-Word Temp
VARIABLE MC 4 ALLOT \ -------- Mix-Columns Temp
VARIABLE ROUND \ ---------------- Round Counter

\ ----------- Lookup Tables -------------------

CREATE SBOX
63 C, 7C C, 77 C, 7B C, F2 C, 6B C, 6F C, C5 C, 
30 C, 01 C, 67 C, 2B C, FE C, D7 C, AB C, 76 C, 
CA C, 82 C, C9 C, 7D C, FA C, 59 C, 47 C, F0 C, 
AD C, D4 C, A2 C, AF C, 9C C, A4 C, 72 C, C0 C, 
B7 C, FD C, 93 C, 26 C, 36 C, 3F C, F7 C, CC C, 
34 C, A5 C, E5 C, F1 C, 71 C, D8 C, 31 C, 15 C, 
04 C, C7 C, 23 C, C3 C, 18 C, 96 C, 05 C, 9A C, 
07 C, 12 C, 80 C, E2 C, EB C, 27 C, B2 C, 75 C, 
09 C, 83 C, 2C C, 1A C, 1B C, 6E C, 5A C, A0 C,
52 C, 3B C, D6 C, B3 C, 29 C, E3 C, 2F C, 84 C, 
53 C, D1 C, 00 C, ED C, 20 C, FC C, B1 C, 5B C, 
6A C, CB C, BE C, 39 C, 4A C, 4C C, 58 C, CF C, 
D0 C, EF C, AA C, FB C, 43 C, 4D C, 33 C, 85 C, 
45 C, F9 C, 02 C, 7F C, 50 C, 3C C, 9F C, A8 C, 
51 C, A3 C, 40 C, 8F C, 92 C, 9D C, 38 C, F5 C, 
BC C, B6 C, DA C, 21 C, 10 C, FF C, F3 C, D2 C, 
CD C, 0C C, 13 C, EC C, 5F C, 97 C, 44 C, 17 C, 
C4 C, A7 C, 7E C, 3D C, 64 C, 5D C, 19 C, 73 C, 
60 C, 81 C, 4F C, DC C, 22 C, 2A C, 90 C, 88 C, 
46 C, EE C, B8 C, 14 C, DE C, 5E C, 0B C, DB C, 
E0 C, 32 C, 3A C, 0A C, 49 C, 06 C, 24 C, 5C C, 
C2 C, D3 C, AC C, 62 C, 91 C, 95 C, E4 C, 79 C, 
E7 C, C8 C, 37 C, 6D C, 8D C, D5 C, 4E C, A9 C, 
6C C, 56 C, F4 C, EA C, 65 C, 7A C, AE C, 08 C, 
BA C, 78 C, 25 C, 2E C, 1C C, A6 C, B4 C, C6 C, 
E8 C, DD C, 74 C, 1F C, 4B C, BD C, 8B C, 8A C, 
70 C, 3E C, B5 C, 66 C, 48 C, 03 C, F6 C, 0E C, 
61 C, 35 C, 57 C, B9 C, 86 C, C1 C, 1D C, 9E C, 
E1 C, F8 C, 98 C, 11 C, 69 C, D9 C, 8E C, 94 C, 
9B C, 1E C, 87 C, E9 C, CE C, 55 C, 28 C, DF C, 
8C C, A1 C, 89 C, 0D C, BF C, E6 C, 42 C, 68 C, 
41 C, 99 C, 2D C, 0F C, B0 C, 54 C, BB C, 16 C,

CREATE SBOXINV
52 C, 09 C, 6A C, D5 C, 30 C, 36 C, A5 C, 38 C, 
BF C, 40 C, A3 C, 9E C, 81 C, F3 C, D7 C, FB C, 
7C C, E3 C, 39 C, 82 C, 9B C, 2F C, FF C, 87 C, 
34 C, 8E C, 43 C, 44 C, C4 C, DE C, E9 C, CB C, 
54 C, 7B C, 94 C, 32 C, A6 C, C2 C, 23 C, 3D C, 
EE C, 4C C, 95 C, 0B C, 42 C, FA C, C3 C, 4E C, 
08 C, 2E C, A1 C, 66 C, 28 C, D9 C, 24 C, B2 C, 
76 C, 5B C, A2 C, 49 C, 6D C, 8B C, D1 C, 25 C, 
72 C, F8 C, F6 C, 64 C, 86 C, 68 C, 98 C, 16 C, 
D4 C, A4 C, 5C C, CC C, 5D C, 65 C, B6 C, 92 C, 
6C C, 70 C, 48 C, 50 C, FD C, ED C, B9 C, DA C, 
5E C, 15 C, 46 C, 57 C, A7 C, 8D C, 9D C, 84 C, 
90 C, D8 C, AB C, 00 C, 8C C, BC C, D3 C, 0A C, 
F7 C, E4 C, 58 C, 05 C, B8 C, B3 C, 45 C, 06 C, 
D0 C, 2C C, 1E C, 8F C, CA C, 3F C, 0F C, 02 C, 
C1 C, AF C, BD C, 03 C, 01 C, 13 C, 8A C, 6B C, 
3A C, 91 C, 11 C, 41 C, 4F C, 67 C, DC C, EA C, 
97 C, F2 C, CF C, CE C, F0 C, B4 C, E6 C, 73 C, 
96 C, AC C, 74 C, 22 C, E7 C, AD C, 35 C, 85 C, 
E2 C, F9 C, 37 C, E8 C, 1C C, 75 C, DF C, 6E C, 
47 C, F1 C, 1A C, 71 C, 1D C, 29 C, C5 C, 89 C, 
6F C, B7 C, 62 C, 0E C, AA C, 18 C, BE C, 1B C, 
FC C, 56 C, 3E C, 4B C, C6 C, D2 C, 79 C, 20 C, 
9A C, DB C, C0 C, FE C, 78 C, CD C, 5A C, F4 C, 
1F C, DD C, A8 C, 33 C, 88 C, 07 C, C7 C, 31 C, 
B1 C, 12 C, 10 C, 59 C, 27 C, 80 C, EC C, 5F C, 
60 C, 51 C, 7F C, A9 C, 19 C, B5 C, 4A C, 0D C, 
2D C, E5 C, 7A C, 9F C, 93 C, C9 C, 9C C, EF C, 
A0 C, E0 C, 3B C, 4D C, AE C, 2A C, F5 C, B0 C, 
C8 C, EB C, BB C, 3C C, 83 C, 53 C, 99 C, 61 C, 
17 C, 2B C, 04 C, 7E C, BA C, 77 C, D6 C, 26 C, 
E1 C, 69 C, 14 C, 63 C, 55 C, 21 C, 0C C, 7D C,

CREATE LOG
00 C, 00 C, 19 C, 01 C, 32 C, 02 C, 1a C, c6 C, 
4b C, c7 C, 1b C, 68 C, 33 C, ee C, df C, 03 C,
64 C, 04 C, e0 C, 0e C, 34 C, 8d C, 81 C, ef C, 
4c C, 71 C, 08 C, c8 C, f8 C, 69 C, 1c C, c1 C,
7d C, c2 C, 1d C, b5 C, f9 C, b9 C, 27 C, 6a C, 
4d C, e4 C, a6 C, 72 C, 9a C, c9 C, 09 C, 78 C,
65 C, 2f C, 8a C, 05 C, 21 C, 0f C, e1 C, 24 C, 
12 C, f0 C, 82 C, 45 C, 35 C, 93 C, da C, 8e C,
96 C, 8f C, db C, bd C, 36 C, d0 C, ce C, 94 C, 
13 C, 5c C, d2 C, f1 C, 40 C, 46 C, 83 C, 38 C,
66 C, dd C, fd C, 30 C, bf C, 06 C, 8b C, 62 C, 
b3 C, 25 C, e2 C, 98 C, 22 C, 88 C, 91 C, 10 C,
7e C, 6e C, 48 C, c3 C, a3 C, b6 C, 1e C, 42 C, 
3a C, 6b C, 28 C, 54 C, fa C, 85 C, 3d C, ba C,
2b C, 79 C, 0a C, 15 C, 9b C, 9f C, 5e C, ca C, 
4e C, d4 C, ac C, e5 C, f3 C, 73 C, a7 C, 57 C,
af C, 58 C, a8 C, 50 C, f4 C, ea C, d6 C, 74 C, 
4f C, ae C, e9 C, d5 C, e7 C, e6 C, ad C, e8 C,
2c C, d7 C, 75 C, 7a C, eb C, 16 C, 0b C, f5 C, 
59 C, cb C, 5f C, b0 C, 9c C, a9 C, 51 C, a0 C,
7f C, 0c C, f6 C, 6f C, 17 C, c4 C, 49 C, ec C, 
d8 C, 43 C, 1f C, 2d C, a4 C, 76 C, 7b C, b7 C,
cc C, bb C, 3e C, 5a C, fb C, 60 C, b1 C, 86 C, 
3b C, 52 C, a1 C, 6c C, aa C, 55 C, 29 C, 9d C,
97 C, b2 C, 87 C, 90 C, 61 C, be C, dc C, fc C, 
bc C, 95 C, cf C, cd C, 37 C, 3f C, 5b C, d1 C,
53 C, 39 C, 84 C, 3c C, 41 C, a2 C, 6d C, 47 C, 
14 C, 2a C, 9e C, 5d C, 56 C, f2 C, d3 C, ab C,
44 C, 11 C, 92 C, d9 C, 23 C, 20 C, 2e C, 89 C, 
b4 C, 7c C, b8 C, 26 C, 77 C, 99 C, e3 C, a5 C,
67 C, 4a C, ed C, de C, c5 C, 31 C, fe C, 18 C, 
0d C, 63 C, 8c C, 80 C, c0 C, f7 C, 70 C, 07 C,

CREATE LOGINV
01 C, 03 C, 05 C, 0f C, 11 C, 33 C, 55 C, ff C, 
1a C, 2e C, 72 C, 96 C, a1 C, f8 C, 13 C, 35 C,
5f C, e1 C, 38 C, 48 C, d8 C, 73 C, 95 C, a4 C, 
f7 C, 02 C, 06 C, 0a C, 1e C, 22 C, 66 C, aa C,
e5 C, 34 C, 5c C, e4 C, 37 C, 59 C, eb C, 26 C, 
6a C, be C, d9 C, 70 C, 90 C, ab C, e6 C, 31 C,
53 C, f5 C, 04 C, 0c C, 14 C, 3c C, 44 C, cc C, 
4f C, d1 C, 68 C, b8 C, d3 C, 6e C, b2 C, cd C,
4c C, d4 C, 67 C, a9 C, e0 C, 3b C, 4d C, d7 C, 
62 C, a6 C, f1 C, 08 C, 18 C, 28 C, 78 C, 88 C,
83 C, 9e C, b9 C, d0 C, 6b C, bd C, dc C, 7f C, 
81 C, 98 C, b3 C, ce C, 49 C, db C, 76 C, 9a C, 
b5 C, c4 C, 57 C, f9 C, 10 C, 30 C, 50 C, f0 C, 
0b C, 1d C, 27 C, 69 C, bb C, d6 C, 61 C, a3 C,
fe C, 19 C, 2b C, 7d C, 87 C, 92 C, ad C, ec C, 
2f C, 71 C, 93 C, ae C, e9 C, 20 C, 60 C, a0 C, 
fb C, 16 C, 3a C, 4e C, d2 C, 6d C, b7 C, c2 C, 
5d C, e7 C, 32 C, 56 C, fa C, 15 C, 3f C, 41 C, 
c3 C, 5e C, e2 C, 3d C, 47 C, c9 C, 40 C, c0 C, 
5b C, ed C, 2c C, 74 C, 9c C, bf C, da C, 75 C,
9f C, ba C, d5 C, 64 C, ac C, ef C, 2a C, 7e C, 
82 C, 9d C, bc C, df C, 7a C, 8e C, 89 C, 80 C,
9b C, b6 C, c1 C, 58 C, e8 C, 23 C, 65 C, af C, 
ea C, 25 C, 6f C, b1 C, c8 C, 43 C, c5 C, 54 C, 
fc C, 1f C, 21 C, 63 C, a5 C, f4 C, 07 C, 09 C, 
1b C, 2d C, 77 C, 99 C, b0 C, cb C, 46 C, ca C, 
45 C, cf C, 4a C, de C, 79 C, 8b C, 86 C, 91 C, 
a8 C, e3 C, 3e C, 42 C, c6 C, 51 C, f3 C, 0e C,
12 C, 36 C, 5a C, ee C, 29 C, 7b C, 8d C, 8c C, 
8f C, 8a C, 85 C, 94 C, a7 C, f2 C, 0d C, 17 C, 
39 C, 4b C, dd C, 7c C, 84 C, 97 C, a2 C, fd C, 
1c C, 24 C, 6c C, b4 C, c7 C, 52 C, f6 C, 01 C,

CREATE ROUNDCON 
00 C, 01 C, 02 C, 04 C, 08 C, 10 C, 20 C, 40 C, 80 C, 1B C, 36 C,

\ ----------- Helper words --------------------

: STATE@ ( n --- @ state + n )
  STATE + C@ ;

: STATE! ( n addrn --- ! state + n )
  STATE + C! ;

: STATE-INIT ( --- state zeros )
  STATE 10 0 FILL ;

: STATE-SET-LOW ( n0, .. , n7 --- state low 8 bytes )
  8 0 DO I 8 + 
    STATE! 
  LOOP ;

: STATE-SET-HIGH ( n0, .. , n7 --- state high 8 bytes )
  8 0 DO I 
    STATE! 
  LOOP ;

: KEY@ ( n --- @ key + n )
  KEY + C@ ;

: KEY! ( n addrn --- ! key + n )
  KEY + C! ;

: KEY-INIT ( --- key zeros )
  KEY 10 0 FILL ;

: KEY-SET-LOW ( n0, .. , n7 --- key low 8 bytes )
  8 0 DO I 8 +
    KEY!
  LOOP ;

: KEY-SET-HIGH ( n0, .. , n7 --- key high 8 bytes )
  8 0 DO I
    KEY!
  LOOP ;

: RKEY@ ( n --- @ rkey + n )
  RKEY + C@ ;

: RKEY! ( n addrn --- ! rkey + n )
  RKEY + C! ;

: RKEY-INIT ( --- key zeros )
  RKEY 10 0 FILL ;

: WT@ ( n --- @ wt + n )
  WT + C@ ;

: WT! ( n addrn --- ! wt + n )
  WT + C! ;

: WT-INIT
  WT 4 0  FILL ;

: MC@ ( n --- @ mc + n )
  MC + C@ ;

: MC! ( n addrn --- ! mc + n )
  MC + C! ;

: MC-INIT
  MC 4 0 FILL ;

: SBOX@ ( n --- @ sbox + n )
  SBOX + C@ ;

: SBOXINV@ ( n --- @ sboxinv + n )
  SBOXINV + C@ ;

: LOG@
  LOG + C@ ;

: LOGINV@
  LOGINV + C@ ;


: MULGF2 ( n1,n2 --- n )
  OVER IF 
    LOG@ SWAP LOG@ +
    FF MOD LOGINV@
  ELSE 
    DROP DROP 0 
  THEN ;

\ --- Ouptut words ----------------------------

: STATE? ( --- )
  10 0 DO I 
    STATE@ .x2 
  LOOP ;

: KEY? ( --- )
  10 0 DO I 
    KEY@ .x2 
  LOOP ;

: RKEY? ( --- )
  10 0 DO I 
    RKEY@ .x2 
  LOOP ;

: WT? ( --- )
  4 0 DO I 
    WT@ .x2 
  LOOP ;

\ --- Key Expansion ---------------------------

: ROTWORD ( wt --- wt )
  0 WT@ BT ! 1 WT@ 0 WT! 2 WT@ 1 WT! 3 WT@ 2 WT! BT @ 3 WT! ;

: SUBWORD ( wt --- wt )
  4 0 DO I WT@ SBOX@ I WT! LOOP ;

: RCON ( wt --- wt )
  0 WT@ ROUND @ ROUNDCON + C@ XOR 0 WT! ;

: NEXTKEY ( rkey --- next rkey )
  \ --- word 0
  4 0 DO I C + RKEY@ I WT! LOOP
  ROTWORD SUBWORD RCON
  4 0 DO I RKEY@ I WT@ XOR I RKEY! LOOP
  \ --- word 1
  4 0 DO I 4 + RKEY@ I RKEY@ XOR I 4 + RKEY! LOOP
  \ --- word 2
  4 0 DO I 8 + RKEY@ I 4 + RKEY@ XOR I 8 + RKEY! LOOP
  \ --- word 3
  4 0 DO I C + RKEY@ I 8 + RKEY@ XOR I C + RKEY! LOOP ;
\ ---------------------------------------------
\ --------- AES 128 INIT ----------------------
\ ---------------------------------------------

: AES-INIT
  STATE-INIT
  KEY-INIT
  RKEY-INIT
  WT-INIT
  MC-INIT ;

\ ---------------------------------------------
\ --------- AES 128 ENCODING ------------------
\ ---------------------------------------------

: BYTES-SBOX ( state --- state )
  10 0 DO 
    I STATE@ SBOX@ 
    I STATE! 
  LOOP ;

: SHIFT-ROWS ( state --- state )
  1 STATE@ BT ! 5 STATE@ 1 STATE! 9 STATE@ 5 STATE! D STATE@ 9 STATE! BT @ D STATE!
  2 STATE@ BT ! A STATE@ 2 STATE! BT @ A STATE! 6 STATE@ BT ! E STATE@ 6 STATE! BT @ E STATE! 
  F STATE@ BT ! B STATE@ F STATE! 7 STATE@ B STATE! 3 STATE@ 7 STATE! BT @ 3 STATE! ;

: MIX-COLUMNS
  D 0 DO 
    \ --- i+0. byte of column
    I STATE@ 2 MULGF2   
    I 1 + STATE@ 3 MULGF2 XOR
    I 2 + STATE@ XOR
    I 3 + STATE@ XOR
    0 MC!
    \ --- i+1. byte of column
    I STATE@
    I 1 + STATE@ 2 MULGF2 XOR
    I 2 + STATE@ 3 MULGF2 XOR
    I 3 + STATE@ XOR
    1 MC!
    \ --- i+2. byte of column
    I STATE@
    I 1 + STATE@ XOR
    I 2 + STATE@ 2 MULGF2 XOR
    I 3 + STATE@ 3 MULGF2 XOR
    2 MC!
    \ --- i+3. byte of column
    I STATE@ 3 MULGF2
    I 1 + STATE@ XOR
    I 2 + STATE@ XOR
    I 3 + STATE@ 2 MULGF2 XOR
    3 MC!
    4 0 DO
      I MC@
      I J + STATE!
    LOOP
  4 +LOOP ;

: ADD-RKEY0 ( key --- rkey , state --- state)
  10 0 DO
    I KEY@
    I RKEY!
  LOOP
  10 0 DO
    I STATE@
    I RKEY@
    XOR
    I STATE!
  LOOP ;

: ADD-RKEYN
  NEXTKEY
  10 0 DO
    I STATE@
    I RKEY@
    XOR
    I STATE!
  LOOP ;

: ENCODE128 ( key plainstate --- cipherstate )
  0 ROUND C! 
  ADD-RKEY0
  A 1 DO 
    I ROUND C!
    BYTES-SBOX
    SHIFT-ROWS
    MIX-COLUMNS
    ADD-RKEYN
  LOOP
  A ROUND C!
  BYTES-SBOX
  SHIFT-ROWS
  ADD-RKEYN ;

\ ---------------------------------------------
\ --------- AES 128 DECODING ------------------
\ ---------------------------------------------

: BYTES-SBOX-INV
  10 0 DO
    I STATE@ SBOXINV@
    I STATE!
  LOOP ;

: SHIFT-ROWS-INV
  ;

: MIX-COLUMNS-INV
  ;

: ADD-RKEY-INV
  ;

: DECODE128
  ;

\ --------- TESTING ---------------------------

: SET-VARS

  AES-INIT

  \ ---- Key from FIPS-197 ( key-byte nr. 0 on top of the stack )
  0F 0E 0D 0C 0B 0A 09 08 
  KEY-SET-LOW
  07 06 05 04 03 02 01 00
  KEY-SET-HIGH

  \ ---- Plaintext from FIPS-197 ( state-byte nr. 0 on top of the stack)
  FF EE DD CC BB AA 99 88 
  STATE-SET-LOW
  77 66 55 44 33 22 11 00
  STATE-SET-HIGH ;

: ENCTEST

  SET-VARS
  \ ---- Do thwe encoding
  CR ." KEY:    " KEY?
  CR ." PLAIN:  " STATE?
  ENCODE128
  CR ." ENCODING... "
  CR ." CIPHER: " STATE? ;

