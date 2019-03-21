package frc.vision;

import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class PixyCamera2
{
    private I2C pixy2 = null;

    /**
     * 0x104
     */
     public final static int PIXY_BUFFERSIZE = 0x104;
     
     /**
      * 4
      */
    public final static int PIXY_SEND_HEADER_SIZE = 4;
    
    /**
     * 33
     */
    public final static int PIXY_MAX_PROGNAME = 33;
    
    /**
     * 0x8000 0000
     */
    public final static int PIXY_DEFAULT_ARGVAL = 0x80000000;
    
    /**
     * 0xc1af
     */
    public final static int PIXY_CHECKSUM_SYNC = 0xc1af;
    
    /**
     * 0xc1ae
     */
    public final static int PIXY_NO_CHECKSUM_SYNC = 0xc1ae;
    
    /**
     * 0x0e
     */
    public final static int PIXY_GETVERSION_TYPE = 0x0e;

    /**
     * 0x20
     */
    public final static int PIXY_GETBLOCKS_TYPE = 0x20;

    /**
     * 0x21
     */
    public final static int PIXY_RESPONSE_BLOCKS = 0x21;

    // Color Connected Component signature map
    public final static byte CCC_SIG1 = 0x01;
	public final static byte CCC_SIG2 = 0x02;
	public final static byte CCC_SIG3 = 0x04;
	public final static byte CCC_SIG4 = 0x08;
	public final static byte CCC_SIG5 = 0x10;
	public final static byte CCC_SIG6 = 0x20;
    public final static byte CCC_SIG7 = 0x40;

    /**
     * 19
     */
    public final static int RESPONSE_BLOCK_LENGTH = 20;
    
    /**
     * Combines two bytes into integer
     * @param upper
     * @param lower
     * @return
     */
    private int bytesToInt(byte upper, byte lower) {
        System.out.println("upper: " + upper + " lower: " + lower);
        return (((int) upper & 0xff) << 8) | ((int) lower & 0xff);
    }

    /**
     * Pixy2 Camera class - 
     *                                                                                
     * Uses the IC2 class to communicates to the Pixy2. Follow the Pixy2 API
     * 
     */
    public PixyCamera2(Port port, int deviceAddress) {
        pixy2 = new I2C(port, deviceAddress);
    }

    /**
     * Sends the version request packet to Pixy and asks for the version response packet.
     * The response packet contains the Pixy2 version information. This method uses the I2C
     * class transaction method to send and receives the packets.
     * @return
     */
    public boolean getVersion() {
        byte requestPacket[] = new byte[PIXY_BUFFERSIZE];
        requestPacket[0] = (byte) (PIXY_NO_CHECKSUM_SYNC & 0xFF);
        requestPacket[1] = (byte) ((PIXY_NO_CHECKSUM_SYNC >> 8) & 0xFF);
        requestPacket[2] = (byte) PIXY_GETVERSION_TYPE;
        requestPacket[3] = (byte) 0x0;

        byte[] responsePacket = new byte[PIXY_BUFFERSIZE];
        
        if (pixy2.transaction(requestPacket, 0x4, responsePacket, responsePacket.length) == false) {
            System.out.println("yassss");
            SmartDashboard.putRaw("Pixy Versions", responsePacket);
            return true;
        }
    
        System.out.println("aborted :(");
        return false;
    }

    public boolean setLamps(byte state) {
        byte[] requestPacket = new byte[PIXY_BUFFERSIZE];
        requestPacket[0] = (byte) (PIXY_NO_CHECKSUM_SYNC & 0xFF);
        requestPacket[1] = (byte) ((PIXY_NO_CHECKSUM_SYNC >> 8) & 0xFF);
        requestPacket[2] = (byte) 0x16;
        requestPacket[3] = (byte) 0x2;
        requestPacket[4] = (byte) state;
        requestPacket[5] = (byte) 0x0;

        byte[] responsePacket = new byte[PIXY_BUFFERSIZE];

        if(pixy2.transaction(requestPacket, 0x6, responsePacket, responsePacket.length) == false) {
            SmartDashboard.putRaw("Pixy Versions", responsePacket);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sends the GetBlocks command to Pixy2 and waits for the GetBlocks response.
     * This method uses the I2C transaction method to send and receive packets.
     */
    public boolean getBlocks(PixyPacket[] pixyPacketArray, byte signature, int numBlocks)
    {
        byte requestPacket[] = new byte[PIXY_BUFFERSIZE];
        requestPacket[0] = (byte) (PIXY_NO_CHECKSUM_SYNC & 0xff);
        requestPacket[1] = (byte) ((PIXY_NO_CHECKSUM_SYNC >> 8) & 0xff);
        requestPacket[2] = (byte) PIXY_GETBLOCKS_TYPE;
        requestPacket[3] = (byte) 0x02; // length of payload
        requestPacket[4] = (byte) signature;
        requestPacket[5] = (byte) numBlocks; // max blocks to return

        PixyPacket pixyPacket = null;
        byte responsePacket[] = new byte[PIXY_BUFFERSIZE];

        if (pixy2.transaction(requestPacket, 0x6, responsePacket, responsePacket.length) == false) {
            SmartDashboard.putRaw("PIXY CCC Blocks", responsePacket);
            for (int i = 0; i < numBlocks; i++) {
                pixyPacketArray[i] = new PixyPacket();
                pixyPacket = pixyPacketArray[i];
                if (responsePacket[2] == PIXY_RESPONSE_BLOCKS) {
                  pixyPacket.setSignature(bytesToInt(responsePacket[8 + i * RESPONSE_BLOCK_LENGTH], 
                    responsePacket[7 + i * RESPONSE_BLOCK_LENGTH]));
                  pixyPacket.setX(bytesToInt(responsePacket[10 + RESPONSE_BLOCK_LENGTH * i], 
                    responsePacket[9 + RESPONSE_BLOCK_LENGTH * i]));
                  pixyPacket.setY(bytesToInt(responsePacket[12 + RESPONSE_BLOCK_LENGTH * i], 
                    responsePacket[11 + RESPONSE_BLOCK_LENGTH * i]));
                  pixyPacket.setWidth(bytesToInt(responsePacket[14 + RESPONSE_BLOCK_LENGTH * i], 
                    responsePacket[13 + RESPONSE_BLOCK_LENGTH * i]));
                  pixyPacket.setHeight(bytesToInt(responsePacket[16 + RESPONSE_BLOCK_LENGTH * i], 
                    responsePacket[15 + RESPONSE_BLOCK_LENGTH * i]));
                }
            }
            return true;
        }

        return false;
    }
}