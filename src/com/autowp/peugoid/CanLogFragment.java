package com.autowp.peugoid;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.codec.binary.Hex;

import com.autowp.can.CanMessage;
import com.autowp.can.CanClient;
import com.autowp.peugeot.CanComfort;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.ToggleButton;

public class CanLogFragment extends Fragment 
    implements CanClient.OnCanMessageTransferListener
{
    
    public interface OnStateChangedListener {
        public void onCanLogStateChanged(boolean enabled);
    }
    
    protected int mCanMessageIndex = 0;
    private ArrayList<HashMap<String, String>> mFillMaps;
    private SimpleAdapter mAdapter;
    private OnStateChangedListener mStateChangedListener;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mStateChangedListener = (OnStateChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
    }
    
    private void createSimpleAdapter()
    {
        // create the grid item mapping
        String[] from = new String[] {
            "n", 
            "id", 
            "data", 
            "comment"
        };
        int[] to = new int[] { 
            R.id.can_grid_header_n, 
            R.id.can_grid_header_id,
            R.id.can_grid_header_data,
            R.id.can_grid_header_comment
        };
        
        mFillMaps = new ArrayList<HashMap<String, String>>();
        // fill in the grid_item layout
        mAdapter = new SimpleAdapter(getActivity(), mFillMaps, R.layout.can_grid_item, from, to);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        
        ListView listView = (ListView) getView().findViewById(R.id.can_log_listview);
        listView.setAdapter(mAdapter);
        
        ToggleButton peButton = (ToggleButton) getView().findViewById(R.id.tb_can_monitor);
        peButton.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mStateChangedListener.onCanLogStateChanged(isChecked);
            }
        });
        
        Button clearButton = (Button)getView().findViewById(R.id.btn_can_log_clear);
        clearButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().runOnUiThread(new Runnable() { 
                    public void run() {
                        mFillMaps.clear();
                
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        
        createSimpleAdapter();
        
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.can_log_fragment, container, false);
 
        return view;
    }
    
    public static final byte[] idToByteArray(int value) {
        return new byte[] {
            (byte)(value >>> 8),
            (byte)value
        };
    }
    
    @Override
    public void handleCanMessageReceivedEvent(final CanMessage message) {
        getActivity().runOnUiThread(new Runnable() { 
            public void run() {
                
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("n", "" + mCanMessageIndex++);
                map.put("id", new String(Hex.encodeHex(idToByteArray(message.getId()))));
                map.put("data", " > " + new String(Hex.encodeHex(message.getData())));
                map.put("comment", CanComfort.getMessageComment(message));
                
                mFillMaps.add(map);
        
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void handleCanMessageSentEvent(final CanMessage message) {
        getActivity().runOnUiThread(new Runnable() { 
            public void run() {

                HashMap<String, String> map = new HashMap<String, String>();
                map.put("n", "" + mCanMessageIndex++);
                map.put("id", new String(Hex.encodeHex(idToByteArray(message.getId()))));
                map.put("data", " < " + new String(Hex.encodeHex(message.getData())));
                map.put("comment", CanComfort.getMessageComment(message));
                mFillMaps.add(map);
        
                mAdapter.notifyDataSetChanged();
            }
        });
    }
}
