package com.example.warrantyvault;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Wires up the floating bottom bar (view_bottom_nav.xml) used on the main screens. */
public class BottomNavHelper {

    public static final int HOME = 0, PRODUCTS = 1, SERVICE = 2, REMINDERS = 3;

    public static void setup(Activity a, int selected) {
        android.view.View root = a.findViewById(R.id.bottomNavRoot);
        if (root == null) return;

        // keep the bar above the system navigation bar
        final android.view.View bar = a.findViewById(R.id.navBar);
        final int basePad = bar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            bar.setPadding(bar.getPaddingLeft(), bar.getPaddingTop(), bar.getPaddingRight(), basePad + bottom);
            return insets;
        });

        int[] ids = {R.id.navHome, R.id.navProducts, R.id.navService, R.id.navReminders};
        int[] icons = {R.id.navHomeIcon, R.id.navProductsIcon, R.id.navServiceIcon, R.id.navRemindersIcon};
        int[] labels = {R.id.navHomeLabel, R.id.navProductsLabel, R.id.navServiceLabel, R.id.navRemindersLabel};
        Class<?>[] targets = {DashboardActivity.class, MyProductsActivity.class, ServiceCentresActivity.class, NotificationsActivity.class};

        for (int i = 0; i < ids.length; i++) {
            boolean sel = i == selected;
            int color = ContextCompat.getColor(a, sel ? R.color.primary : R.color.text_secondary);
            ((ImageView) a.findViewById(icons[i])).setColorFilter(color);
            ((TextView) a.findViewById(labels[i])).setTextColor(color);

            final Class<?> target = targets[i];
            a.findViewById(ids[i]).setOnClickListener(v -> go(a, target, selected));
        }

        android.view.View add = a.findViewById(R.id.navAdd);
        UiUtils.applyPressScaleAnimation(add);
        add.setOnClickListener(v -> a.startActivity(new Intent(a, AddProductActivity.class)));
    }

    private static void go(Activity a, Class<?> target, int selected) {
        if (a.getClass() == target) return;
        Intent intent = new Intent(a, target);
        if (target == DashboardActivity.class) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            a.startActivity(intent);
        } else {
            a.startActivity(intent);
            // keep the back stack tidy: Home -> one tab
            if (!(a instanceof DashboardActivity)) a.finish();
        }
        a.overridePendingTransition(0, 0);
    }
}
