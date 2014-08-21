package com.autowp.peugoid;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

public class IndexFragment extends Fragment {
    
    public interface OnConnectedStateChangeListener {
        public void handleConnectedStateChange(boolean isConnected);
    }
    
    public interface OnIgnitionEmulateStateChangeListener {
        public void handleIgnitionEmulateStateChange(boolean isEnabled);
    }
    
    public interface OnParktronicEmulateStateChangeListener {
        public void handleParktronicEmulateStateChange(boolean isEnabled);
    }
    
    private OnConnectedStateChangeListener mConnectedStateChangeListener;
    
    private OnIgnitionEmulateStateChangeListener mIgnitionEmulateStateChangeListener;
    
    private OnParktronicEmulateStateChangeListener mParktronicEmulateStateChangeListener;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        System.out.println("onAttach");
        try {
            mConnectedStateChangeListener = (OnConnectedStateChangeListener) activity;
            mIgnitionEmulateStateChangeListener = (OnIgnitionEmulateStateChangeListener) activity;
            mParktronicEmulateStateChangeListener = (OnParktronicEmulateStateChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnConnectedStateChangeListener and OnIgnitionEmulateStateChangeListener");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        System.out.println("onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.index_fragment, container, false);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        System.out.println("onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        setupControls();
    }
    
    public void refreshButton(boolean canClientEnabled)
    {
        System.out.println("Set checked");
        System.out.println(canClientEnabled);
        
        Switch connectButton = (Switch) getView().findViewById(R.id.switch_connect);
        if (connectButton != null) {
            connectButton.setChecked(canClientEnabled);
        }
        
        Switch buttonOne = (Switch) getView().findViewById(R.id.switch_ignition_emulate);
        if (buttonOne != null) {
            buttonOne.setEnabled(canClientEnabled);
        }
        
        Switch peButton = (Switch) getView().findViewById(R.id.switch_parktronic_emulator);
        if (peButton != null) {
            peButton.setEnabled(canClientEnabled);
        }
    }
    
    private void setupControls()
    {
        Switch connectButton = (Switch) getView().findViewById(R.id.switch_connect);
        connectButton.setOnClickListener(new Switch.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("onCLick");
                System.out.println(((Switch)v).isChecked());
                mConnectedStateChangeListener.handleConnectedStateChange(((Switch)v).isChecked());
            }
        });
        
        Switch buttonOne = (Switch) getView().findViewById(R.id.switch_ignition_emulate);
        buttonOne.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIgnitionEmulateStateChangeListener.handleIgnitionEmulateStateChange(isChecked);
            }
        });
        
        Switch peButton = (Switch) getView().findViewById(R.id.switch_parktronic_emulator);
        peButton.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mParktronicEmulateStateChangeListener.handleParktronicEmulateStateChange(isChecked);
            }
            
        });
        
    }
}
