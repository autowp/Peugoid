package com.autowp.peugoid;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.ToggleButton;

import com.autowp.canhacker.CanHacker;
import com.autowp.canhacker.command.Command;
import com.autowp.canhacker.response.Response;

public class CanHackerLogFragment extends Fragment
    implements CanHacker.OnCommandSentListener, CanHacker.OnResponseReceivedListener
{
    
    public interface OnStateChangedListener {
        public void onCanHackerLogStateChanged(boolean enabled);
    }
    
    protected int mCanCommandIndex = 0;
    
    private OnStateChangedListener mStateChangedListener;

    private ArrayList<HashMap<String, String>> mFillMaps;

    private SimpleAdapter mAdapter;
    
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
            "data"
        };
        int[] to = new int[] { 
            R.id.canhacker_grid_header_n, 
            R.id.canhacker_grid_header_data 
        };
        
        mFillMaps = new ArrayList<HashMap<String, String>>();
        // fill in the grid_item layout
        mAdapter = new SimpleAdapter(getActivity(), mFillMaps, R.layout.canhacker_grid_item, from, to);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        
        ListView mListView = (ListView) getView().findViewById(R.id.cahnhacker_log_listview);
        mListView.setAdapter(mAdapter);
        
        ToggleButton peButton = (ToggleButton) getView().findViewById(R.id.tb_canhacker_monitor);
        peButton.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {

                mStateChangedListener.onCanHackerLogStateChanged(isChecked);

            }
            
        });
        
        Button clearButton = (Button)getView().findViewById(R.id.btn_canhacker_log_clear);
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
        
        return inflater.inflate(R.layout.canhacker_log_fragment, container, false); 
    }

    @Override
    public void handleResponseReceivedEvent(final Response response) {
        FragmentActivity activity = getActivity();
        if (activity != null) { 
            activity.runOnUiThread(new Runnable() { 
                public void run() {
    
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("n", "" + mCanCommandIndex++);
                    map.put("data", " < " + response.toString());
                    mFillMaps.add(map);
            
            
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void handleCommandSentEvent(final Command command) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() { 
                public void run() {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("n", "" + mCanCommandIndex++);
                    map.put("data", " > " + command.toString());
                    mFillMaps.add(map);
            
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }
}
