package com.example.warrantyvault;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddProductActivity extends AppCompatActivity {

    private TextInputEditText etProductName, etBrand, etModelNumber, etSerialNumber, etPurchaseDate, etPurchasePrice, etWarrantyDuration;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnSelectImage, btnSaveProduct, btnAddDocuments;
    private ImageView ivProductPreview;
    private TextView tvBillFileName;
    private ChipGroup chipGroupDocs;
    private ProgressBar progressBar;

    private Uri billUri;
    private final List<Uri> extraDocUris = new ArrayList<>();

    private long userId = -1;
    private DatabaseHelper databaseHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String[] categories = {"Electronics", "Home Appliances", "Mobile & Gadgets", "Computers & IT", "Vehicles", "Others"};

    private ActivityResultLauncher<Intent> billPicker;
    private ActivityResultLauncher<Intent> docsPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.addProductRoot));

        SharedPreferences prefs = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);

        databaseHelper = new DatabaseHelper(this);

        etProductName = findViewById(R.id.etProductName);
        etBrand = findViewById(R.id.etBrand);
        etModelNumber = findViewById(R.id.etModelNumber);
        etSerialNumber = findViewById(R.id.etSerialNumber);
        etPurchaseDate = findViewById(R.id.etPurchaseDate);
        etPurchasePrice = findViewById(R.id.etPurchasePrice);
        etWarrantyDuration = findViewById(R.id.etWarrantyDuration);
        actvCategory = findViewById(R.id.actvCategory);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAddDocuments = findViewById(R.id.btnAddDocuments);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);
        ivProductPreview = findViewById(R.id.ivProductPreview);
        tvBillFileName = findViewById(R.id.tvBillFileName);
        chipGroupDocs = findViewById(R.id.chipGroupDocs);
        progressBar = findViewById(R.id.progressBar);

        UiUtils.applyPressScaleAnimation(btnSelectImage);
        UiUtils.applyPressScaleAnimation(btnAddDocuments);
        UiUtils.applyPressScaleAnimation(btnSaveProduct);

        actvCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories));
        actvCategory.setText(categories[0], false);

        billPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null && data.getData() != null) {
                onBillPicked(data.getData());
            }
        });

        docsPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() != RESULT_OK || data == null) return;
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    addExtraDoc(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                addExtraDoc(data.getData());
            }
        });

        etPurchaseDate.setOnClickListener(v -> showDatePickerDialog());
        btnSelectImage.setOnClickListener(v -> openBillPicker());
        btnAddDocuments.setOnClickListener(v -> openDocsPicker());
        btnSaveProduct.setOnClickListener(v -> saveProduct());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) ->
                etPurchaseDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        // Purchase date can't be in the future
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    // ------------------------------------------------------------------
    // Pickers
    // ------------------------------------------------------------------
    private void openBillPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        billPicker.launch(intent);
    }

    private void openDocsPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        docsPicker.launch(intent);
    }

    private void onBillPicked(Uri uri) {
        billUri = uri;
        String name = fileName(uri);
        tvBillFileName.setText(name);
        tvBillFileName.setVisibility(View.VISIBLE);

        String mime = getContentResolver().getType(uri);
        if (DocumentStore.isImage(mime, name)) {
            ivProductPreview.setImageURI(uri);
            ivProductPreview.setVisibility(View.VISIBLE);
        } else {
            ivProductPreview.setVisibility(View.GONE);
        }
        btnSelectImage.setText("Change Bill");
    }

    private void addExtraDoc(Uri uri) {
        for (Uri u : extraDocUris) {
            if (u.equals(uri)) return;
        }
        extraDocUris.add(uri);

        Chip chip = new Chip(this);
        chip.setText(fileName(uri));
        chip.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        chip.setMaxLines(1);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            extraDocUris.remove(uri);
            chipGroupDocs.removeView(chip);
        });
        chipGroupDocs.addView(chip);
    }

    private String fileName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (i != -1 && c.getString(i) != null) return c.getString(i);
            }
        } catch (Exception ignored) {
        }
        String last = uri.getLastPathSegment();
        return last != null ? last : "file";
    }

    // ------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------
    private String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    private void saveProduct() {
        if (userId == -1) {
            Toast.makeText(this, "User session not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String name = text(etProductName);
        String brand = text(etBrand);
        String category = actvCategory.getText() != null && actvCategory.getText().length() > 0
                ? actvCategory.getText().toString() : "Others";
        String model = text(etModelNumber);
        String serial = text(etSerialNumber);
        String purchaseDateStr = text(etPurchaseDate);
        String priceStr = text(etPurchasePrice);
        String durationStr = text(etWarrantyDuration);

        if (TextUtils.isEmpty(name)) {
            etProductName.setError("Product name is required");
            etProductName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(brand)) {
            etBrand.setError("Brand is required");
            etBrand.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(purchaseDateStr)) {
            etPurchaseDate.setError("Purchase date is required");
            etPurchaseDate.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(durationStr)) {
            etWarrantyDuration.setError("Warranty duration is required");
            etWarrantyDuration.requestFocus();
            return;
        }
        if (isFutureDate(purchaseDateStr)) {
            etPurchaseDate.setError("Purchase date cannot be in the future");
            etPurchaseDate.requestFocus();
            return;
        }

        double price = 0.0;
        try {
            if (!TextUtils.isEmpty(priceStr)) price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etPurchasePrice.setError("Enter a valid price");
            etPurchasePrice.requestFocus();
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            etWarrantyDuration.setError("Enter months as a number");
            etWarrantyDuration.requestFocus();
            return;
        }
        if (duration <= 0 || duration > 600) {
            etWarrantyDuration.setError("Enter a valid number of months");
            etWarrantyDuration.requestFocus();
            return;
        }

        setSaving(true);

        final String expiryDateStr = WarrantyUtils.calculateExpiry(purchaseDateStr, duration);
        final String status = WarrantyUtils.computeStatus(expiryDateStr);
        final double finalPrice = price;
        final int finalDuration = duration;
        final Uri finalBillUri = billUri;
        final List<Uri> finalDocs = new ArrayList<>(extraDocUris);

        executorService.execute(() -> {
            try {
                String billPath = "";
                if (finalBillUri != null) {
                    DocumentStore.Saved saved = DocumentStore.copyIn(this, finalBillUri, DocumentStore.DIR_BILLS);
                    if (saved != null) billPath = saved.path;
                }

                long rowId = databaseHelper.insertProduct(
                        userId, name, brand, category, model, serial,
                        purchaseDateStr, finalPrice, finalDuration,
                        expiryDateStr, status, billPath, billPath);

                int failedDocs = 0;
                if (rowId != -1) {
                    for (Uri u : finalDocs) {
                        DocumentStore.Saved d = DocumentStore.copyIn(this, u, DocumentStore.DIR_DOCS);
                        if (d == null) {
                            failedDocs++;
                            continue;
                        }
                        long id = databaseHelper.insertDocument(userId, rowId, d.name, d.path, d.mime, d.size);
                        if (id == -1) {
                            DocumentStore.deleteFile(d.path);
                            failedDocs++;
                        }
                    }
                } else {
                    DocumentStore.deleteFile(billPath);
                }

                final int failed = failedDocs;
                mainHandler.post(() -> {
                    setSaving(false);
                    if (rowId != -1) {
                        Toast.makeText(this, failed == 0 ? "Product added successfully!"
                                : "Product added, but " + failed + " file(s) could not be saved", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MyProductsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to add product. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setSaving(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setSaving(boolean saving) {
        progressBar.setVisibility(saving ? View.VISIBLE : View.GONE);
        btnSaveProduct.setEnabled(!saving);
        btnSaveProduct.setText(saving ? "" : "Save Product");
        btnSaveProduct.setIconTint(android.content.res.ColorStateList.valueOf(
                saving ? android.graphics.Color.TRANSPARENT : android.graphics.Color.WHITE));
    }

    private boolean isFutureDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date d = sdf.parse(dateStr);
            return d != null && d.getTime() > System.currentTimeMillis();
        } catch (ParseException e) {
            return false;
        }
    }
}
