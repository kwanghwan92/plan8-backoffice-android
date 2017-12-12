package io.plan8.backoffice.activity;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsResultListener;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizerConfig;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.ui.MentionsEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.plan8.backoffice.ApplicationManager;
import io.plan8.backoffice.BR;
import io.plan8.backoffice.BuildConfig;
import io.plan8.backoffice.Constants;
import io.plan8.backoffice.R;
import io.plan8.backoffice.SharedPreferenceManager;
import io.plan8.backoffice.adapter.RestfulAdapter;
import io.plan8.backoffice.databinding.ActivityDetailReservationBinding;
import io.plan8.backoffice.model.BaseModel;
import io.plan8.backoffice.model.api.Attachment;
import io.plan8.backoffice.model.api.Comment;
import io.plan8.backoffice.model.api.Member;
import io.plan8.backoffice.model.api.Reservation;
import io.plan8.backoffice.model.api.User;
import io.plan8.backoffice.model.item.DetailReservationMoreButtonItem;
import io.plan8.backoffice.util.DateUtil;
import io.plan8.backoffice.vm.DetailReservationActivityVM;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailReservationActivity extends BaseActivity implements SuggestionsResultListener {
    private ActivityDetailReservationBinding binding;
    private DetailReservationActivityVM vm;
    private Uri captureImageUri;
    private long fileLength;
    private Reservation reservation;
    private int reservationId;
    private MentionsEditText mentionsEditText;
    private User.UserLoader userLoader;
    private static final String BUCKET = "user";
    private boolean isAlreadyReplaceMention;
    private List<Comment> comments;
    private List<BaseModel> detailReservations;
    private List<BaseModel> tempList;
    private boolean editFlag = false;
    private Uri photoURI;
    private static final WordTokenizerConfig tokenizerConfig = new WordTokenizerConfig
            .Builder()
            .setMaxNumKeywords(1)
            .build();

    public static Intent buildIntent(Context context, Reservation reservation) {
        Intent intent = new Intent(context, DetailReservationActivity.class);
        intent.putExtra("reservation", reservation);
        return intent;
    }

    public static Intent buildIntent(Context context, String openUrl, boolean deepLinkFlag) {
        Intent intent = new Intent(context, DetailReservationActivity.class);
        intent.putExtra("openUrl", openUrl);
        intent.putExtra("deepLinkFlag", deepLinkFlag);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.reservation = (Reservation) getIntent().getSerializableExtra("reservation");

        if (getIntent().getBooleanExtra("deepLinkFlag", false)) {
            Intent deepLinkData = getIntent();

            if (getIntent().getData() != null) {
                reservationId = Integer.parseInt(deepLinkData.getData().getPath().replace("/", "").trim());
            } else {
                Uri deepLinkUri = Uri.parse(deepLinkData.getStringExtra("openUrl"));
                reservationId = Integer.parseInt(deepLinkUri.getPath().replace("/", "").trim());
            }
        } else {
            reservationId = reservation.getId();
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail_reservation);
        vm = new DetailReservationActivityVM(this, savedInstanceState);
        binding.setVariable(BR.vm, vm);
        binding.executePendingBindings();

        setMentionEditText(ApplicationManager.getInstance().getCurrentTeamMembers());
        refreshReservation();
    }

    private void setMentionEditText(List<Member> memberList) {
        List<User> userList = new ArrayList<>();
        for (Member m : memberList) {
            if (null != m) {
                userList.add(m.getUser());
            }
        }

        userLoader = new User.UserLoader(userList);
        mentionsEditText = findViewById(R.id.mentionEditText);
        mentionsEditText.setTokenizer(new WordTokenizer(tokenizerConfig));
        mentionsEditText.setQueryTokenReceiver(new QueryTokenReceiver() {
            @Override
            public List<String> onQueryReceived(@NonNull QueryToken queryToken) {
                List<String> buckets = Arrays.asList(BUCKET);
                List<User> suggestions = userLoader.getSuggestions(queryToken);
                SuggestionsResult result = new SuggestionsResult(queryToken, suggestions);
                // Have suggestions, now call the listener (which is this activity)
                onReceiveSuggestionsResult(result, BUCKET);
                return buckets;
            }
        });
        mentionsEditText.setSuggestionsVisibilityManager(new SuggestionsVisibilityManager() {
            @Override
            public void displaySuggestions(boolean display) {

            }

            @Override
            public boolean isDisplayingSuggestions() {
                return false;
            }
        });
    }

    @Override
    public void onReceiveSuggestionsResult(@NonNull SuggestionsResult result, @NonNull String bucket) {
        if (isAlreadyReplaceMention) {
            return;
        }
        List<User> userList = (List<User>) result.getSuggestions();
        vm.setAutoCompleteMentionData(userList);
    }

    @Override
    protected void onDestroy() {
        binding.unbind();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (ApplicationManager.getInstance().getMainActivity() != null) {
            Intent returnIntent = new Intent();
            if (editFlag) {
                returnIntent.putExtra("reservation", reservation);
            }
            setResult(Constants.REFRESH_RESERVATION_FRAGMENT, returnIntent);
            finish();
            overridePendingTransition(R.anim.pull_in_left_activity, R.anim.push_out_right_activity);
        } else {
            Intent mainIntent = MainActivity.buildIntent(getApplicationContext());
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            finish();
            overridePendingTransition(R.anim.pull_in_left_activity, R.anim.push_out_right_activity);
        }
    }

    public void showBottomSheet() {
        if (null != vm) {
            vm.showBottomSheet();
        }
    }

    public void pickImageForCamera() {
        Intent i = new Intent();
        i.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            photoURI = FileProvider.getUriForFile(DetailReservationActivity.this, BuildConfig.APPLICATION_ID + ".provider", new File(Environment.getExternalStorageDirectory(), "task_" + new DateUtil().getCurrentDateAPIFormpat() + ".jpg"));
            i.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(i, Constants.PICK_IMAGE_CODE);
        } else {
            captureImageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "task_" + new DateUtil().getCurrentDateAPIFormpat() + ".jpg"));
            i.putExtra(MediaStore.EXTRA_OUTPUT, captureImageUri);
            startActivityForResult(i, Constants.PICK_IMAGE_CODE);
        }
    }

    public void pickFileForFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, Constants.SELECT_FILE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.PICK_IMAGE_CODE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    callFileUpload(photoURI);
                } else {
                    callFileUpload(captureImageUri);
                }
            } else if (requestCode == Constants.SELECT_FILE_CODE) {
                callFileUpload(data.getData());
            }
        }
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            PermissionListener permissionListener = new PermissionListener() {
                @Override
                public void onPermissionGranted() {
                    vm.showFileBottomSheet();
                }

                @Override
                public void onPermissionDenied(ArrayList<String> deniedPermissions) {

                }
            };

            TedPermission.with(getApplicationContext())
                    .setPermissionListener(permissionListener)
                    .setRationaleMessage("파일 업로드를 정상적으로 사용하기 위해서는 내부저장소 접근권한이 필요합니다.")
                    .setDeniedMessage("접근권한을 거부하셨습니다. \n[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                    .setPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .check();
        } else {
            vm.showFileBottomSheet();
        }
    }

    public void callFileUpload(Uri data) {
        String absolutePath = getRealPathFromURI(getApplicationContext(), data);

        File file = null;

        if (absolutePath != null) {
            file = new File(absolutePath);
        }

        if (!Uri.fromFile(file).toString().contains("/cache/") && absolutePath != null) {
            MimeTypeMap type = MimeTypeMap.getSingleton();
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            String mime = type.getMimeTypeFromExtension(extension);

            RequestBody requestBody = RequestBody.create(MediaType.parse(mime), file);

            fileLength = file.length();

            MultipartBody.Part body = MultipartBody.Part.createFormData("files", file.getName(), requestBody);

            Call<List<Attachment>> uploadCall = RestfulAdapter.getInstance().getServiceApi().postUpload("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getApplicationContext()), body);
            uploadCall.enqueue(new Callback<List<Attachment>>() {
                @Override
                public void onResponse(Call<List<Attachment>> call, Response<List<Attachment>> response) {
                    List<Attachment> attachments = response.body();
                    if (null != attachments) {
                        Attachment attachment = attachments.get(0);
                        sendAttachment(attachment);
                    }
                }

                @Override
                public void onFailure(Call<List<Attachment>> call, Throwable t) {
                    Log.e("failure : ", t.getMessage());
                }
            });
        }
    }

    public String getRealPathFromURI(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } // TODO handle non-primary volumes
            }// DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } // MediaProvider
            else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public void deleteComment(Comment comment) {
        //TODO : 코멘트 삭제
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("댓글을 삭제하시겠어요?")
                .positiveText("삭제")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Toast.makeText(getApplicationContext(), "댓글 삭제", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        dialog.show();
    }

    public void replaceToMention(User user) {
        isAlreadyReplaceMention = true;
        vm.replaceToMention(user);
        isAlreadyReplaceMention = false;
    }

    public void refreshReservation() {
        if (null == detailReservations) {
            detailReservations = new ArrayList<>();
        }
        Call<Reservation> reservationCall = RestfulAdapter.getInstance().getServiceApi().getReservation("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getApplicationContext()), reservationId);
        reservationCall.enqueue(new Callback<Reservation>() {
            @Override
            public void onResponse(Call<Reservation> call, Response<Reservation> response) {
                Reservation r = response.body();
                if (null != r) {
                    if (detailReservations.size() <= 0) {
                        detailReservations.add(0, r);
                        refreshCommentData();
                    } else {
                        detailReservations.set(0, r);
                        vm.setData(detailReservations);
                    }
                }
            }

            @Override
            public void onFailure(Call<Reservation> call, Throwable t) {

            }
        });
    }

    public void refreshCommentData() {
        if (null == comments) {
            comments = new ArrayList<>();
        }
        Call<List<Comment>> commentCall = RestfulAdapter.getInstance().getServiceApi().getComments("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getApplicationContext()),
                reservationId,
                15,
                comments.size());
        commentCall.enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (detailReservations.size() <= 1) {
                    detailReservations.add(1, new DetailReservationMoreButtonItem("이전 내용 보기"));
                    vm.setData(detailReservations);
                }
                List<Comment> result = response.body();
                if (null != result) {
                    if (comments.size() + result.size() > comments.size()) {
                        Collections.reverse(result);
                        comments.addAll(result);
                        List<BaseModel> tempList = new ArrayList<BaseModel>();
                        tempList.addAll(result);
                        vm.addDatas(tempList, 2, result.size());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {

            }
        });
    }

    public void editReservationStatus(final String status) {
        HashMap<String, String> statusMap = new HashMap<>();
        statusMap.put("status", status);
        Call<Reservation> putReservationStatus = RestfulAdapter.getInstance().getServiceApi().putReservation("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getApplicationContext()), reservationId, statusMap);
        putReservationStatus.enqueue(new Callback<Reservation>() {
            @Override
            public void onResponse(Call<Reservation> call, Response<Reservation> response) {
                if (response.body() != null) {
                    refreshReservation();
                    reservation.setStatus(status);
                    editFlag = true;
                }
            }

            @Override
            public void onFailure(Call<Reservation> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "예약상태 수정에 실패하였습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void sendComment(String text) {
        Call<Comment> createCommentCall = RestfulAdapter.getInstance().getServiceApi().createComment("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getApplicationContext()), reservationId, text);
        createCommentCall.enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                Comment result = response.body();
                if (null != result) {
                    comments.add(0, result);
                    vm.addData(result, 2, comments.size());
                }
                vm.setCurrentText("");
            }

            @Override
            public void onFailure(Call<Comment> call, Throwable t) {
            }
        });
    }

    public void sendAttachment(Attachment attachment) {
        Call<Comment> createCommentCall = RestfulAdapter.getInstance().getServiceApi().createComment("Bearer " + SharedPreferenceManager.getInstance().getUserToken(getApplicationContext()), reservationId, attachment);
        createCommentCall.enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                Comment result = response.body();
                if (result != null) {
                    comments.add(0, result);
                    vm.addDatas(detailReservations, 2, comments.size());
                }
                vm.setCurrentText("");
            }

            @Override
            public void onFailure(Call<Comment> call, Throwable t) {
            }
        });
    }
}