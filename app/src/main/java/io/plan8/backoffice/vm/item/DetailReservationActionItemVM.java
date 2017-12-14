package io.plan8.backoffice.vm.item;

import android.app.Activity;
import android.databinding.Bindable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.databinding.library.baseAdapters.BR;

import io.plan8.backoffice.ApplicationManager;
import io.plan8.backoffice.manager.RealTimeHandlerManager;
import io.plan8.backoffice.model.api.Action;
import io.plan8.backoffice.util.DateUtil;
import io.plan8.backoffice.vm.ActivityVM;

/**
 * Created by chokwanghwan on 2017. 11. 29..
 */

public class DetailReservationActionItemVM extends ActivityVM {
    private Action action;
    private Handler handler;

    public DetailReservationActionItemVM(Activity activity, Bundle savedInstanceState, final Action action) {
        super(activity, savedInstanceState);
        this.action = action;
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                setCreated();
                Log.e(action.getType() + "-updated : ", "OK");
                this.sendEmptyMessageDelayed(0, 30000);
            }
        };
        RealTimeHandlerManager.getInstance().addActionHandler(handler);
        RealTimeHandlerManager.getInstance().addHandler("action", RealTimeHandlerManager.getInstance().getActionHandler());
        handler.sendEmptyMessage(0);
    }

    @Bindable
    public String getImageUrl() {
        if (null == action || null == action.getCreator()) {
            return "";
        }
        return action.getCreator().getAvatar();
    }

    @Bindable
    public String getAuthor() {
        if (null == action || null == action.getCreator() || null == action.getCreator().getName()) {
            return "이름 없음";
        }
        return action.getCreator().getName();
    }

    @Bindable
    public String getAction() {
        if (null == action) {
            return "";
        }
        return action.getText();
    }

    @Bindable
    public String getCreated() {
        if (null == action) {
            return "";
        }
        return DateUtil.getInstance().getChatTime(action.getCreated());
    }

    public void setCreated() {
        notifyPropertyChanged(BR.created);
    }
}
