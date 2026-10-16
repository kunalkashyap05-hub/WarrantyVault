package com.example.warrantyvault;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailsActivity extends AppCompatActivity {

    private TextView tvProductName, tvBrandCategory, tvStatus, tvInitial, tvExpiryHint;
    private TextView tvModelValue, tvSerialValue, tvPurchaseDateValue, tvPriceValue, tvDurationValue, tvExpiryValue;
    private TextView tvNoBill, tvBillName, tvNoDocs;
    private ImageView ivBill;
    private View layoutBillFile, layoutBillActions;
    private MaterialButton btnServiceRecord, btnDelete, btnViewBill, btnDownloadBill, btnShareBill, btnReplaceBill, btnAddDocument;
    private RecyclerView recyclerDocuments;

    private Product product;
    private long productId = -1;
    private long userId = -1;
    private DatabaseHelper databaseHelper;
    private DocumentAdapter documentAdapter;
    private final List<ProductDocument> documents = new ArrayList<>();

    // pending "Save as…" request on Android 9 and below
    private String pendingSavePath, pendingSaveMime;

    private ActivityResultLauncher<Intent> billPicker, docsPicker, saveAsPicker;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.productDetailsRoot));

        databaseHelper = new DatabaseHelper(this);
        userId = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE).getLong("userId", -1);
        product = (Product) getIntent().getSerializableExtra("product");

        if (product == null) {
            Toast.makeText(this, "Product details not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            productId = Long.parseLong(product.getProductId());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid product ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupLaunchers();
        setupDocumentList();
        setupActionListeners();
        populateProductData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFromDb();
    }

    private void bindViews() {
        tvInitial = findViewById(R.id.tvInitial);
        tvProductName = findViewById(R.id.tvProductName);
        tvBrandCategory = findViewById(R.id.tvBrandCategory);
        tvStatus = findViewById(R.id.tvStatus);
        tvExpiryHint = findViewById(R.id.tvExpiryHint);

        tvModelValue = findViewById(R.id.tvModelValue);
        tvSerialValue = findViewById(R.id.tvSerialValue);
        tvPurchaseDateValue = findViewById(R.id.tvPurchaseDateValue);
        tvPriceValue = findViewById(R.id.tvPriceValue);
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvExpiryValue = findViewById(R.id.tvExpiryValue);

        tvNoBill = findViewById(R.id.tvNoBill);
        tvBillName = findViewById(R.id.tvBillName);
        ivBill = findViewById(R.id.ivBill);
        layoutBillFile = findViewById(R.id.layoutBillFile);
        layoutBillActions = findViewById(R.id.layoutBillActions);
        btnViewBill = findViewById(R.id.btnViewBill);
        btnDownloadBill = findViewById(R.id.btnDownloadBill);
        btnShareBill = findViewById(R.id.btnShareBill);
        btnReplaceBill = findViewById(R.id.btnReplaceBill);

        tvNoDocs = findViewById(R.id.tvNoDocs);
        recyclerDocuments = findViewById(R.id.recyclerDocuments);
        btnAddDocument = findViewById(R.id.btnAddDocument);

        btnServiceRecord = findViewById(R.id.btnServiceRecord);
        btnDelete = findViewById(R.id.btnDelete);

        UiUtils.applyPressScaleAnimation(btnServiceRecord);
        UiUtils.applyPressScaleAnimation(btnDelete);
        UiUtils.applyPressScaleAnimation(btnViewBill);
        UiUtils.applyPressScaleAnimation(btnDownloadBill);
        UiUtils.applyPressScaleAnimation(btnShareBill);
        UiUtils.applyPressScaleAnimation(btnReplaceBill);
        UiUtils.applyPressScaleAnimation(btnAddDocument);
    }

    private void setupLaunchers() {
        billPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null && data.getData() != null) {
                replaceBill(data.getData());
            }
        });

        docsPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() != RESULT_OK || data == null) return;
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            addDocuments(uris);
        });

        saveAsPicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            String path = pendingSavePath;
            pendingSavePath = null;
            if (result.getResultCode() != RESULT_OK || data == null || data.getData() == null || path == null) return;
            Uri dest = data.getData();
            executorService.execute(() -> {
                String msg;
                try {
                    DocumentStore.copyTo(this, path, dest);
                    msg = "File saved";
                } catch (Exception e) {
                    msg = "Download failed: " + e.getMessage();
                }
                final String m = msg;
                mainHandler.post(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show());
            });
        });
    }

    private void setupDocumentList() {
        recyclerDocuments.setLayoutManager(new LinearLayoutManager(this));
        documentAdapter = new DocumentAdapter(documents, new DocumentAdapter.Listener() {
            @Override
            public void onOpen(ProductDocument doc) {
                DocumentStore.open(ProductDetailsActivity.this, doc.path, doc.mime);
            }

            @Override
            public void onDownload(ProductDocument doc) {
                downloadFile(doc.path, doc.name, doc.mime, doc.name);
            }

            @Override
            public void onDelete(ProductDocument doc) {
                confirmDeleteDocument(doc);
            }
        });
        recyclerDocuments.setAdapter(documentAdapter);
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------
    private void refreshFromDb() {
        executorService.execute(() -> {
            Product fresh = databaseHelper.getProductObject(productId);
            List<ProductDocument> docs = databaseHelper.getDocumentsByProduct(productId);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (fresh != null) product = fresh;
                documents.clear();
                documents.addAll(docs);
                populateProductData();
                updateDocumentsUi();
            });
        });
    }

    private void populateProductData() {
        String name = product.getProductName();
        tvProductName.setText(name != null && !name.isEmpty() ? name : "Product Details");
        tvInitial.setText(name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "P");
        tvBrandCategory.setText((product.getBrand() != null ? product.getBrand() : "") + " • " + (product.getCategory() != null ? product.getCategory() : ""));

        tvModelValue.setText(orDash(product.getModelNumber()));
        tvSerialValue.setText(orDash(product.getSerialNumber()));
        tvPurchaseDateValue.setText(orDash(product.getPurchaseDate()));
        tvPriceValue.setText(formatPrice(product.getPurchasePrice()));
        tvDurationValue.setText((product.getWarrantyDuration() != null ? product.getWarrantyDuration() : "12") + " months");
        tvExpiryValue.setText(orDash(product.getWarrantyExpiry()));
        tvExpiryHint.setText(WarrantyUtils.describe(product.getWarrantyExpiry()));

        String status = product.getWarrantyStatus();
        tvStatus.setText(status != null ? status.toUpperCase() : "ACTIVE");

        if (WarrantyUtils.EXPIRED.equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.bg_status_expired);
            tvStatus.setTextColor(Color.parseColor("#E11D48"));
        } else if (WarrantyUtils.EXPIRING.equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.bg_status_expiring);
            tvStatus.setTextColor(Color.parseColor("#D97706"));
        } else {
            tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            tvStatus.setTextColor(Color.parseColor("#059669"));
        }

        populateBill();
    }

    private static String orDash(String s) {
        return s != null && !s.isEmpty() ? s : "—";
    }

    private static String formatPrice(String p) {
        try {
            double v = Double.parseDouble(p);
            if (v == Math.floor(v)) return "₹" + (long) v;
            return "₹" + String.format(java.util.Locale.getDefault(), "%.2f", v);
        } catch (Exception e) {
            return "₹0";
        }
    }

    private String billPath() {
        String p = product.getBillImageUrl();
        if (p == null || p.isEmpty()) return null;
        return new File(p).exists() ? p : null;
    }

    private void populateBill() {
        String path = billPath();
        if (path == null) {
            ivBill.setVisibility(View.GONE);
            layoutBillFile.setVisibility(View.GONE);
            layoutBillActions.setVisibility(View.GONE);
            tvNoBill.setVisibility(View.VISIBLE);
            btnReplaceBill.setText("Upload Bill");
            return;
        }

        tvNoBill.setVisibility(View.GONE);
        layoutBillActions.setVisibility(View.VISIBLE);
        btnReplaceBill.setText("Replace Bill");

        String mime = DocumentStore.mimeForName(path);
        if (DocumentStore.isImage(mime, path)) {
            layoutBillFile.setVisibility(View.GONE);
            ivBill.setVisibility(View.VISIBLE);
            ivBill.setImageURI(null);
            ivBill.setImageURI(Uri.fromFile(new File(path)));
        } else {
            ivBill.setVisibility(View.GONE);
            layoutBillFile.setVisibility(View.VISIBLE);
            tvBillName.setText(DocumentStore.isPdf(mime, path) ? "Bill (PDF)" : "Bill file");
        }
    }

    private void updateDocumentsUi() {
        documentAdapter.notifyDataSetChanged();
        boolean empty = documents.isEmpty();
        recyclerDocuments.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvNoDocs.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------
    private void setupActionListeners() {
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("This will also delete its bill, documents and service records. Continue?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProduct())
                .setNegativeButton("Cancel", null)
                .show());

        btnServiceRecord.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceHistoryActivity.class);
            intent.putExtra("productId", product.getProductId());
            intent.putExtra("productName", product.getProductName());
            startActivity(intent);
        });

        ivBill.setOnClickListener(v -> openBill());
        layoutBillFile.setOnClickListener(v -> openBill());
        btnViewBill.setOnClickListener(v -> openBill());

        btnDownloadBill.setOnClickListener(v -> {
            String path = billPath();
            if (path == null) return;
            String base = (product.getProductName() != null ? product.getProductName() : "Product") + " - Bill";
            downloadFile(path, base, DocumentStore.mimeForName(path), path);
        });

        btnShareBill.setOnClickListener(v -> {
            String path = billPath();
            if (path != null) DocumentStore.share(this, path, DocumentStore.mimeForName(path));
        });

        btnReplaceBill.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
            billPicker.launch(intent);
        });

        btnAddDocument.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            docsPicker.launch(intent);
        });
    }

    private void openBill() {
        String path = billPath();
        if (path != null) DocumentStore.open(this, path, DocumentStore.mimeForName(path));
    }

    /** Saves a copy of a stored file to the phone's Downloads folder (or asks where to save on old Androids). */
    private void downloadFile(String path, String baseName, String mime, String originalName) {
        if (path == null || !new File(path).exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }
        final String fileName = DocumentStore.downloadName(baseName != null ? baseName : "document", path, originalName);
        final String finalMime = mime != null ? mime : DocumentStore.mimeForName(fileName);

        executorService.execute(() -> {
            try {
                boolean saved = DocumentStore.saveToDownloads(this, path, fileName, finalMime);
                mainHandler.post(() -> {
                    if (saved) {
                        Toast.makeText(this, "Saved to Downloads/WarrantyVault\n" + fileName, Toast.LENGTH_LONG).show();
                    } else {
                        // Android 9 and below: let the user choose where to save
                        pendingSavePath = path;
                        pendingSaveMime = finalMime;
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType(finalMime);
                        i.putExtra(Intent.EXTRA_TITLE, fileName);
                        saveAsPicker.launch(i);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void replaceBill(Uri uri) {
        final String oldPath = product.getBillImageUrl();
        executorService.execute(() -> {
            DocumentStore.Saved saved = DocumentStore.copyIn(this, uri, DocumentStore.DIR_BILLS);
            if (saved == null) {
                mainHandler.post(() -> Toast.makeText(this, "Could not read that file", Toast.LENGTH_SHORT).show());
                return;
            }
            int rows = databaseHelper.updateProductBill(productId, saved.path);
            if (rows > 0) {
                DocumentStore.deleteFile(oldPath);
            } else {
                DocumentStore.deleteFile(saved.path);
            }
            mainHandler.post(() -> {
                Toast.makeText(this, rows > 0 ? "Bill saved" : "Failed to save bill", Toast.LENGTH_SHORT).show();
                refreshFromDb();
            });
        });
    }

    private void addDocuments(List<Uri> uris) {
        if (uris.isEmpty()) return;
        executorService.execute(() -> {
            int ok = 0, failed = 0;
            for (Uri u : uris) {
                DocumentStore.Saved d = DocumentStore.copyIn(this, u, DocumentStore.DIR_DOCS);
                if (d == null) {
                    failed++;
                    continue;
                }
                long id = databaseHelper.insertDocument(userId, productId, d.name, d.path, d.mime, d.size);
                if (id == -1) {
                    DocumentStore.deleteFile(d.path);
                    failed++;
                } else {
                    ok++;
                }
            }
            final int fOk = ok, fFailed = failed;
            mainHandler.post(() -> {
                String msg = fOk + (fOk == 1 ? " file added" : " files added");
                if (fFailed > 0) msg += ", " + fFailed + " failed";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                refreshFromDb();
            });
        });
    }

    private void confirmDeleteDocument(ProductDocument doc) {
        new AlertDialog.Builder(this)
                .setTitle("Delete file")
                .setMessage("Remove \"" + doc.name + "\" from this product?")
                .setPositiveButton("Delete", (d, w) -> executorService.execute(() -> {
                    databaseHelper.deleteDocument(doc.id);
                    DocumentStore.deleteFile(doc.path);
                    mainHandler.post(this::refreshFromDb);
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct() {
        executorService.execute(() -> {
            // collect files first: DB rows are removed by ON DELETE CASCADE
            List<ProductDocument> docs = databaseHelper.getDocumentsByProduct(productId);
            String bill = product.getBillImageUrl();

            int rowsDeleted = databaseHelper.deleteProduct(productId);
            if (rowsDeleted > 0) {
                DocumentStore.deleteFile(bill);
                for (ProductDocument d : docs) DocumentStore.deleteFile(d.path);
            }

            mainHandler.post(() -> {
                if (rowsDeleted > 0) {
                    Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
