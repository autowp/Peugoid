package com.autowp.peugoid;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.autowp.HexHelper;
import com.autowp.HexHelper.HexException;
import com.autowp.arduinocan.ArduinoCan;
import com.autowp.arduinocan.ArduinoCanException;
import com.autowp.can.CanAdapter;
import com.autowp.can.CanAdapterException;
import com.autowp.can.CanFrame;
import com.autowp.can.CanFrameException;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class ArduinoCanUsb extends ArduinoCan {
    
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbSerialDriver mDriver;
    private UsbSerialPort mPort;
    
    private SerialInputOutputManager mSerialIoManager;
    
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    
    public ArduinoCanUsb(UsbManager usbManager, UsbDevice usbDevice) {
        super();
        
        mUsbManager = usbManager;
        mUsbDevice = usbDevice;
    }
    
    public static boolean isArduinoCan(UsbDevice device)
    {
        return (device.getVendorId() == ARDUINO_USB_VENDOR_ID);
    }
    
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
                if (dataChar != '\n' && dataChar != '\r') {
                    buffer[bufferPos++] = (byte)bytes[i];
                }
                System.out.println("onNewData");
                
                if (dataChar == '\n' || dataChar == '\r') {
                    if (bufferPos > 0) {
                        byte[] commandBytes = new byte[bufferPos];
                        System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
                        
                        try {
                            CanFrame frame = parseArduinoCanMessage(buffer);
                            fireFrameReceivedEvent(frame);
                        } catch (ArduinoCanException e) {
                            fireErrorEvent(e);
                        }
                        
                    }
                    bufferPos = 0;
                }
                
                byte[] commandBytes = new byte[bufferPos];
                System.arraycopy(buffer, 0, commandBytes, 0, bufferPos);
            }
        }
    };
    
    @Override
    public void send(CanFrame frame) throws CanAdapterException {
        if (!this.isConnected()) {
            throw new ArduinoCanUsbException("ArduinoCan is not connected");
        }
        
        byte[] data = buildArduinoCanMessage(frame);
        
        if (mPort == null) {
            throw new ArduinoCanUsbException("Port is null");
        }
        
        try {
        
            System.out.println(mUsbDevice);
            System.out.println(mUsbDevice.getProductId());
            System.out.println(mUsbDevice.getVendorId());
            System.out.println(mUsbDevice.getInterfaceCount());
            System.out.println(mUsbDevice.getInterface(0));
            
            int sent = mPort.write(data, 1000);
            
            if (sent != data.length) {
                throw new ArduinoCanUsbException("bulkTransfer error: sent " + sent + " of " + data.length);
            }
            
        } catch (IOException e) {
            throw new ArduinoCanUsbException("I/O error: " + e.getMessage());
        }
        
    }

    @Override
    public void connect() throws CanAdapterException {
        mDriver = UsbSerialProber.getDefaultProber().probeDevice(mUsbDevice);
        
        if (mDriver == null) {
            throw new ArduinoCanUsbException("Driver not found");
        }
        
        List<UsbSerialPort> ports = mDriver.getPorts();
        
        if (ports.size() != 1) {
            throw new ArduinoCanUsbException("Can adapter must have 1 port");
        }
        
        mPort = ports.get(0);
        
        UsbDeviceConnection connection = mUsbManager.openDevice(mPort.getDriver().getDevice());
        if (connection == null) {
            throw new ArduinoCanUsbException("Opening device failed");
        }
            
        try {
            mPort.open(connection);
            mPort.setParameters(BAUDRATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            throw new ArduinoCanUsbException("I/O error: " + e.getMessage());
        } 
        
        mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
        mExecutor.submit(mSerialIoManager);
    }

    @Override
    public void disconnect() {
        if (mDriver != null) {
            try {
                System.out.println("ArduinoCan disconnect");
                mSerialIoManager.stop();
                mSerialIoManager = null;
                mPort.close();
                mPort = null;
                mDriver = null;
            } catch (IOException e) {
                fireErrorEvent(new ArduinoCanUsbException("I/O error: " + e.getMessage()));
            }
        }
    }

    @Override
    public boolean isConnected() {
        return mPort != null;
    }

}
