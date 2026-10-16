package com.example.warrantyvault;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView tvGreeting, tvDate, tvTotalCount, tvActiveCount, tvExpiringCount, tvEmptyText;
    private TextView tvDrawerUserName, tvDrawerUserEmail;
    private View cardTotal, cardActive, cardExpiring, headerBar, layoutEmptyState, cardAvatar;
    private NestedScrollView dashboardScroll;
    private ChipGroup chipGroupFilters;
    private RecyclerView recyclerViewHomeProducts;
    private View cardAlert;
    private TextView tvAlertText;

    private Button btnDrawerEditProfile, btnDrawerMyProducts, btnDrawerAddProduct, btnDrawerServiceCentres, btnDrawerNotifications, btnDrawerLogout;

    private ProductAdapter adapter;
    private List<Product> fullProductList;
    private List<Product> filteredProductList;

    private DatabaseHelper databaseHelper;
    private long sqliteUserId = -1;
    private String userName = "";
    private String userEmail = "";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int prevTotal = 0, prevActive = 0, prevExpiring = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        headerBar = findViewById(R.id.headerBar);
        cardAvatar = findViewById(R.id.cardAvatar);

        UiUtils.setupEdgeToEdge(this, headerBar);
        // header is a dark gradient: use light (white) status bar icons
        new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        SharedPreferences prefs = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE);
        boolean loggedIn = prefs.getBoolean("loggedIn", false);
        sqliteUserId = prefs.getLong("userId", -1);
        userEmail = prefs.getString("email", "");

        if (!loggedIn || sqliteUserId == -1) {
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvDate = findViewById(R.id.tvDate);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvExpiringCount = findViewById(R.id.tvExpiringCount);
        tvEmptyText = findViewById(R.id.tvEmptyText);

        tvDrawerUserName = findViewById(R.id.tvDrawerUserName);
        tvDrawerUserEmail = findViewById(R.id.tvDrawerUserEmail);

        cardTotal = findViewById(R.id.cardTotal);
        cardActive = findViewById(R.id.cardActive);
        cardExpiring = findViewById(R.id.cardExpiring);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        dashboardScroll = findViewById(R.id.dashboardScroll);

        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        recyclerViewHomeProducts = findViewById(R.id.recyclerViewHomeProducts);
        cardAlert = findViewById(R.id.cardAlert);
        tvAlertText = findViewById(R.id.tvAlertText);

        // Drawer Menu Buttons
        btnDrawerEditProfile = findViewById(R.id.btnDrawerEditProfile);
        btnDrawerMyProducts = findViewById(R.id.btnDrawerMyProducts);
        btnDrawerAddProduct = findViewById(R.id.btnDrawerAddProduct);
        btnDrawerServiceCentres = findViewById(R.id.btnDrawerServiceCentres);
        btnDrawerNotifications = findViewById(R.id.btnDrawerNotifications);
        btnDrawerLogout = findViewById(R.id.btnDrawerLogout);

        // Android 13+: ask once for permission to show warranty reminders
        if (android.os.Build.VERSION.SDK_INT >= 33
                && androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { })
                    .launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }

        setupRecyclerView();
        setupBehaviors();
        setupClickListeners();
        setupFilterChips();
        BottomNavHelper.setup(this, BottomNavHelper.HOME);
        updateDateDisplay();
        setupBackPressedHandler();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));
    }

    private void setupRecyclerView() {
        recyclerViewHomeProducts.setLayoutManager(new LinearLayoutManager(this));
        fullProductList = new ArrayList<>();
        filteredProductList = new ArrayList<>();

        adapter = new ProductAdapter(filteredProductList, product -> {
            Intent intent = new Intent(DashboardActivity.this, ProductDetailsActivity.class);
            intent.putExtra("product", product);
            startActivity(intent);
        });
        recyclerViewHomeProducts.setAdapter(adapter);
    }

    private void setupBehaviors() {
        UiUtils.applyPressScaleAnimation(cardAvatar);
        UiUtils.applyPressScaleAnimation(cardTotal);
        UiUtils.applyPressScaleAnimation(cardActive);
        UiUtils.applyPressScaleAnimation(cardExpiring);
        UiUtils.applyPressScaleAnimation(btnDrawerEditProfile);

        UiUtils.animateEntrance(cardTotal, 100);
        UiUtils.animateEntrance(cardActive, 180);
        UiUtils.animateEntrance(cardExpiring, 260);
    }

    private void setupClickListeners() {
        // Open Side Bar Drawer when tapping profile avatar
        cardAvatar.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        btnDrawerLogout.setOnClickListener(v -> performLogout());

        cardAlert.setOnClickListener(v -> {
            chipGroupFilters.check(R.id.chipExpiring);
            dashboardScroll.post(() -> dashboardScroll.smoothScrollTo(0, findViewById(R.id.tvLivingProductsHeader).getTop() - 16));
        });

        // Side Bar Menu Listeners
        btnDrawerEditProfile.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
            showEditProfileBottomSheet();
        });

        btnDrawerMyProducts.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(DashboardActivity.this, MyProductsActivity.class));
        });

        btnDrawerAddProduct.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(DashboardActivity.this, AddProductActivity.class));
        });

        btnDrawerServiceCentres.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(DashboardActivity.this, ServiceCentresActivity.class));
        });

        btnDrawerNotifications.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(DashboardActivity.this, NotificationsActivity.class));
        });

        cardTotal.setOnClickListener(v -> chipGroupFilters.check(R.id.chipAll));
        cardActive.setOnClickListener(v -> chipGroupFilters.check(R.id.chipActive));
        cardExpiring.setOnClickListener(v -> chipGroupFilters.check(R.id.chipExpiring));
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showEditProfileBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.Theme_WarrantyVault_BottomSheetDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        bottomSheetDialog.setContentView(dialogView);

        TextInputEditText etEditFullName = dialogView.findViewById(R.id.etEditFullName);
        Button btnCancelProfile = dialogView.findViewById(R.id.btnCancelProfile);
        Button btnSaveProfile = dialogView.findViewById(R.id.btnSaveProfile);

        if (etEditFullName != null) {
            etEditFullName.setText(userName);
            etEditFullName.setSelection(userName.length());
        }

        UiUtils.applyPressScaleAnimation(btnCancelProfile);
        UiUtils.applyPressScaleAnimation(btnSaveProfile);

        btnCancelProfile.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etEditFullName != null && etEditFullName.getText() != null ? etEditFullName.getText().toString().trim() : "";
            if (TextUtils.isEmpty(newName)) {
                if (etEditFullName != null) etEditFullName.setError("Full Name is required");
                return;
            }

            executorService.execute(() -> {
                int rowsUpdated = databaseHelper.updateUserName(sqliteUserId, newName);
                mainHandler.post(() -> {
                    if (rowsUpdated > 0) {
                        userName = newName;
                        updateGreetingText();
                        if (tvDrawerUserName != null) {
                            tvDrawerUserName.setText(userName);
                        }
                        Toast.makeText(DashboardActivity.this, "Profile name updated!", Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                    } else {
                        Toast.makeText(DashboardActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        bottomSheetDialog.show();
    }

    private void setupFilterChips() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipAll) {
                filterLivingList(null);
            } else if (checkedId == R.id.chipActive) {
                filterLivingList("ACTIVE");
            } else if (checkedId == R.id.chipExpiring) {
                filterLivingList("EXPIRING SOON");
            } else if (checkedId == R.id.chipExpired) {
                filterLivingList("EXPIRED");
            }
        });
    }

    private void filterLivingList(String targetStatus) {
        filteredProductList.clear();
        if (targetStatus == null) {
            filteredProductList.addAll(fullProductList);
        } else {
            for (Product p : fullProductList) {
                if (targetStatus.equalsIgnoreCase(p.getWarrantyStatus())) {
                    filteredProductList.add(p);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredProductList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewHomeProducts.setVisibility(View.GONE);
            tvEmptyText.setText(targetStatus == null ? "No products added yet" : "No " + targetStatus.toLowerCase() + " products");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewHomeProducts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (sqliteUserId == -1) return;

        executorService.execute(() -> {
            // Get user name and email
            Cursor userCursor = databaseHelper.getUserById(sqliteUserId);
            if (userCursor != null && userCursor.moveToFirst()) {
                int nameCol = userCursor.getColumnIndex(DatabaseHelper.COLUMN_USER_NAME);
                int emailCol = userCursor.getColumnIndex(DatabaseHelper.COLUMN_USER_EMAIL);

                if (nameCol != -1) {
                    userName = userCursor.getString(nameCol);
                }
                if (emailCol != -1) {
                    userEmail = userCursor.getString(emailCol);
                }
                userCursor.close();
            }

            // Get products (status is recomputed from the expiry date)
            List<Product> tempList;
            try {
                tempList = databaseHelper.getProductsList(sqliteUserId);
            } catch (Exception e) {
                tempList = new ArrayList<>();
            }
            int totalCount = tempList.size();
            int activeCount = 0;
            int expiringCount = 0;
            for (Product p : tempList) {
                if (WarrantyUtils.ACTIVE.equalsIgnoreCase(p.getWarrantyStatus())) activeCount++;
                else if (WarrantyUtils.EXPIRING.equalsIgnoreCase(p.getWarrantyStatus())) expiringCount++;
            }
            final List<Product> loadedList = tempList;

            final int finalTotal = totalCount;
            final int finalActive = activeCount;
            final int finalExpiring = expiringCount;

            mainHandler.post(() -> {
                updateGreetingText();

                if (tvDrawerUserName != null && !userName.isEmpty()) {
                    tvDrawerUserName.setText(userName);
                }
                if (tvDrawerUserEmail != null && !userEmail.isEmpty()) {
                    tvDrawerUserEmail.setText(userEmail);
                }

                UiUtils.animateCountUp(tvTotalCount, prevTotal, finalTotal, 600);
                UiUtils.animateCountUp(tvActiveCount, prevActive, finalActive, 600);
                UiUtils.animateCountUp(tvExpiringCount, prevExpiring, finalExpiring, 600);

                if (finalExpiring > 0) {
                    tvAlertText.setText(finalExpiring == 1
                            ? "1 warranty expires within 30 days"
                            : finalExpiring + " warranties expire within 30 days");
                    cardAlert.setVisibility(View.VISIBLE);
                } else {
                    cardAlert.setVisibility(View.GONE);
                }

                prevTotal = finalTotal;
                prevActive = finalActive;
                prevExpiring = finalExpiring;

                fullProductList.clear();
                fullProductList.addAll(loadedList);

                NotificationHelper.checkAndNotify(DashboardActivity.this, loadedList);

                // Apply current chip filter
                int checkedChipId = chipGroupFilters.getCheckedChipId();
                if (checkedChipId == R.id.chipActive) {
                    filterLivingList("ACTIVE");
                } else if (checkedChipId == R.id.chipExpiring) {
                    filterLivingList("EXPIRING SOON");
                } else if (checkedChipId == R.id.chipExpired) {
                    filterLivingList("EXPIRED");
                } else {
                    filterLivingList(null);
                }
            });
        });
    }

    private void updateGreetingText() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greetingPrefix;
        if (hour < 12) {
            greetingPrefix = "Good morning";
        } else if (hour < 17) {
            greetingPrefix = "Good afternoon";
        } else {
            greetingPrefix = "Good evening";
        }

        String displayName = (userName != null && !userName.isEmpty()) ? userName.split(" ")[0] : "there";
        tvGreeting.setText(greetingPrefix + ", " + displayName + " 👋");
    }

}
