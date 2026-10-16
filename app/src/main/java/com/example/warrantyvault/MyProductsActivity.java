package com.example.warrantyvault;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private TextView tvEmpty;
    private TextInputEditText etSearch;
    private ProductAdapter adapter;
    private List<Product> fullProductList;
    private List<Product> filteredProductList;
    private DatabaseHelper databaseHelper;
    private long userId = -1;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_products);

        UiUtils.setupEdgeToEdge(this, findViewById(R.id.myProductsRoot));
        BottomNavHelper.setup(this, BottomNavHelper.PRODUCTS);

        SharedPreferences prefs = getSharedPreferences("WarrantyVaultSession", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);

        databaseHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerViewProducts);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fullProductList = new ArrayList<>();
        filteredProductList = new ArrayList<>();
        adapter = new ProductAdapter(filteredProductList, product -> {
            Intent intent = new Intent(MyProductsActivity.this, ProductDetailsActivity.class);
            intent.putExtra("product", product);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        setupSearchFilter();
    }

    private void setupSearchFilter() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProducts(String query) {
        String cleanQuery = query.toLowerCase().trim();
        filteredProductList.clear();

        if (cleanQuery.isEmpty()) {
            filteredProductList.addAll(fullProductList);
        } else {
            for (Product p : fullProductList) {
                if ((p.getProductName() != null && p.getProductName().toLowerCase().contains(cleanQuery)) ||
                        (p.getBrand() != null && p.getBrand().toLowerCase().contains(cleanQuery)) ||
                        (p.getCategory() != null && p.getCategory().toLowerCase().contains(cleanQuery))) {
                    filteredProductList.add(p);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredProductList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (!cleanQuery.isEmpty()) {
                tvEmpty.setText("No products match \"" + query + "\"");
            } else {
                tvEmpty.setText("No products added yet.");
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {
        if (userId == -1) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmpty.setText("User session not found.");
            recyclerView.setVisibility(View.GONE);
            return;
        }

        executorService.execute(() -> {
            List<Product> tempProductList;
            try {
                tempProductList = databaseHelper.getProductsList(userId);
            } catch (Exception e) {
                tempProductList = new ArrayList<>();
            }
            final List<Product> loaded = tempProductList;

            mainHandler.post(() -> {
                fullProductList.clear();
                fullProductList.addAll(loaded);

                String currentQuery = etSearch != null && etSearch.getText() != null ? etSearch.getText().toString() : "";
                filterProducts(currentQuery);
            });
        });
    }
}
