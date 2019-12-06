package com.liang.tablayoutkt;

import android.content.Context;

import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liang.tabs.BadgeView;

import com.liang.tabs.TabImp;

import org.jetbrains.annotations.NotNull;


public class TabMenu extends TabImp {

    private View tabView;

    public TabMenu(@NonNull Context context) {
        this(context, null);
    }

    public TabMenu(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabMenu(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @NotNull
    @Override
    protected View setContentView() {
        tabView = LayoutInflater.from(getContext()).inflate(R.layout.tab_menul, null);
        return tabView;
    }


    @Override
    protected TextView setTabTitleView() {
        TextView title = tabView.findViewById(R.id.navigation_title);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        return title;
    }

    @Override
    protected ImageView setTabIconView() {
        return tabView.findViewById(R.id.navigation_icon);
    }

    @Override
    protected BadgeView setTabBadgeView() {
        BadgeView badge = tabView.findViewById(R.id.navigation_badge);
        return badge;
    }
}
