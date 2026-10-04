package com.newtube.mobile.ui.browse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.button.MaterialButton;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.Motion;

import java.util.ArrayList;
import java.util.List;

/**
 * Five-destination floating navigation tailored for the phone shell.
 *
 * <p>BottomNavigationView's equal-width item layout cannot reproduce the M3 Expressive/VIVI
 * pattern on a narrow phone with five Japanese labels: the selected label wraps or the indicator
 * escapes its container. This view keeps four inactive destinations compact and gives the selected
 * destination a content-driven pill whose width springs between states.</p>
 */
public final class ExpressiveFloatingNav extends LinearLayout {
    public interface OnItemSelectedListener {
        void onItemSelected(int itemId);
    }

    public interface OnItemReselectedListener {
        void onItemReselected(int itemId);
    }

    public static final class Item {
        public final int id;
        public final CharSequence title;
        @DrawableRes public final int iconRes;

        public Item(int id, CharSequence title, @DrawableRes int iconRes) {
            this.id = id;
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    private static final int NO_SELECTION = View.NO_ID;

    private final SparseArray<Holder> mHolders = new SparseArray<>();
    private final List<Item> mItems = new ArrayList<>();
    private int mSelectedItemId = NO_SELECTION;
    private OnItemSelectedListener mSelectedListener;
    private OnItemReselectedListener mReselectedListener;

    public ExpressiveFloatingNav(Context context) {
        this(context, null);
    }

    public ExpressiveFloatingNav(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpressiveFloatingNav(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        mSelectedListener = listener;
    }

    public void setOnItemReselectedListener(@Nullable OnItemReselectedListener listener) {
        mReselectedListener = listener;
    }

    public int getSelectedItemId() {
        return mSelectedItemId;
    }

    public int getItemCount() {
        return mItems.size();
    }

    public boolean containsItem(int itemId) {
        return mHolders.get(itemId) != null;
    }

    public void setItems(@NonNull List<Item> items) {
        int oldSelection = mSelectedItemId;

        removeAllViews();
        mHolders.clear();
        mItems.clear();
        mItems.addAll(items);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Item item : mItems) {
            FrameLayout root = (FrameLayout) inflater.inflate(
                    R.layout.item_mobile_floating_nav, this, false);
            View indicator = root.findViewById(R.id.mobile_floating_nav_indicator);
            MaterialButton button = root.findViewById(R.id.mobile_floating_nav_button);
            View badge = root.findViewById(R.id.mobile_floating_nav_badge);

            button.setId(item.id);
            button.setIconResource(item.iconRes);
            button.setCheckable(true);
            button.setSingleLine(true);
            button.setEllipsize(android.text.TextUtils.TruncateAt.END);
            button.setGravity(Gravity.CENTER);
            button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            button.setIconPadding(dp(6));
            // 48dp-tall destinations are pills from the very first frame. Do not wait for a
            // checked-state shape transition; that was why the first selection could look less
            // rounded until it was tapped again.
            button.setCornerRadius(dp(24));
            button.setContentDescription(item.title);

            Holder holder = new Holder(item, root, indicator, button, badge);
            mHolders.put(item.id, holder);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(48));
            params.leftMargin = dp(2);
            params.rightMargin = dp(2);
            addView(root, params);

            button.setOnClickListener(v -> {
                if (item.id == mSelectedItemId) {
                    if (mReselectedListener != null) {
                        mReselectedListener.onItemReselected(item.id);
                    }
                } else {
                    selectInternal(item.id, true, true);
                }
            });
        }

        mSelectedItemId = containsItem(oldSelection) ? oldSelection : NO_SELECTION;
        applySelection(false);
    }

    public void setSelectedItemId(int itemId) {
        if (!containsItem(itemId) || itemId == mSelectedItemId) {
            return;
        }
        selectInternal(itemId, true, true);
    }

    public void setBadgeVisible(int itemId, boolean visible, @Nullable CharSequence description) {
        Holder holder = mHolders.get(itemId);
        if (holder == null) {
            return;
        }
        holder.badge.setVisibility(visible ? VISIBLE : GONE);
        holder.button.setContentDescription(
                visible && description != null
                        ? holder.item.title + ", " + description
                        : holder.item.title);
    }

    private void selectInternal(int itemId, boolean animate, boolean notify) {
        mSelectedItemId = itemId;
        applySelection(animate);
        if (notify && mSelectedListener != null) {
            mSelectedListener.onItemSelected(itemId);
        }
    }

    private void applySelection(boolean animate) {
        for (int i = 0; i < mHolders.size(); i++) {
            Holder holder = mHolders.valueAt(i);
            boolean selected = holder.item.id == mSelectedItemId;
            styleHolder(holder, selected, animate);
        }
    }

