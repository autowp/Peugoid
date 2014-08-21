package com.autowp.peugoid;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.autowp.can.CanAdapterException;
import com.autowp.can.CanFrameException;
import com.autowp.canhacker.CanHacker;
import com.autowp.canhacker.command.BitRateCommand;
import com.autowp.canhacker.command.Command;
import com.autowp.canhacker.command.CommandException;
import com.autowp.canhacker.command.OperationalModeCommand;
import com.autowp.canhacker.command.ResetModeCommand;
import com.autowp.canhacker.command.BitRateCommand.BitRate;
import com.autowp.canhacker.response.Response;
import com.autowp.canhacker.response.ResponseException;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;


public class CanHackerUsb extends CanHacker {
    
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbSerialDriver mDriver;
    private UsbSerialPort mPort;
    
    private SerialInputOutputManager mSerialIoManager;
    
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        private byte[] buffer = new byte[1024];
        private int bufferPos = 0;

        @Override
        public void onRunError(Exception e) {
            e.printStackTrace();
        }

        @Override
        public synchronized void onNewData(final byte[] bytes) {
            //System.out.println("Response " + Hex.encodeHexString(bytes));
            for (int i = 0; i<bytes.length; i++) {
                char dataChar = (char)bytes[i];
                if (dataChar != COMMAND_DELIMITER) {
                    buffer[bufferPos++] = (byte)bytes[i];
                }
                
                if (dataChar == COMMAND_DELIMITER || dataChar == BELL) { 
                    if (bufferPos > 0) {
                        byte[] commandBytes = new byte[bufferPos];
                        System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
                        try {
                            Response response = Response.fromBytes(commandBytes);
                            fireResponseReceivedEvent(response);
                        } catch (CanFrameException e) {
                            fireErrorEvent(new CanHackerUsbException("CanFrame error: " + e.getMessage()));
                        } catch (ResponseException e) {
                            fireErrorEvent(new CanHackerUsbException("Response error: " + e.getMessage())); 
                        }
                    }
                    bufferPos = 0;
                }
                
                byte[] commandBytes = new byte[bufferPos];
                System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
            }
        }
    };
    
    public CanHackerUsb(UsbManager usbManager, UsbDevice usbDevice) throws CanHackerUsbException {
        super();
        
        mUsbManager = usbManager;
        mUsbDevice = usbDevice;
    }
    
    public static boolean isCanHacker(UsbDevice device)
    {
        return (device.getVendorId() == VID) && 
               (device.getProductId() == PID);
    }
    
    @Override
    public synchronized void connect() throws CanAdapterException {
        
        mDriver = UsbSerialProber.getDefaultProber().probeDevice(mUsbDevice);
        
        if (mDriver == null) {
            throw new CanHackerUsbException("Driver not found");
        }
        
        List<UsbSerialPort> ports = mDriver.getPorts();
        
        if (ports.size() != 1) {
            throw new CanHackerUsbException("Can hacker must have 1 port");
        }
        
        mPort = ports.get(0);
        
        UsbDeviceConnection connection = mUsbManager.openDevice(mPort.getDriver().getDevice());
        if (connection == null) {
            throw new CanHackerUsbException("Opening device failed");
        }
            
        try {
            mPort.open(connection);
            mPort.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            throw new CanHackerUsbException("I/O error: " + e.getMessage());
        } 
        
        mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
        mExecutor.submit(mSerialIoManager);
        
        BitRate busSpeed;
        switch (this.specs.getSpeed()) {
            case 10:   busSpeed = BitRate.S0; break;
            case 20:   busSpeed = BitRate.S1; break;
            case 50:   busSpeed = BitRate.S2; break;
            case 100:  busSpeed = BitRate.S3; break;
            case 125:  busSpeed = BitRate.S4; break;
            case 250:  busSpeed = BitRate.S5; break;
            case 500:  busSpeed = BitRate.S6; break;
            case 800:  busSpeed = BitRate.S7; break;
            case 1000: busSpeed = BitRate.S8; break;
            default:
                throw new CanHackerUsbException("Unsupported bus speed");
        }
        
        this.send(new ResetModeCommand());
        try {
            this.send(new BitRateCommand(busSpeed));
        } catch (CommandException e) {
            throw new CanHackerUsbException(e.getMessage());
        }
        this.send(new OperationalModeCommand());
    }

    @Override
    public synchronized void disconnect() {
        if (mDriver != null) {
            try {
                System.out.println("canhackerusb disconnect");
                mSerialIoManager.stop();
                mSerialIoManager = null;
                mPort.close();
                mPort = null;
                mDriver = null;
            } catch (IOException e) {
                fireErrorEvent(new CanHackerUsbException("I/O error: " + e.getMessage()));
            }
        }
        
    }

    @Override
    public boolean isConnected() {
        return mPort != null;
    }
    
    public synchronized CanHackerUsb send(Command c) throws CanHackerUsbException
    {
        if (!this.isConnected()) {
            throw new CanHackerUsbException("CanHacker is not connected");
        }
        
        String command = c.toString() + COMMAND_DELIMITER;
        
        try {
            
            byte[] data = command.getBytes("ISO-8859-1");
            
            int sent = mPort.write(data, 1000);
            
            if (sent != data.length) {
                throw new CanHackerUsbException("bulkTransfer error: sent " + sent + " of " + data.length);
            }
            
            fireCommandSendEvent(c);
            
        } catch (IOException e) {
            throw new CanHackerUsbException("I/O error: " + e.getMessage());
        } catch (CanFrameException e) {
            throw new CanHackerUsbException("Can frame error: " + e.getMessage());
        }
                
        return this;
    }
}
