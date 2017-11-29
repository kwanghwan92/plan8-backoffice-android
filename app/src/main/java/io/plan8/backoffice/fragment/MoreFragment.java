package io.plan8.backoffice.fragment;

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.plan8.backoffice.ApplicationManager;
import io.plan8.backoffice.BR;
import io.plan8.backoffice.R;
import io.plan8.backoffice.SharedPreferenceManager;
import io.plan8.backoffice.adapter.RestfulAdapter;
import io.plan8.backoffice.databinding.FragmentMoreBinding;
import io.plan8.backoffice.model.BaseModel;
import io.plan8.backoffice.model.api.Upload;
import io.plan8.backoffice.model.item.EmptySpaceItem;
import io.plan8.backoffice.model.item.LabelItem;
import io.plan8.backoffice.model.item.MoreProfileItem;
import io.plan8.backoffice.model.item.MoreTeamItem;
import io.plan8.backoffice.vm.MoreFragmentVM;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by SSozi on 2017. 11. 28..
 */

public class MoreFragment extends BaseFragment {
    private FragmentMoreBinding binding;
    private MoreFragmentVM vm;
    private RelativeLayout progressBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        List<BaseModel> testData = new ArrayList<>();
        String userName;

        if (ApplicationManager.getInstance().getMe().getName() != null) {
            userName = ApplicationManager.getInstance().getMe().getName();
        } else {
            userName = "이름없음";
        }

        testData.add(new MoreProfileItem(userName, ApplicationManager.getInstance().getMe().getPhoneNumber()));
        testData.add(new LabelItem("팀 선택"));

        if (ApplicationManager.getInstance().getMe().getTeams() != null && ApplicationManager.getInstance().getMe().getTeams().size() > 0) {
            for (int i = 0; i < ApplicationManager.getInstance().getMe().getTeams().size(); i++) {
                testData.add(new MoreTeamItem(ApplicationManager.getInstance().getMe().getTeams().get(i).getName(), ApplicationManager.getInstance().getMe().getTeams().get(i).getName()));
            }
        }
        testData.add(new EmptySpaceItem(0));
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.fragment_more, container, false);
        vm = new MoreFragmentVM(this, savedInstanceState, testData);
        binding.setVariable(BR.vm, vm);
        binding.executePendingBindings();

        progressBar = binding.moreMenuProgressBar;

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        binding.unbind();
        super.onDestroy();
    }

    public void uploadImage(Uri data) {
        Uri uri = data;
        String imagePath = getRealPathFromURI(uri);

        final Cursor cursor = getActivity().getContentResolver().query(Uri.parse(uri.toString()), null, null, null, null);
        assert cursor != null;
        cursor.moveToNext();
        String absolutePath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        cursor.close();

        File files = new File(absolutePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse(getActivity().getContentResolver().getType(uri)), files);
        MultipartBody.Part multipart = MultipartBody.Part.createFormData("files", files.getName(), requestFile);
        Call<List<Upload>> uploadCall = RestfulAdapter.getInstance().getServiceApi().postUpload("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getActivity()), multipart);
        uploadCall.enqueue(new Callback<List<Upload>>() {
            @Override
            public void onResponse(Call<List<Upload>> call, Response<List<Upload>> response) {

            }

            @Override
            public void onFailure(Call<List<Upload>> call, Throwable t) {

            }
        });
    }

    private String getRealPathFromURI(Uri imageUri) {
        String[] path = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().managedQuery(imageUri, path, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}
