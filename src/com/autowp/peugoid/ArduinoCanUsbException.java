package com.autowp.peugoid;

import com.autowp.can.CanAdapterException;

public class ArduinoCanUsbException extends CanAdapterException {

    private static final long serialVersionUID = 1L;

    public ArduinoCanUsbException(String message) {
        super(message);
    }

}
