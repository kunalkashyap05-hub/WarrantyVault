package com.example.warrantyvault;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    public interface Listener {
        void onOpen(ProductDocument doc);
        void onDownload(ProductDocument doc);
        void onDelete(ProductDocument doc);
    }

    private final List<ProductDocument> docs;
    private final Listener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public DocumentAdapter(List<ProductDocument> docs, Listener listener) {
        this.docs = docs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ProductDocument d = docs.get(position);
        h.tvName.setText(d.name != null ? d.name : "Document");

        String type;
        if (DocumentStore.isPdf(d.mime, d.name)) type = "PDF";
        else if (DocumentStore.isImage(d.mime, d.name)) type = "Image";
        else {
            String ext = DocumentStore.extensionOf(d.name);
            type = ext.isEmpty() ? "File" : ext.toUpperCase(Locale.ROOT);
        }
        h.tvMeta.setText(type + " • " + DocumentStore.humanSize(d.size) + " • " + dateFormat.format(new Date(d.addedAt)));

        h.itemView.setOnClickListener(v -> listener.onOpen(d));
        h.btnDownload.setOnClickListener(v -> listener.onDownload(d));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(d));
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta;
        ImageButton btnDownload, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDocName);
            tvMeta = itemView.findViewById(R.id.tvDocMeta);
            btnDownload = itemView.findViewById(R.id.btnDocDownload);
            btnDelete = itemView.findViewById(R.id.btnDocDelete);
        }
    }
}
