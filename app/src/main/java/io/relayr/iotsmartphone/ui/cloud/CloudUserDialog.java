package io.relayr.iotsmartphone.ui.cloud;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.User;
import rx.android.schedulers.AndroidSchedulers;

public class CloudUserDialog extends LinearLayout {

    @InjectView(R.id.cloud_user_id) TextView mIdTv;
    @InjectView(R.id.cloud_user_name) EditText mNameEt;
    @InjectView(R.id.cloud_user_email) TextView mEmailTv;

    public CloudUserDialog(Context context) {
        this(context, null);
    }

    public CloudUserDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CloudUserDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.inject(this, this);

        RelayrSdk.getUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<User>() {
                    @Override public void error(Throwable e) {
                        Toast.makeText(getContext(), "something went wrong", Toast.LENGTH_LONG).show();
                    }

                    @Override public void success(User user) {
                        setInfo(user);
                    }
                });
    }

    private void setInfo(User user) {
        mIdTv.setText(user.getId());
        mNameEt.setText(user.getName());
        mEmailTv.setText(user.getEmail());

        setActions(user);
    }

    private void setActions(final User user) {
        mNameEt.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        UiUtil.hideKeyboard(getContext(), mNameEt);
                        final String newName = mNameEt.getText().toString();
                        if (newName.equals(user.getName())) return false;

                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            user.update(newName)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .timeout(5, TimeUnit.SECONDS)
                                    .subscribe(new SimpleObserver<User>() {
                                        @Override public void error(Throwable e) {
                                            Toast.makeText(getContext(), "something went wrong", Toast.LENGTH_LONG).show();
                                        }

                                        @Override public void success(User updated) {
                                            setInfo(updated);
                                            Toast.makeText(getContext(), "User name updated", Toast.LENGTH_LONG).show();
                                        }
                                    });
                            return true;
                        }
                        return false;
                    }
                });
    }
}
