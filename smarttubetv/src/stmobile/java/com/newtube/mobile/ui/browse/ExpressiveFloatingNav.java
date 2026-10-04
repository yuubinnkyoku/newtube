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
            button.setContentDescription(item.title);

            Holder holder = new Holder(item, root, button, badge);
            mHolders.put(item.id, holder);

            MarginLayoutParams params = new MarginLayoutParams(dp(44), dp(48));
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

        int bg = selected
                ? getContext().getColor(R.color.mobile_m3_secondary_container)
                : Color.TRANSPARENT;
        int fg = selected
                ? getContext().getColor(R.color.mobile_m3_on_secondary_container)
                : getContext().getColor(R.color.mobile_m3_on_surface_variant);

        holder.button.setBackgroundTintList(ColorStateList.valueOf(bg));
        holder.button.setIconTint(ColorStateList.valueOf(fg));
        holder.button.setTextColor(fg);

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
        animator.addUpdateListener(a -> {
            holder.root.getLayoutParams().width = (int) a.getAnimatedValue();
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
        int desired = Math.round(textWidth) + dp(54);

        int available = getWidth();
        if (available <= 0) {
            available = getResources().getDisplayMetrics().widthPixels - dp(32);
        }

        // Each other destination keeps a 44dp touch/visual slot and every wrapper has 2dp margins
        // on both sides. This guarantees the selected pill can never push outside the floating bar.
        int siblings = Math.max(0, mItems.size() - 1);
        int max = available - siblings * dp(44) - mItems.size() * dp(4);
        max = Math.max(dp(96), max);

        return Math.max(dp(96), Math.min(desired, max));
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static final class Holder {
        final Item item;
        final FrameLayout root;
        final MaterialButton button;
        final View badge;
        @Nullable ValueAnimator widthAnimator;

        Holder(Item item, FrameLayout root, MaterialButton button, View badge) {
            this.item = item;
            this.root = root;
            this.button = button;
            this.badge = badge;
        }
    }
}
