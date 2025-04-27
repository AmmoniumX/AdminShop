package com.ammonium.adminshop.blocks.interfaces;

public interface Detector extends ShopMachine {
    void setThreshold(long threshold);
    long getThreshold();
}
