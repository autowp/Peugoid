package com.autowp.peugoid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.autowp.can.*;
import com.autowp.peugeot.*;
import com.autowp.peugeot.parktronic.*;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;



public class MainActivity extends FragmentActivity 
    implements CanHackerLogFragment.OnStateChangedListener, 
        CanLogFragment.OnStateChangedListener, TabListener, 
        IndexFragment.OnConnectedStateChangeListener, 
        IndexFragment.OnIgnitionEmulateStateChangeListener,
        IndexFragment.OnParktronicEmulateStateChangeListener,
        CanClient.OnClientConnectedStateChangeListener
{
    
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    
    private CanClient mCanClient;
 
    private UsbManager mUsbManager;

    //private CanHackerUsb mCanHacker;
    
    private String[] mTabs = { "Index", "CanHacker", "Can", "Parktronic", "Log" };

    private ViewPager mViewPager;
    
    private boolean emulationStarted = false;
    
    private ParktronicEmulation mParktronicEmulator = null;

    private TabsPagerAdapter mTabsAdapter;
    
    private boolean mConnected = false;
    
    private boolean mCanLogEnabled = false;
    
    private boolean mCanHackerLogEnabled = false;

    private ArduinoCanUsb mArduinoCan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_main);
        
        // Initialization
        mViewPager = (ViewPager) findViewById(R.id.pager);
        final ActionBar actionBar = getActionBar();
        
        List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this, IndexFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, CanHackerLogFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, CanLogFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, ParktronicFragment.class.getName()));
        fragments.add(Fragment.instantiate(this, LogFragment.class.getName()));
        
        mTabsAdapter = new TabsPagerAdapter(getSupportFragmentManager(), fragments);
 
        mViewPager.setAdapter(mTabsAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
 
        // Adding Tabs
        for (String tab_name : mTabs) {
            actionBar.addTab(actionBar.newTab().setText(tab_name)
                    .setTabListener(this));
        }
        
        /**
         * on swiping the viewpager make respective tab selected
         * */
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
 
            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionBar.setSelectedNavigationItem(position);
            }
 
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }
 
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
        
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbPermissionReceiver, filter);
        
        IntentFilter attachedFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbAttachedReceiver, attachedFilter);
        
        IntentFilter detachedFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbDetachedReceiver, detachedFilter);
    }

    public void onDestroy() {
        super.onDestroy();
        
        unregisterReceiver(mUsbPermissionReceiver);
        unregisterReceiver(mUsbAttachedReceiver);
        unregisterReceiver(mUsbDetachedReceiver);
    }
    
    public void resetDevice()
    {
        System.out.println("resetDevice");
        if (mCanClient != null) {
            mCanClient.disconnect();
            mCanClient = null;
            
        }
    }
    
    public void setDevice(UsbDevice device) throws CanClientException
    {
        resetDevice();
        
        System.out.println("setDevice " + device.getDeviceName());
        
        if (!mUsbManager.hasPermission(device)) {
            System.out.println("requestPermission " + device.getDeviceName());
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(device, mPermissionIntent);
        } else {
            System.out.println("hasPermission " + device.getDeviceName());
            
            //mCanHacker = new CanHackerUsb(mUsbManager, device);
            mArduinoCan = new ArduinoCanUsb(mUsbManager, device);
            
            CanComfortSpecs canComfortSpecs = new CanComfortSpecs();
            
            mCanClient = new CanClient(canComfortSpecs);
            mCanClient.setAdapter(mArduinoCan);
            
            mCanClient.connect();
            
            Parktronic parktronic = new Parktronic(mCanClient);
            
            ParktronicFragment parktronicFragment = (ParktronicFragment) mTabsAdapter.getItem(TabsPagerAdapter.PARKTRONIC_FRAGMENT);
            
            parktronic.addEventListener(parktronicFragment);
            
        }
    }
    
    private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            System.out.println("Persmission braodcast");
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null){
                            System.out.println("Permissions granted for device " + device.getDeviceName());
                            
                            try {
                                setDevice(device);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println("permission denied for device " + device.getDeviceName());
                    } 
                }
            }
        }
    };
    
    private final BroadcastReceiver mUsbAttachedReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            System.out.println("UsbAttachedReceiver.onReceive");
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                
                System.out.println("BroadcastReceiver USB Connected");
                System.out.println(device);
                System.out.println(device.getProductId());
                System.out.println(device.getVendorId());
                
                if (ArduinoCanUsb.isArduinoCan(device)) {
                    try {
                        setDevice(device);
                    } catch (CanClientException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        }
    };
    
    private final BroadcastReceiver mUsbDetachedReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                
                System.out.println("BroadcastReceiver USB Disconnected");
                System.out.println(device);
                
                resetDevice();
            }
        }
    };

    private boolean mIgnitionEmulateEnabled = false;

    private boolean mParktronicEmulateEnabled = false;

    @Override
    public void onCanHackerLogStateChanged(boolean enabled) {
        
        if (mCanHackerLogEnabled != enabled) {
            
            mCanHackerLogEnabled = enabled;
            
            /*CanHackerLogFragment canHackerLogFragment = (CanHackerLogFragment) mTabsAdapter.getItem(TabsPagerAdapter.CAN_HACKER_LOG_FRAGMENT);
            
            if (enabled) {
                mCanHacker.addEventListener((CanHacker.OnCommandSentListener)canHackerLogFragment);
                mCanHacker.addEventListener((CanHacker.OnResponseReceivedListener)canHackerLogFragment);
            } else {
                mCanHacker.removeEventListener((CanHacker.OnCommandSentListener)canHackerLogFragment);
                mCanHacker.removeEventListener((CanHacker.OnResponseReceivedListener)canHackerLogFragment);
            }*/
        }
        
    }

    @Override
    public void onCanLogStateChanged(boolean enabled) {
        
        if (enabled != mCanLogEnabled) {
            
            mCanLogEnabled = enabled;
        
            CanLogFragment canLogFragment = (CanLogFragment) mTabsAdapter.getItem(TabsPagerAdapter.CAN_LOG_FRAGMENT);
            
            if (mCanClient != null) {
                if (enabled) {
                    mCanClient.addEventListener(canLogFragment);
                } else {
                    mCanClient.removeEventListener(canLogFragment);
                }
            }
        }
        
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        mViewPager.setCurrentItem(tab.getPosition(), true);
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleConnectedStateChange(boolean isConnected) {
        System.out.println("handleConnectedStateChange");
        if (mConnected != isConnected) {
            
            mConnected = isConnected;
        
            if (isConnected) {
                System.out.println("Connect"); 
                
                HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
                
                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        
                while(deviceIterator.hasNext()) {
                    UsbDevice usbDevice = deviceIterator.next();
                    System.out.println(usbDevice.getProductId());
                    System.out.println(usbDevice.getVendorId());
                    System.out.println(usbDevice.getDeviceName());
                    System.out.println(usbDevice);
                    if (ArduinoCanUsb.isArduinoCan(usbDevice)) {
                        try {
                            setDevice(usbDevice);
                        } catch (CanClientException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    }
                }
                
                IndexFragment indexFragment = (IndexFragment) mTabsAdapter.getItem(TabsPagerAdapter.INDEX_FRAGMENT);

                if (indexFragment != null) {
                    indexFragment.refreshButton(isConnected);
                }
                
                /*if (!isConnected) {
                    System.out.println("Disconnected");
                    if (mCanHacker != null) {
                        mCanHacker.disconnect();
                        mCanHacker = null;
                    }
                } else {
                    System.out.println("Connected");
                }*/
            } else {
                
                System.out.println("handleDisconnectClick");
                
                resetDevice();
                
            }
        }
    }

    @Override
    public void handleIgnitionEmulateStateChange(boolean isEnabled) {
        if (mIgnitionEmulateEnabled != isEnabled) {
            mIgnitionEmulateEnabled = isEnabled;
            System.out.println("handleIgnitionEmulateStateChange");
            if (isEnabled) {
                try {
                    if (emulationStarted) {
                        System.out.println("Stop emulate");
                        mCanClient.stopTimers();
                        emulationStarted = false;
                    } else {
                        if (mCanClient != null) {
                            System.out.println("Start emulate");
                            CanComfort.emulateBSIIgnition(mCanClient, "21496464");
                            emulationStarted = true;
                        } else {
                            System.out.println("unable to emulate on null client");
                        }
                    }
                } catch (CanComfortException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
    }

    @Override
    public void handleParktronicEmulateStateChange(boolean isEnabled) {
        if (mParktronicEmulateEnabled != isEnabled) {
            mParktronicEmulateEnabled = isEnabled;
            System.out.println("handleParktronicEmulateStateChange");
            if (mParktronicEmulator != null) {
                mParktronicEmulator.stop();
                mParktronicEmulator = null;
            }
            
            if (isEnabled) {
                mParktronicEmulator = new ParktronicEmulation(mCanClient, true);
                mParktronicEmulator.start();
            }
        }
    }

    @Override
    public void handleClientConnectedStateChanged(boolean isConnected) {
        IndexFragment indexFragment = (IndexFragment) mTabsAdapter.getItem(TabsPagerAdapter.INDEX_FRAGMENT);

        if (indexFragment != null) {
            indexFragment.refreshButton(isConnected);
        }
        
    }

}
