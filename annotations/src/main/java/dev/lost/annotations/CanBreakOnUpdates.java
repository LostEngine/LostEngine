package dev.lost.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface CanBreakOnUpdates {
    String value(); // Last Checked Version
}
