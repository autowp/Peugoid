package com.autowp.peugoid;

import com.autowp.peugeot.parktronic.Parktronic;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ParktronicFragment extends Fragment
    implements Parktronic.OnParktronicStateChangedListener
{
    private int mDangerColor;
    
    private int mWarningColor;
    
    private int mNormalColor;
    
    private final static int CENTER_POINTS = 4;
    private final static int SIDE_POINTS = 3;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState); 
        
        Resources resources = getResources();
        
        mDangerColor = resources.getColor(R.color.parktronic_danger_color);
        mWarningColor = resources.getColor(R.color.parktronic_warning_color);
        mNormalColor = resources.getColor(R.color.parktronic_normal_color);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.parktronic_fragment, container, false); 
    }

    @Override
    public void handleStateChangedEvent(final Parktronic parktronic) {
        
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() { 
                public void run() {
                    
                    setTextView(R.id.tv_front_center, parktronic.getFrontCenter(), CENTER_POINTS);
                    setTextView(R.id.tv_front_left, parktronic.getFrontLeft(), SIDE_POINTS);
                    setTextView(R.id.tv_front_right, parktronic.getFrontRight(), SIDE_POINTS);
                    setTextView(R.id.tv_rear_center, parktronic.getRearCenter(), CENTER_POINTS);
                    setTextView(R.id.tv_rear_left, parktronic.getRearLeft(), SIDE_POINTS);
                    setTextView(R.id.tv_rear_right, parktronic.getRearRight(), SIDE_POINTS);
                }
            });
        }
         
    }
    
    private void setTextView(int tv_id, int value, int range)
    {
        View view = getView();
        if (view != null) {
            TextView tvFrontCenter = (TextView) view.findViewById(tv_id);
            if (tvFrontCenter != null) {
                tvFrontCenter.setText("" + value);
                tvFrontCenter.setBackgroundColor(getColorByValue(value, range));
            }
        }
    }
    
    private int getColorByValue(int value, int range)
    {
        int result = 0;
        switch (range) {
            case SIDE_POINTS:
                switch (value) {
                    case 0: result = 0x00000000; break;
                    case 1: result = mNormalColor; break;
                    case 2: result = mWarningColor; break;
                    case 3: result = mDangerColor; break;
                }
                break;
            case CENTER_POINTS:
                switch (value) {
                    case 0: result = 0x00000000; break;
                    case 1: result = mNormalColor; break;
                    case 2: result = mWarningColor; break;
                    case 3: result = mWarningColor; break;
                    case 4: result = mDangerColor; break;
                }
                break;
        }
        
        
        return result;
    }
}
