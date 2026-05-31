package com.example.sdk.inheritance;

import com.byd.dilink.anotation.Supported;

/**
 * Interface only supported on DiLink300 — used to verify platform filtering
 * hides packages/groups when no member matches the selected platform.
 */
@Supported(platforms = {"DiLink300"})
public interface PlatformOnlyInterface {
    /** Returns a label. */
    String label();
}
