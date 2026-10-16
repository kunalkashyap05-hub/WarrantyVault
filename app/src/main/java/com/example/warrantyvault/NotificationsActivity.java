package com.example.warrantyvault;

import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private final List<NotificationHelper.Item> items = new ArrayList<>();
    private RecyclerView recycler;
    private View layoutEmpty, btnClearAll;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.notificationsRoot));
        BottomNavHelper.setup(this, BottomNavHelper.REMINDERS);

        recycler = findViewById(R.id.recyclerNotifications);
        layoutEmpty = findViewById(R.id.layoutNoNotifications);
        btnClearAll = findViewById(R.id.btnClearAll);

        adapter = new Adapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnNotifSettings).setOnClickListener(v -> showSettings());
        btnClearAll.setOnClickListener(v -> {
            NotificationHelper.clearHistory(this);
            refresh();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        items.clear();
        items.addAll(NotificationHelper.getHistory(this));
        adapter.notifyDataSetChanged();
        boolean empty = items.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        btnClearAll.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showSettings() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.Theme_WarrantyVault_BottomSheetDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_notification_settings, null);
        dialog.setContentView(view);

        SharedPreferences prefs = getSharedPreferences(NotificationHelper.PREFS_NAME, MODE_PRIVATE);
        MaterialSwitch sw = view.findViewById(R.id.switchReminders);
        sw.setChecked(prefs.getBoolean(NotificationHelper.KEY_NOTIFS_ENABLED, true));
        sw.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(NotificationHelper.KEY_NOTIFS_ENABLED, checked).apply();
            Toast.makeText(this, checked ? "Warranty alerts turned on" : "Warranty alerts turned off", Toast.LENGTH_SHORT).show();
        });

        MaterialButton btnSystem = view.findViewById(R.id.btnSystemSettings);
        btnSystem.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        });

        dialog.show();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            NotificationHelper.Item it = items.get(position);
            h.title.setText(it.title);
            h.message.setText(it.message);
            h.time.setText(DateUtils.getRelativeTimeSpanString(it.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title, message, time;

            VH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tvNotifTitle);
                message = v.findViewById(R.id.tvNotifMessage);
                time = v.findViewById(R.id.tvNotifTime);
            }
        }
    }
}