    private void styleHolder(Holder holder, boolean selected, boolean animate) {
        holder.button.setChecked(selected);

        // Keep the destination inside the floating shell at all times. Selection is expressed by
        // width + tonal fill, not by scaling the whole button beyond its allocated slot.
        holder.button.animate().cancel();
        holder.button.setScaleX(1f);
        holder.button.setScaleY(1f);

        int fg = selected
                ? getContext().getColor(R.color.mobile_m3_primary)
                : getContext().getColor(R.color.mobile_m3_on_surface_variant);

        // The button itself stays transparent. Selection is painted by a dedicated indicator
        // underneath it, so we can reproduce NavigationBar's indicator motion without changing
        // the VIVI-like floating-pill geometry that already works well on five destinations.
        holder.button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        holder.button.setIconTint(ColorStateList.valueOf(fg));
        holder.button.setTextColor(fg);

        if (animate) {
            animateIndicator(holder, selected);
        } else {
            holder.indicator.animate().cancel();
            holder.indicator.setScaleX(selected ? 1f : 0.4f);
            holder.indicator.setScaleY(1f);
            holder.indicator.setAlpha(selected ? 0.40f : 0f);
        }

        int targetWidth = selected ? selectedWidth(holder) : dp(44);

        if (!animate) {
            holder.root.getLayoutParams().width = targetWidth;
            holder.root.requestLayout();
            holder.button.setText(selected ? holder.item.title : "");
            return;
        }

        if (selected) {
            // Reveal text while the pill grows; singleLine+ellipsize guarantees no vertical wrap.
            holder.button.setText(holder.item.title);
        }

        animateWidth(holder, targetWidth, selected);
    }

    /**
     * M3E NavigationBar motion, applied only to the tonal indicator inside our custom pill:
     * horizontal reveal (0.4 -> 1.0), no Y scaling, and a fast alpha ramp. The destination
     * container itself never scales, so the bar cannot protrude or lose its established shape.
     */
    private void animateIndicator(Holder holder, boolean selected) {
        holder.indicator.animate().cancel();

        if (selected) {
            holder.indicator.setScaleX(0.4f);
            holder.indicator.setScaleY(1f);
            holder.indicator.setAlpha(0f);
            holder.indicator.animate()
                    .scaleX(1f)
                    .alpha(0.40f)
                    .setDuration(300)
                    .setInterpolator(Motion.EMPHASIZED_DECELERATE)
                    .start();
        } else {
            holder.indicator.animate()
                    .scaleX(0.4f)
                    .alpha(0f)
                    .setDuration(180)
                    .setInterpolator(Motion.EMPHASIZED_ACCELERATE)
                    .start();
        }
    }

    private void animateWidth(Holder holder, int targetWidth, boolean selected) {
        if (holder.widthAnimator != null) {
            holder.widthAnimator.cancel();
        }

        int current = holder.root.getWidth();
        if (current <= 0) {
            current = holder.root.getLayoutParams().width;
        }
        if (current == targetWidth) {
            if (!selected) {
                holder.button.setText("");
            }
            return;
        }

        Motion.Spring spring = new Motion.Spring(0f, 1f, 0f, 520f, 0.78f, 0.001f);
        ValueAnimator animator = ValueAnimator.ofInt(current, targetWidth);
        animator.setDuration(Math.max(190L, spring.durationMs));
        animator.setInterpolator(spring);
        final int lower = Math.min(current, targetWidth);
        final int upper = Math.max(current, targetWidth);
        animator.addUpdateListener(a -> {
            int width = (int) a.getAnimatedValue();
            width = Math.max(lower, Math.min(upper, width));
            holder.root.getLayoutParams().width = width;
            holder.root.requestLayout();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!selected && holder.item.id != mSelectedItemId) {
                    holder.button.setText("");
                }
                if (holder.widthAnimator == animation) {
                    holder.widthAnimator = null;
                }
            }
        });
        holder.widthAnimator = animator;
        animator.start();
    }

    private int selectedWidth(Holder holder) {
        float textWidth = holder.button.getPaint().measureText(holder.item.title.toString());

        // MaterialButton has non-trivial content insets. Measuring only text + an arbitrary
        // constant was why even "ホーム" collapsed to "ホ…". Count the real button padding plus
        // the explicit icon/gap from item_mobile_floating_nav.xml.
        int desired = (int) Math.ceil(textWidth)
                + holder.button.getPaddingStart()
                + holder.button.getPaddingEnd()
                + dp(20)   // icon
                + dp(6)    // icon-to-label gap
                + dp(4);   // anti-clipping breathing room

        // The outer nav is wrap_content, so its CURRENT width cannot be used as the
        // expansion budget (that would create a circular cap). Budget against the viewport and
        // let the parent grow/shrink around the animated child widths.
        int available = getResources().getDisplayMetrics().widthPixels
                - dp(24)
                - getPaddingStart()
                - getPaddingEnd();

        // Inactive destinations stay compact, but the selected pill may consume all genuinely
        // free width. Never force a minimum wider than what the bar can actually provide.
        int siblings = Math.max(0, mItems.size() - 1);
        int max = available - siblings * dp(44) - mItems.size() * dp(4);
        int safeMax = Math.max(dp(72), max);
        int min = Math.min(dp(96), safeMax);

        return Math.max(min, Math.min(desired, safeMax));
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static final class Holder {
        final Item item;
        final FrameLayout root;
        final View indicator;
        final MaterialButton button;
        final View badge;
        @Nullable ValueAnimator widthAnimator;

        Holder(Item item, FrameLayout root, View indicator, MaterialButton button, View badge) {
            this.item = item;
            this.root = root;
            this.indicator = indicator;
            this.button = button;
            this.badge = badge;
        }
    }
}
