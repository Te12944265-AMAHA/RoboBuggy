/********************************************************************************
 * All rights are open to the public, to use, share, dump, spread for the       *
 * foreseeable future. This software and documentation constitutes purely 		*
 * published and public work, and contains open source knowledge by a bunch		*
 * of college kids who just want to have fun. All the material and code may be 	*
 * used, copied, duplicated, changed, disclosed... with any human's free will.	*
 * Have a nice day! :)															*
 ********************************************************************************/

#include "IO_pad.h"
#include "lib_protocol.h"
#include "lib_types.h"

void setup() {
  Serial.begin(9600);
}

void loop() {
  // This is the tester
  // It will continuously send a packet to the computer side
  // Arduino -> Computer
  byte index = 0x01;
  unsigned int message = 0xDEAD;
  unsigned long packet = protocol_send(index, message);
  Serial.write((uint8_t*)packet, 4);
  
  // TODO: should have a safe fault eg. packet != NULL
//  Serial.print("The packet is: ");
//  Serial.println(packet,HEX);
  
  
  /************** RECEIVE PACKET ********************/
   // if Serial is available read if there are any incoming messages
//   if (Serial.available()) {
//          // read the incoming byte:
//          byte incomingByte = Serial.read();
//
//          // say what you got:
////          Serial.print("I received: ");
////          Serial.println(incomingByte, HEX);
//          
//          if(incomingByte == HEAD)
//          {
//            unsigned long pckt = incomingByte;
//            for(int i = 0; i < 3; i++)
//            {
//              pckt = pckt << ONE_BYTE_SIZE;
//              incomingByte = Serial.read();
//              pckt = pckt | incomingByte;  
//            }
////           Serial.print("I received a packet! It is: ");
////           Serial.println(pckt, HEX); 
//           Serial.write(pckt);
//          }
//          // parse the byte?
//          
//  } 
  delay(500);
}
