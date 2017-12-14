package io.plan8.backoffice.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.plan8.backoffice.ApplicationManager;
import io.plan8.backoffice.BR;
import io.plan8.backoffice.Constants;
import io.plan8.backoffice.R;
import io.plan8.backoffice.SharedPreferenceManager;
import io.plan8.backoffice.adapter.RestfulAdapter;
import io.plan8.backoffice.databinding.ActivityMainBinding;
import io.plan8.backoffice.fragment.MoreFragment;
import io.plan8.backoffice.fragment.NotificationFragment;
import io.plan8.backoffice.fragment.ReservationFragment;
import io.plan8.backoffice.model.api.Member;
import io.plan8.backoffice.model.api.Reservation;
import io.plan8.backoffice.vm.MainActivityVM;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private MainActivityVM vm;
    private FragmentManager fragmentManager;
    private int currentTabPosition = 0;
    private List<Fragment> fragments = new ArrayList<>();
    private List<Member> members;
    private ReservationFragment reservationFragment;
    private NotificationFragment notificationFragment;

    public static Intent buildIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        vm = new MainActivityVM(this, savedInstanceState);
        binding.setVariable(BR.vm, vm);
        binding.executePendingBindings();

        Call<List<Member>> getUserMembersCall = RestfulAdapter.getInstance().getServiceApi().getUserMembers("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getApplicationContext()));
        getUserMembersCall.enqueue(new Callback<List<Member>>() {
            @Override
            public void onResponse(Call<List<Member>> call, Response<List<Member>> response) {
                members = response.body();
                ApplicationManager.getInstance().setMembers(members);
                if (null == members || members.size() == 0) {
                    vm.setEmptyTeamFlag(true);
                } else {
                    if (null != members.get(0)) {
                        ApplicationManager.getInstance().setCurrentMember(members.get(0));
                        ApplicationManager.getInstance().setCurrentTeam(members.get(0).getTeam());
                    }
                    vm.setEmptyTeamFlag(false);
                    initTabAndViewPager();
                }
            }

            @Override
            public void onFailure(Call<List<Member>> call, Throwable t) {
                Log.e("api : ", "failure");
            }
        });
    }

    private void initTabAndViewPager() {
        for (int i = 0; i < 3; i++) {
            TabLayout.Tab tab = binding.mainTabLayout.newTab();
            binding.mainTabLayout.setSelectedTabIndicatorHeight(0);
            tab.setCustomView(R.layout.item_main_tab);

            if (null != tab.getCustomView()) {
                AppCompatImageView tabItemIcon = tab.getCustomView().findViewById(R.id.mainTabItemIcon);
                AppCompatTextView tabItemTitle = tab.getCustomView().findViewById(R.id.mainTabItemTitle);
                if (i == 0) {
                    tabItemIcon.setImageResource(R.drawable.ic_line_calendar);
                    tabItemIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.selectTabItem));

                    tabItemTitle.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.selectTabItem));
                    tabItemTitle.setText("예약");

                    reservationFragment = new ReservationFragment();
                    Bundle bundle = new Bundle();
//        bundle.putSerializable("dynamicUiConfiguration", dynamicUiConfigurations.get(i))
                    reservationFragment.setArguments(bundle);
                    fragments.add(reservationFragment);
                } else if (i == 1) {
                    tabItemIcon.setImageResource(R.drawable.ic_line_alarm);
                    tabItemTitle.setText("알림");

                    notificationFragment = new NotificationFragment();
                    Bundle bundle = new Bundle();
//        bundle.putSerializable("dynamicUiConfiguration", dynamicUiConfigurations.get(i))
                    notificationFragment.setArguments(bundle);
                    fragments.add(notificationFragment);
                } else {
                    tabItemIcon.setImageResource(R.drawable.ic_solid_more);
                    tabItemTitle.setText("더보기");

                    MoreFragment moreFragment = new MoreFragment();
                    Bundle bundle = new Bundle();
//        bundle.putSerializable("dynamicUiConfiguration", dynamicUiConfigurations.get(i))
                    moreFragment.setArguments(bundle);
                    fragments.add(moreFragment);
                }
                //                        tab.getCustomView().setLayoutParams(params);

                binding.mainTabLayout.addTab(tab);
            }
        }

        binding.mainViewPager.setOffscreenPageLimit(fragments.size());

        fragmentManager = getSupportFragmentManager();
        FragmentStatePagerAdapter pagerAdapter = new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                return fragments.get(position);
            }

            @Override
            public int getCount() {
                return fragments.size();
            }
        };
        binding.mainViewPager.setAdapter(pagerAdapter);
        binding.mainViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.mainTabLayout));
        binding.mainTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                binding.mainViewPager.setCurrentItem(tab.getPosition());
                if (tab.getCustomView() != null) {
                    ((AppCompatImageView) tab.getCustomView().findViewById(R.id.mainTabItemIcon)).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.selectTabItem));
                    ((AppCompatTextView) tab.getCustomView().findViewById(R.id.mainTabItemTitle)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.selectTabItem));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab != null) {
                    ((AppCompatImageView) tab.getCustomView().findViewById(R.id.mainTabItemIcon)).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.unselectTabItem));
                    ((AppCompatTextView) tab.getCustomView().findViewById(R.id.mainTabItemTitle)).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.unselectTabItem));
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                if (data.getAction() == null) {
                    if (null != fragments && null != fragments.get(2) && fragments.get(2) instanceof MoreFragment) {
                        ((MoreFragment) fragments.get(2)).uploadImage(data.getData());
                    }
                } else {
                    if (null != fragments && null != fragments.get(2) && fragments.get(2) instanceof MoreFragment) {
                        ((MoreFragment) fragments.get(2)).uploadImage(getImageUri(getApplicationContext(), (Bitmap) data.getExtras().get("data")));
                    }
                }
                break;
            case Constants.REFRESH_RESERVATION_FRAGMENT:
                reservationFragment.setEditFlag(true);
                reservationFragment.editItem((Reservation) data.getSerializableExtra("reservation"));
                break;
            default:
                break;
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    protected void onDestroy() {
        binding.unbind();
        ApplicationManager.getInstance().setMainActivity(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        ApplicationManager.getInstance().setMainActivity(this);
        super.onResume();
    }

    @Override
    public void onBackPressed(boolean canDoubleClickFinish) {
        super.onBackPressed(canDoubleClickFinish);
    }

    public void setEmptyFlag(boolean flag) {
        vm.setEmptyTeamFlag(flag);
    }

    public NotificationFragment getNotificationFragment() {
        return notificationFragment;
    }
}