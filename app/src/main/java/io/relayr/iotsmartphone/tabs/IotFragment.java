package io.relayr.iotsmartphone.tabs;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

public class IotFragment extends Fragment {

    @SuppressWarnings("ConstantConditions")
    public void setTitle(String title) {
        if (title != null && getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null)
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
    }
}
