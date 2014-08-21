package com.autowp.peugoid;

import java.util.ArrayList;
import java.util.HashMap;
import com.autowp.can.CanClient;
import com.autowp.can.CanClientException;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class LogFragment extends Fragment implements CanClient.OnCanClientErrorListener
{
    private ArrayList<HashMap<String, String>> mFillMaps;

    private SimpleAdapter mAdapter;
    
    private void createSimpleAdapter()
    {
        // create the grid item mapping
        String[] from = new String[] {
            "text"
        };
        int[] to = new int[] { 
            R.id.log_grid_header_text
        };
        
        mFillMaps = new ArrayList<HashMap<String, String>>();
        // fill in the grid_item layout
        mAdapter = new SimpleAdapter(getActivity(), mFillMaps, R.layout.log_grid_item, from, to);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        
        ListView mListView = (ListView) getView().findViewById(R.id.log_listview);
        mListView.setAdapter(mAdapter);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        
        createSimpleAdapter();
        
        return inflater.inflate(R.layout.log_fragment, container, false); 
    }
    
    @Override
    public void handleErrorEvent(final CanClientException e) {
        getActivity().runOnUiThread(new Runnable() { 
            public void run() {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("text", e.getMessage());
                mFillMaps.add(map);
        
                mAdapter.notifyDataSetChanged();
            }
        });
    }
}
