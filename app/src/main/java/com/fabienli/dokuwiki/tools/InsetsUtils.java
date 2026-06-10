package com.fabienli.dokuwiki.tools;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helpers to keep views clear of the system bars under edge-to-edge display,
 * which is enforced from Android 15 (API 35) when targetSdk is 35+.
 *
 * On older versions the system still reserves space for the bars, so the
 * reported bottom inset is 0 and these helpers are no-ops.
 */
public class InsetsUtils {

    /**
     * Adds the system bar bottom inset to the view's bottom margin. Use for
     * bottom-anchored views such as a FloatingActionButton so they don't end up
     * hidden behind the navigation bar.
     */
    public static void applyBottomMarginInset(final View view) {
        final int baseMargin = ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = baseMargin + bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });
    }

    /**
     * Adds the system bar bottom inset as bottom padding. Use for content that
     * fills to the bottom of the screen (text editors, web views, lists) so its
     * last lines/items can scroll clear of the navigation bar.
     */
    public static void applyBottomPaddingInset(final View view) {
        final int basePadding = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), basePadding + bars.bottom);
            return insets;
        });
    }
}
