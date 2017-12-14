package io.plan8.backoffice.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.plan8.backoffice.BR;
import io.plan8.backoffice.R;
import io.plan8.backoffice.SharedPreferenceManager;
import io.plan8.backoffice.adapter.RestfulAdapter;
import io.plan8.backoffice.databinding.FragmentNotificationBinding;
import io.plan8.backoffice.listener.EndlessRecyclerOnScrollListener;
import io.plan8.backoffice.model.api.Notification;
import io.plan8.backoffice.vm.NotificationFragmentVM;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by SSozi on 2017. 12. 5..
 */

public class NotificationFragment extends BaseFragment {
    private FragmentNotificationBinding binding;
    private NotificationFragmentVM vm;
    private List<Notification> notifications;
    private Handler handler = null;
    private List<Notification> refreshData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.fragment_notification, container, false);
        vm = new NotificationFragmentVM(this, savedInstanceState);
        binding.setVariable(BR.vm, vm);
        binding.executePendingBindings();
        return binding.getRoot();
    }

    public void refreshNotificationList() {
        if (null == notifications) {
            notifications = new ArrayList<>();
        }

        Call<List<Notification>> getNotifications = RestfulAdapter.getInstance().getServiceApi().getNotifications("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getContext()), 15, notifications.size());
        getNotifications.enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                List<Notification> result = response.body();

                if (null != result) {
                    Log.e("notification", "" + result.size());
                    if (notifications.size() + result.size() > notifications.size()) {
                        if (handler == null) {
                            initHandler();
                        }
                        notifications.addAll(result);
                        vm.addData(result);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                Toast.makeText(getContext(), "알림 목록을 받아오는데 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.notificationRecycler.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore(int currentPage) {
                refreshNotificationList();
            }
        });
        refreshNotificationList();

        initHandler();
    }

    private void initHandler() {
        if (notifications != null && notifications.size() != 0) {
            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);

                    vm.setDataNotifyItemRangeChanged(notifications);
                    Log.e("notification : ", "refresh");

                    this.sendEmptyMessageDelayed(0, 30000);
                }
            };
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public void onResume() {
        if (handler != null) {
            handler.sendEmptyMessage(0);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (handler != null) {
            handler.removeMessages(0);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeMessages(0);
        }
        if (null != binding) {
            binding.unbind();
        }
        super.onDestroy();
    }

    public void readAllNotifications() {
        for (Notification n : notifications) {
            if (null != n) {
                n.setRead(true);
            }
        }
        vm.setData(notifications);
    }
}