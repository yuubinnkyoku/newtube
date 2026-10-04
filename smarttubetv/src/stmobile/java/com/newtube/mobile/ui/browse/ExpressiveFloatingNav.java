package com.newtube.mobile.ui.browse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.motion.MotionUtils;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Five-destination floating navigation for the phone shell.
 *
 * <p>The outer floating shell is NewTube's own treatment, but destination behavior follows the
 * M3 Expressive navigation-bar model on phone: fixed item slots, a 56x32 active indicator that
 * grows horizontally from 0.4x to 1x using the emphasized motion token, selected-only labels when
 * there are more than three destinations, and no Button-style press morph/bounce on the whole
 * destination.</p>
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
    private static final int[] CHECKED_STATE = {android.R.attr.state_checked};
    private static final int[] EMPTY_STATE = new int[0];

    // M3 Expressive phone nav-bar tokens:
    // active indicator 56x32dp, item icon 24dp, container 64dp.
    private static final int ITEM_WIDTH_DP = 60;
    private static final int ITEM_HEIGHT_DP = 56;

    private final SparseArray<Holder> mHolders = new SparseArray<>();
    private final List<Item> mItems = new ArrayList<>();

    private final TimeInterpolator mIndicatorInterpolator;
    private final int mIndicatorDurationMs;

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

        mIndicatorInterpolator = MotionUtils.resolveThemeInterpolator(
                context,
                com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
                new FastOutSlowInInterpolator());
        mIndicatorDurationMs = MotionUtils.resolveThemeDuration(
                context,
                com.google.android.material.R.attr.motionDurationLong2,
                300);
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
            ImageView icon = root.findViewById(R.id.mobile_floating_nav_icon);
            TextView label = root.findViewById(R.id.mobile_floating_nav_label);
            View badge = root.findViewById(R.id.mobile_floating_nav_badge);

            root.setId(item.id);
            root.setContentDescription(item.title);
            icon.setImageResource(item.iconRes);
            label.setText(item.title);

            Holder holder = new Holder(item, root, indicator, icon, label, badge);
            mHolders.put(item.id, holder);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(ITEM_WIDTH_DP), dp(ITEM_HEIGHT_DP));
            addView(root, params);

            root.setOnClickListener(v -> {
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
        holder.root.setContentDescription(
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
        holder.root.setSelected(selected);
        holder.icon.setImageState(selected ? CHECKED_STATE : EMPTY_STATE, true);

        if (holder.animator != null) {
            holder.animator.cancel();
            holder.animator = null;
        }

        float target = selected ? 1f : 0f;
        if (!animate) {
            holder.progress = target;
            applyIndicatorProgress(holder, target, target);
            if (!selected) {
                holder.label.setVisibility(INVISIBLE);
            }
            return;
        }

        if (selected) {
            holder.label.setVisibility(VISIBLE);
        }

        ValueAnimator animator = ValueAnimator.ofFloat(holder.progress, target);
        animator.setDuration(mIndicatorDurationMs);
        animator.setInterpolator(mIndicatorInterpolator);
        animator.addUpdateListener(a -> {
            float progress = (float) a.getAnimatedValue();
            holder.progress = progress;
            applyIndicatorProgress(holder, progress, target);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                holder.progress = target;
                applyIndicatorProgress(holder, target, target);
                if (!selected && holder.item.id != mSelectedItemId) {
                    holder.label.setVisibility(INVISIBLE);
                }
                if (holder.animator == animation) {
                    holder.animator = null;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (holder.animator == animation) {
                    holder.animator = null;
                }
            }
        });
        holder.animator = animator;
        animator.start();
    }

    /**
     * Mirrors NavigationBarItemView.ActiveIndicatorTransform:
     * indicator scaleX 0.4 -> 1.0, scaleY fixed at 1, alpha uses the first/last fifth of progress.
     */
    private void applyIndicatorProgress(Holder holder, float progress, float target) {
        float clamped = Math.max(0f, Math.min(1f, progress));

        holder.indicator.setScaleX(0.4f + 0.6f * clamped);
        holder.indicator.setScaleY(1f);
        holder.indicator.setAlpha(indicatorAlpha(clamped, target));

        // Selected-only label: appear with the destination selection, but never alter item width.
        holder.label.setAlpha(clamped);
        holder.label.setScaleX(0.92f + 0.08f * clamped);
        holder.label.setScaleY(0.92f + 0.08f * clamped);

        int inactive = getContext().getColor(R.color.mobile_m3_on_surface_variant);
        int activeIcon = getContext().getColor(R.color.mobile_m3_on_secondary_container);
        int activeLabel = getContext().getColor(R.color.mobile_m3_secondary);

        int iconColor = (int) ArgbEvaluator.getInstance().evaluate(clamped, inactive, activeIcon);
        int labelColor = (int) ArgbEvaluator.getInstance().evaluate(clamped, inactive, activeLabel);
        holder.icon.setImageTintList(ColorStateList.valueOf(iconColor));
        holder.label.setTextColor(labelColor);
    }

    private float indicatorAlpha(float progress, float target) {
        // Same 1/5 alpha window as MDC NavigationBarItemView.ActiveIndicatorTransform.
        if (target == 0f) {
            return clamp01((progress - 0.8f) / 0.2f);
        }
        return clamp01(progress / 0.2f);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static final class Holder {
        final Item item;
        final FrameLayout root;
        final View indicator;
        final ImageView icon;
        final TextView label;
        final View badge;

        float progress;
        @Nullable ValueAnimator animator;

        Holder(
                Item item,
                FrameLayout root,
                View indicator,
                ImageView icon,
                TextView label,
                View badge) {
            this.item = item;
            this.root = root;
            this.indicator = indicator;
            this.icon = icon;
            this.label = label;
            this.badge = badge;
        }
    }
}
