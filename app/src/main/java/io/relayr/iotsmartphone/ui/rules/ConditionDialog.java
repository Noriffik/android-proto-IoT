package io.relayr.iotsmartphone.ui.rules;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.storage.Constants;
import io.relayr.iotsmartphone.storage.Storage;
import io.relayr.iotsmartphone.ui.MainTabActivity;
import io.relayr.iotsmartphone.utils.UiHelper;
import io.relayr.java.model.models.transport.DeviceCommand;
import io.relayr.java.model.models.transport.DeviceReading;

import static io.relayr.iotsmartphone.storage.Constants.DeviceType.PHONE;
import static io.relayr.iotsmartphone.storage.Constants.DeviceType.WATCH;

public class ConditionDialog extends LinearLayout {

    @InjectView(R.id.condition_dialog_list) ListView mListView;
    @InjectView(R.id.condition_dialog_phone) ImageView mPhoneImg;
    @InjectView(R.id.condition_dialog_watch) ImageView mWatchImg;
    @InjectView(R.id.condition_dialog_watch_container) View mWatchContainer;

    private boolean mCondition;
    private List<String> mListItems = new ArrayList<>();
    private List<DeviceReading> mReadings = new ArrayList<>();
    private List<DeviceCommand> mCommands = new ArrayList<>();

    private Object mSelected;
    private Constants.DeviceType mType;
    private OnClickListener mClickListener;

    public ConditionDialog(Context context) {
        this(context, null);
    }

    public ConditionDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConditionDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setUp(Constants.DeviceType type, Object selectedReading, boolean condition, OnClickListener clickListener) {
        this.mType = type;
        this.mSelected = selectedReading;
        this.mCondition = condition;
        this.mClickListener = clickListener;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        if (mCondition && UiHelper.isWearableConnected((MainTabActivity) getContext()))
            mWatchContainer.setVisibility(VISIBLE);

        if (mType == null) onPhoneClicked();
        else if (mType == PHONE) onPhoneClicked();
        else onWatchClicked();
    }

    @OnClick(R.id.condition_dialog_phone)
    public void onPhoneClicked() {
        mType = PHONE;
        mWatchImg.setBackgroundResource(R.drawable.cloud_circle_dark);
        mPhoneImg.setBackgroundResource(R.drawable.cloud_circle);

        setUpListItems();
    }

    @OnClick(R.id.condition_dialog_watch)
    public void onWatchClicked() {
        mType = WATCH;
        mWatchImg.setBackgroundResource(R.drawable.cloud_circle);
        mPhoneImg.setBackgroundResource(R.drawable.cloud_circle_dark);
        setUpListItems();
    }

    private void setUpListItems() {
        mReadings.clear();
        mCommands.clear();
        mListItems.clear();

        if (mCondition)
            for (DeviceReading reading : Storage.instance().loadReadings(mType)) {
                if (reading.getMeaning().equals("location") || reading.getMeaning().equals("touch"))
                    continue;
                mReadings.add(reading);
                mListItems.add(UiHelper.getNameForMeaning(getContext(), reading.getMeaning()));
            }
        else
            for (DeviceCommand command : Storage.instance().loadCommands(mType)) {
                mCommands.add(command);
                mListItems.add(command.getName());
            }

        if (mSelected == null) mSelected = mCondition ? mReadings.get(0) : mCommands.get(0);
        setListAdapter();
    }

    private void setListAdapter() {
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.rule_dialog_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setBackgroundColor(ContextCompat.getColor(getContext(),
                        mListView.isItemChecked(position) ? R.color.primary : R.color.primaryDark));
                v.setGravity(mType == PHONE ? Gravity.LEFT : Gravity.RIGHT);
                return v;
            }
        };
        adapter.addAll(mListItems);

        mListView.setAdapter(adapter);
        mListView.setItemsCanFocus(true);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelected = mCondition ? mReadings.get(position) : mCommands.get(position);
                adapter.notifyDataSetChanged();
                if (mClickListener != null) mClickListener.onClick(null);
            }
        });
    }

    public Constants.DeviceType getType() {
        return mType;
    }

    public Object getSelected() {
        return mSelected;
    }
}
