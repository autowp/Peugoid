package com.autowp.peugoid;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class TabsPagerAdapter extends FragmentPagerAdapter {
    
    public static final int INDEX_FRAGMENT = 0;
    public static final int CAN_HACKER_LOG_FRAGMENT = 1;
    public static final int CAN_LOG_FRAGMENT = 2;
    public static final int PARKTRONIC_FRAGMENT = 3;
    public static final int LOG_FRAGMENT = 4;
    
    private List<Fragment> fragments;

    public TabsPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return this.fragments.get(position);
        /*Fragment fragment = null;
        switch (index) {
            case INDEX_FRAGMENT: 
                fragment = new IndexFragment();
                break;
            case CAN_HACKER_LOG_FRAGMENT: 
                fragment = new CanHackerLogFragment();
                break;
            case CAN_LOG_FRAGMENT:
                fragment = new CanLogFragment();
                break;
            case PARKTRONIC_FRAGMENT:
                fragment = new ParktronicFragment();
                break;
        }
 
        return fragment;*/
    }

    @Override
    public int getCount() {
        return this.fragments.size();
    }

}
