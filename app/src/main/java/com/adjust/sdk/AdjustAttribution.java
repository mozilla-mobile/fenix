package com.adjust.sdk;

import java.io.Serializable;

public class AdjustAttribution implements Serializable {
    public String network;
    public String campaign;
    public String adgroup;
    public String creative;

    @Override
    public boolean equals(Object other) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "";
    }
}
