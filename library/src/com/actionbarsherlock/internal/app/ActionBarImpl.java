/*
 * Copyright (C) 2011 Jake Wharton <jakewharton@gmail.com>
 * Copyright (C) 2010 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.actionbarsherlock.internal.app;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActionBar;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.ActionMode;
import android.support.v4.view.MenuItem;
import android.view.View;
import android.widget.SpinnerAdapter;
import com.actionbarsherlock.R;
import com.actionbarsherlock.internal.view.menu.ActionMenuItemView;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuItemImpl;
import com.actionbarsherlock.internal.widget.ActionBarContainer;
import com.actionbarsherlock.internal.widget.ActionBarView;

public final class ActionBarImpl extends ActionBar {
    /** Action bar container. */
    private ActionBarContainer mContainerView;

    /** Action bar view. */
    private ActionBarView mActionView;

    /** List of listeners to the menu visibility. */
    private final List<OnMenuVisibilityListener> mMenuListeners = new ArrayList<OnMenuVisibilityListener>();



    public <T extends Activity & SupportActivity> ActionBarImpl(T activity) {
        super(activity);
    }


    // ------------------------------------------------------------------------
    // ACTION BAR SHERLOCK SUPPORT
    // ------------------------------------------------------------------------

    @Override
    protected ActionBar getPublicInstance() {
        return (mActionView != null) ? this : null;
    }

    public void init() {
        mActionView = (ActionBarView)mActivity.findViewById(R.id.abs__action_bar);
        mContainerView = (ActionBarContainer)mActivity.findViewById(R.id.abs__action_bar_container);

        if (mActionView == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used with a screen_*.xml layout");
        }

        final PackageManager pm = mActivity.getPackageManager();
        ActivityInfo actInfo = null;
        try {
            actInfo = pm.getActivityInfo(mActivity.getComponentName(), PackageManager.GET_ACTIVITIES);
        } catch (NameNotFoundException e) {}


        if (mActionView.getTitle() == null) {
            if ((actInfo != null) && (actInfo.labelRes != 0)) {
                //Load label string resource from the activity entry
                mActionView.setTitle(actInfo.labelRes);
            } else {
                //No activity label string resource and none in theme
                mActionView.setTitle(actInfo.loadLabel(pm));
            }
        }
    }

    public void onMenuInflated(MenuBuilder menu) {
        if (mActionView == null) {
            return;
        }

        final int maxItems = mActivity.getResources().getInteger(R.integer.abs__max_action_buttons);

        //Iterate and grab as many actions as we can up to maxItems honoring
        //their showAsAction values
        int ifItems = 0;
        final int count = menu.size();
        boolean showsActionItemText = menu.getShowsActionItemText();
        List<MenuItemImpl> keep = new ArrayList<MenuItemImpl>();
        for (int i = 0; i < count; i++) {
            MenuItemImpl item = (MenuItemImpl)menu.getItem(i);

            //Items without an icon or custom view are forced into the overflow menu
            if (!showsActionItemText && (item.getIcon() == null) && (item.getActionView() == null)) {
                continue;
            }
            if (showsActionItemText && ((item.getTitle() == null) || "".equals(item.getTitle()))) {
                continue;
            }

            if ((item.getShowAsAction() & MenuItem.SHOW_AS_ACTION_ALWAYS) != 0) {
                //Show always therefore add to keep list
                keep.add(item);

                if ((keep.size() > maxItems) && (ifItems > 0)) {
                    //If we have exceeded the max and there are "ifRoom" items
                    //then iterate backwards to remove one and add it to the
                    //head of the classic items list.
                    for (int j = keep.size() - 1; j >= 0; j--) {
                        if ((keep.get(j).getShowAsAction() & MenuItem.SHOW_AS_ACTION_IF_ROOM) != 0) {
                            keep.remove(j);
                            ifItems -= 1;
                            break;
                        }
                    }
                }
            } else if (((item.getShowAsAction() & MenuItem.SHOW_AS_ACTION_IF_ROOM) != 0)
                    && (keep.size() < maxItems)) {
                //"ifRoom" items are added if we have not exceeded the max.
                keep.add(item);
                ifItems += 1;
            }
        }

        //Mark items that will be shown on the action bar as such so they do
        //not show up on the activity options menu
        mActionView.removeAllItems();
        for (MenuItemImpl item : keep) {
            item.setIsShownOnActionBar(true);

            //Get a new item for this menu item
            ActionMenuItemView actionItem = mActionView.newItem();
            actionItem.initialize(item, MenuBuilder.TYPE_ACTION_BAR);

            //Associate the itemview with the item so changes will be reflected
            item.setItemView(MenuBuilder.TYPE_ACTION_BAR, actionItem);

            //Add to the action bar for display
            mActionView.addItem(actionItem);
        }
    }

    public void onMenuVisibilityChanged(boolean isVisible) {
        //Marshal to all listeners
        for (OnMenuVisibilityListener listener : mMenuListeners) {
            listener.onMenuVisibilityChanged(isVisible);
        }
    }

    public void setProgressBarIndeterminateVisibility(boolean visible) {
        if (mActionView != null) {
            mActionView.setProgressBarIndeterminateVisibility(visible);
        }
    }

    // ------------------------------------------------------------------------
    // ACTION MODE METHODS
    // ------------------------------------------------------------------------

    @Override
    protected ActionMode startActionMode(ActionMode.Callback callback) {
        throw new RuntimeException("Not implemented.");
    }

    // ------------------------------------------------------------------------
    // ACTION BAR METHODS
    // ------------------------------------------------------------------------

    @Override
    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        if (!mMenuListeners.contains(listener)) {
            mMenuListeners.add(listener);
        }
    }

    @Override
    public void addTab(Tab tab) {
        mActionView.addTab(tab);
    }

    @Override
    public void addTab(Tab tab, boolean setSelected) {
        mActionView.addTab(tab, setSelected);
    }

    @Override
    public void addTab(Tab tab, int position) {
        mActionView.addTab(tab, position);
    }

    @Override
    public void addTab(ActionBar.Tab tab, int position, boolean setSelected) {
        mActionView.addTab(tab, position, setSelected);
    }

    @Override
    public View getCustomView() {
        return mActionView.getCustomView();
    }

    @Override
    public int getDisplayOptions() {
        return mActionView.getDisplayOptions();
    }

    @Override
    public int getHeight() {
        return mActionView.getHeight();
    }

    @Override
    public int getNavigationItemCount() {
        return mActionView.getNavigationItemCount();
    }

    @Override
    public int getNavigationMode() {
        return mActionView.getNavigationMode();
    }

    @Override
    public int getSelectedNavigationIndex() {
        return mActionView.getSelectedNavigationIndex();
    }

    @Override
    public ActionBar.Tab getSelectedTab() {
        return mActionView.getSelectedTab();
    }

    @Override
    public CharSequence getSubtitle() {
        return mActionView.getSubtitle();
    }

    @Override
    public ActionBar.Tab getTabAt(int index) {
        return mActionView.getTabAt(index);
    }

    @Override
    public int getTabCount() {
        return mActionView.getTabCount();
    }

    @Override
    public CharSequence getTitle() {
        return mActionView.getTitle();
    }

    @Override
    public void hide() {
        //TODO: animate
        mContainerView.setVisibility(View.GONE);
    }

    @Override
    public boolean isShowing() {
        return mContainerView.getVisibility() == View.VISIBLE;
    }

    @Override
    public ActionBar.Tab newTab() {
        return mActionView.newTab();
    }

    @Override
    public void removeAllTabs() {
        mActionView.removeAllTabs();
    }

    @Override
    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        mMenuListeners.remove(listener);
    }

    @Override
    public void removeTab(ActionBar.Tab tab) {
        mActionView.removeTab(tab);
    }

    @Override
    public void removeTabAt(int position) {
        mActionView.removeTabAt(position);
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        mContainerView.setBackgroundDrawable(d);
    }

    @Override
    public void setCustomView(int resId) {
        mActionView.setCustomView(resId);
    }

    @Override
    public void setCustomView(View view) {
        mActionView.setCustomView(view);
    }

    @Override
    public void setCustomView(View view, ActionBar.LayoutParams layoutParams) {
        mActionView.setCustomView(view, layoutParams);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        mActionView.setDisplayHomeAsUpEnabled(showHomeAsUp);
    }

    @Override
    public void setDisplayOptions(int options, int mask) {
        mActionView.setDisplayOptions(options, mask);
    }

    @Override
    public void setDisplayOptions(int options) {
        mActionView.setDisplayOptions(options);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean showCustom) {
        mActionView.setDisplayShowCustomEnabled(showCustom);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean showHome) {
        mActionView.setDisplayShowHomeEnabled(showHome);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean showTitle) {
        mActionView.setDisplayShowTitleEnabled(showTitle);
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean useLogo) {
        mActionView.setDisplayUseLogoEnabled(useLogo);
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter adapter, ActionBar.OnNavigationListener callback) {
        mActionView.setListNavigationCallbacks(adapter, callback);
    }

    @Override
    public void setNavigationMode(int mode) {
        mActionView.setNavigationMode(mode);
    }

    @Override
    public void setSelectedNavigationItem(int position) {
        mActionView.setSelectedNavigationItem(position);
    }

    @Override
    public void selectTab(ActionBar.Tab tab) {
        mActionView.selectTab(tab);
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mActionView.setSubtitle(subtitle);
    }

    @Override
    public void setSubtitle(int resId) {
        mActionView.setSubtitle(resId);
    }

    @Override
    public void setTitle(CharSequence title) {
        mActionView.setTitle(title);
    }
    @Override
    public void setTitle(int resId) {
        mActionView.setTitle(resId);
    }

    @Override
    public void show() {
        //TODO: animate
        mContainerView.setVisibility(View.VISIBLE);
    }
}
