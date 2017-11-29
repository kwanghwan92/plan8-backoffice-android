package io.plan8.backoffice.activity;

import android.content.Intent;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by SSozi on 2017. 11. 28..
 */

public abstract class BaseActivity extends AppCompatActivity {
    private ViewDataBinding binding;
    private long backPressedTime = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        Log.d("lifeCycle", "onCreate :: " + getClass().getSimpleName() + "  ::  " + hashCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("lifeCycle", "onStart :: " + getClass().getSimpleName() + "  ::  " + hashCode());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lifeCycle", "onResume :: " + getClass().getSimpleName() + "  ::  " + hashCode());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lifeCycle", "onPause :: " + getClass().getSimpleName() + "  ::  " + hashCode());
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("lifeCycle", "onStop :: " + getClass().getSimpleName() + "  ::  " + hashCode());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("lifeCycle", "onDestroy :: " + getClass().getSimpleName() + "  ::  " + hashCode());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("lifeCycle", "onNewIntent :: " + getClass().getSimpleName() + "  ::  " + hashCode());
    }

    public void onBackPressed(boolean canDoubleClickFinish) {
        if (canDoubleClickFinish) {
            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPressedTime;

            if (0 <= intervalTime && 2000 >= intervalTime) {
                finish();
            } else {
                backPressedTime = tempTime;
                Toast toast = Toast.makeText(getBaseContext(), "'뒤로'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    public ViewDataBinding getBinding() {
        return binding;
    }

    public void setBinding(ViewDataBinding binding) {
        this.binding = binding;
    }
}