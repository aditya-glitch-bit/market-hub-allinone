package com.markethub.service;

/** Catalogue search parameters (all optional) as sent by the storefront's filter bar. */
public class ProductQuery {

    private String text;
    private String category;
    private boolean onSaleOnly;
    private SortOrder sort = SortOrder.FEATURED;
    private int page = 1;
    private int size = 12;

    public String getText() { return text; }
    public ProductQuery setText(String text) { this.text = text; return this; }
    public String getCategory() { return category; }
    public ProductQuery setCategory(String category) { this.category = category; return this; }
    public boolean isOnSaleOnly() { return onSaleOnly; }
    public ProductQuery setOnSaleOnly(boolean onSaleOnly) { this.onSaleOnly = onSaleOnly; return this; }
    public SortOrder getSort() { return sort; }
    public ProductQuery setSort(SortOrder sort) { this.sort = sort; return this; }
    public int getPage() { return page; }
    public ProductQuery setPage(int page) { this.page = page; return this; }
    public int getSize() { return size; }
    public ProductQuery setSize(int size) { this.size = size; return this; }
}
