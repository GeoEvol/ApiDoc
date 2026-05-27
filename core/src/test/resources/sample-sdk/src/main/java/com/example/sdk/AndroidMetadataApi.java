package com.example.sdk;

import com.example.sdk.annotations.FloatRange;
import com.example.sdk.annotations.IntRange;
import com.example.sdk.annotations.NonNull;
import com.example.sdk.annotations.Nullable;
import com.example.sdk.annotations.RequiresPermission;

/**
 * Android metadata sample.
 *
 * @since 1.5
 * @pending
 * @apiSince 12
 * @sdkExtSince R Extensions 4
 * @deprecated use Foo instead
 * @deprecatedSince 13
 * @removed removed after replacement shipped
 * @removedSince 14
 */
@Deprecated
public class AndroidMetadataApi {
    /**
     * Returns a guarded value.
     *
     * @return guarded value
     */
    @NonNull
    @IntRange(from = 1, to = 10)
    @RequiresPermission(value = {"sample.permission.READ", "sample.permission.WRITE"})
    public String guardedValue() {
        return "value";
    }

    /**
     * Returns a nullable ratio.
     *
     * @return ratio value
     */
    @Nullable
    @FloatRange(from = 0.0, to = 1.0)
    public Double nullableRatio() {
        return null;
    }

    /**
     * Requires all listed permissions.
     */
    @RequiresPermission(allOf = {"sample.permission.CAMERA", "sample.permission.LOCATION"})
    public void allRequired() {
    }

    /**
     * Requires one listed permission.
     */
    @RequiresPermission(anyOf = {"sample.permission.BLUETOOTH", "sample.permission.NFC"})
    public void anyRequired() {
    }
}
