package com.example.warrantyvault;

import java.io.Serializable;

/** An extra document (warranty card, invoice PDF, manual, photo...) attached to a product. */
public class ProductDocument implements Serializable {
    public long id;
    public long productId;
    public String name;
    public String path;
    public String mime;
    public long size;
    public long addedAt;
}
