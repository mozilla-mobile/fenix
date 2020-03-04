package org.mozilla.fenix.utils;

/**
 * TODO SAM conversion
 */
@FunctionalInterface
public interface OnWifiChanged {
    void invoke(boolean Connected);
}
