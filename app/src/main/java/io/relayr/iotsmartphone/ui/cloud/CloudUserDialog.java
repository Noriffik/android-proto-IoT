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
import butterknife.BindView;
import io.relayr.android.RelayrSdk;
import io.relayr.iotsmartphone.R;
import io.relayr.iotsmartphone.ui.utils.UiUtil;
import io.relayr.java.helper.observer.SimpleObserver;
import io.relayr.java.model.User;
import rx.android.schedulers.AndroidSchedulers;

public class CloudUserDialog extends LinearLayout {

    @BindView(R.id.cloud_user_id) TextView mIdTv;
    @BindView(R.id.cloud_user_name) EditText mNameEt;
    @BindView(R.id.cloud_user_email) TextView mEmailTv;

    public CloudUserDialog(Context context) {
        this(context, null);
    }

    public CloudUserDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CloudUserDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private User mUser;

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ButterKnife.bind(this, this);

        RelayrSdk.getUser()
                .timeout(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<User>() {
                    @Override public void error(Throwable e) {
                        Toast.makeText(getContext(), R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                    }

                    @Override public void success(User user) {
                        mUser = user;
                        setInfo(user);
                        setActions();
                    }
                });
    }

    @Override protected void onDetachedFromWindow() {
        updateName();
        super.onDetachedFromWindow();
    }

    private void setInfo(User user) {
        mIdTv.setText(user.getId());
        mNameEt.setText(user.getName());
        mEmailTv.setText(user.getEmail());
    }

    private void setActions() {
        mNameEt.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            updateName();
                            return true;
                        }
                        return false;
                    }
                });
    }

    private void updateName() {
        if (mUser == null) return;

        UiUtil.hideKeyboard(getContext(), mNameEt);
        final String newName = mNameEt.getText().toString();
        if (newName.equals(mUser.getName())) return;

        mUser.update(newName)
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(5, TimeUnit.SECONDS)
                .subscribe(new SimpleObserver<User>() {
                    @Override public void error(Throwable e) {
                        Toast.makeText(getContext(), R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                    }

                    @Override public void success(User updated) {
                        mUser = updated;
                        setInfo(updated);
                        Toast.makeText(getContext(), R.string.username_updated, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
