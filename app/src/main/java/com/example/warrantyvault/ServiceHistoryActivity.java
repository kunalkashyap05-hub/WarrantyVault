package com.example.warrantyvault;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private TextView tvEmpty;
    private Button btnAddService;
    private ServiceRecordAdapter adapter;
    private List<ServiceRecord> list;
    private DatabaseHelper databaseHelper;
    private long userId = -1;
    private String productId, productName;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_history);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.serviceHistoryRoot));

        SharedPreferences prefs = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);

        databaseHelper = new DatabaseHelper(this);

        productId = getIntent().getStringExtra("productId");
        productName = getIntent().getStringExtra("productName");
        

        recyclerView = findViewById(R.id.recyclerViewService);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnAddService = findViewById(R.id.btnAddService);

        UiUtils.applyPressScaleAnimation(btnAddService);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        list = new ArrayList<>();
        adapter = new ServiceRecordAdapter(list, this::confirmDelete);
        recyclerView.setAdapter(adapter);

        btnAddService.setOnClickListener(v -> showAddServiceBottomSheet());

        loadServiceRecords();
    }

    private void showAddServiceBottomSheet() {
        if (userId == -1) {
            Toast.makeText(this, "User session not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.Theme_WarrantyVault_BottomSheetDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_service_record, null);
        bottomSheetDialog.setContentView(dialogView);

        View tilProduct = dialogView.findViewById(R.id.tilServiceProduct);
        AutoCompleteTextView actvProduct = dialogView.findViewById(R.id.actvServiceProduct);
        // When opened from the dashboard no product is preselected: let the user choose one.
        final List<Product> choices = new ArrayList<>();
        if (productId == null) {
            tilProduct.setVisibility(View.VISIBLE);
            executorService.execute(() -> {
                List<Product> mine = databaseHelper.getProductsList(userId);
                mainHandler.post(() -> {
                    choices.addAll(mine);
                    List<String> labels = new ArrayList<>();
                    for (Product p : mine) labels.add(p.getProductName() + " (" + p.getBrand() + ")");
                    actvProduct.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
                    if (mine.isEmpty()) actvProduct.setHint("Add a product first");
                });
            });
        }

        EditText etDate = dialogView.findViewById(R.id.etServiceDate);
        EditText etCentre = dialogView.findViewById(R.id.etServiceCentre);
        EditText etDesc = dialogView.findViewById(R.id.etDescription);
        EditText etCost = dialogView.findViewById(R.id.etCost);
        EditText etRemarks = dialogView.findViewById(R.id.etRemarks);

        Button btnCancel = dialogView.findViewById(R.id.btnCancelService);
        Button btnSave = dialogView.findViewById(R.id.btnSaveService);

        UiUtils.applyPressScaleAnimation(btnCancel);
        UiUtils.applyPressScaleAnimation(btnSave);

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
            String centre = etCentre.getText() != null ? etCentre.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String cost = etCost.getText() != null ? etCost.getText().toString().trim() : "";
            String remarks = etRemarks.getText() != null ? etRemarks.getText().toString().trim() : "";

            if (TextUtils.isEmpty(date) || TextUtils.isEmpty(centre)) {
                Toast.makeText(this, "Date and Service Center are required", Toast.LENGTH_SHORT).show();
                return;
            }

            long parsedProdId;
            String recordProductName = productName;
            if (productId != null) {
                try {
                    parsedProdId = Long.parseLong(productId);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid product", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                String chosen = actvProduct.getText() != null ? actvProduct.getText().toString() : "";
                Product match = null;
                for (Product p : choices) {
                    if ((p.getProductName() + " (" + p.getBrand() + ")").equals(chosen)) {
                        match = p;
                        break;
                    }
                }
                if (match == null) {
                    Toast.makeText(this, "Please select a product", Toast.LENGTH_SHORT).show();
                    return;
                }
                parsedProdId = Long.parseLong(match.getProductId());
                recordProductName = match.getProductName();
            }
            final String finalProductName = recordProductName;

            double parsedCost = 0.0;
            try {
                if (!TextUtils.isEmpty(cost)) {
                    parsedCost = Double.parseDouble(cost);
                }
            } catch (NumberFormatException e) {
                parsedCost = 0.0;
            }

            final long finalProdId = parsedProdId;
            final double finalCost = parsedCost;

            executorService.execute(() -> {
                long rowId = databaseHelper.insertServiceRecord(
                        userId, finalProdId, finalProductName, date, centre, desc, finalCost, remarks
                );

                mainHandler.post(() -> {
                    if (rowId != -1) {
                        Toast.makeText(ServiceHistoryActivity.this, "Service record added!", Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                        loadServiceRecords();
                    } else {
                        Toast.makeText(ServiceHistoryActivity.this, "Failed to add service record.", Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        bottomSheetDialog.show();
    }

    private void confirmDelete(ServiceRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete service record")
                .setMessage("Remove this service record?")
                .setPositiveButton("Delete", (d, w) -> executorService.execute(() -> {
                    try {
                        databaseHelper.deleteServiceRecord(Long.parseLong(record.getServiceId()));
                    } catch (NumberFormatException ignored) {
                    }
                    mainHandler.post(this::loadServiceRecords);
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadServiceRecords() {
        if (userId == -1) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmpty.setText("User session not found.");
            recyclerView.setVisibility(View.GONE);
            return;
        }

        executorService.execute(() -> {
            List<ServiceRecord> tempList = new ArrayList<>();
            Cursor cursor = null;
            try {
                if (productId != null) {
                    try {
                        long pId = Long.parseLong(productId);
                        cursor = databaseHelper.getServiceRecordsByProduct(pId);
                    } catch (NumberFormatException e) {
                        cursor = databaseHelper.getServiceRecordsByUser(userId);
                    }
                } else {
                    cursor = databaseHelper.getServiceRecordsByUser(userId);
                }

                if (cursor != null && cursor.moveToFirst()) {
                    int idCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_ID);
                    int userIdCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_USER_ID);
                    int prodIdCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_PRODUCT_ID);
                    int prodNameCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_PRODUCT_NAME);
                    int dateCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_DATE);
                    int centreCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_CENTRE);
                    int descCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_DESCRIPTION);
                    int costCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_COST);
                    int remarksCol = cursor.getColumnIndex(DatabaseHelper.COLUMN_SERVICE_REMARKS);

                    do {
                        String serviceId = idCol != -1 ? String.valueOf(cursor.getInt(idCol)) : "";
                        String sUserId = userIdCol != -1 ? String.valueOf(cursor.getLong(userIdCol)) : "";
                        String sProdId = prodIdCol != -1 ? String.valueOf(cursor.getLong(prodIdCol)) : "";
                        String sProdName = prodNameCol != -1 ? cursor.getString(prodNameCol) : "";
                        String sDate = dateCol != -1 ? cursor.getString(dateCol) : "";
                        String sCentre = centreCol != -1 ? cursor.getString(centreCol) : "";
                        String sDesc = descCol != -1 ? cursor.getString(descCol) : "";
                        String sCost = costCol != -1 ? String.valueOf(cursor.getDouble(costCol)) : "0.0";
                        String sRemarks = remarksCol != -1 ? cursor.getString(remarksCol) : "";

                        ServiceRecord record = new ServiceRecord(serviceId, sUserId, sProdId, sProdName, sDate, sCentre, sDesc, sCost, sRemarks);
                        tempList.add(record);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                // Handle exception
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            mainHandler.post(() -> {
                list.clear();
                list.addAll(tempList);
                adapter.notifyDataSetChanged();

                if (list.isEmpty()) {
                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
