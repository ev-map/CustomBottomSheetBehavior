package com.mahc.custombottomsheetbehavior;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.widget.ActionMenuView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~      http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~
 ~ https://github.com/miguelhincapie/CustomBottomSheetBehavior
 */

/**
 * This behavior should be applied on an AppBarLayout... More Explanations coming soon
 */
public class MergedAppBarLayoutBehavior extends AppBarLayout.ScrollingViewBehavior {

    private static final String TAG = MergedAppBarLayoutBehavior.class.getSimpleName();

    private boolean mInit = false;

    private FrameLayout.LayoutParams mBackGroundLayoutParams;

    private Context mContext;
    /**
     * To avoid using multiple "peekheight=" in XML and looking flexibility allowing {@link BottomSheetBehaviorGoogleMapsLike#mPeekHeight}
     * get changed dynamically we get the {@link NestedScrollView} that has
     * "app:layout_behavior=" {@link BottomSheetBehaviorGoogleMapsLike} inside the {@link CoordinatorLayout}
     */
    private WeakReference<BottomSheetBehaviorGoogleMapsLike> mBottomSheetBehaviorRef;
    private float mInitialY;
    private boolean mVisible = false;

    private String mToolbarTitle;

    private Toolbar mToolbar;
    private TextView mTitleTextView;
    private View mBackground;
    private View.OnClickListener mOnNavigationClickListener;

    private ValueAnimator mTitleAlphaValueAnimator;
    private int mCurrentTitleAlpha = 0;

    private List<LayerDrawable> transitionDrawables;
    private boolean buttonBackgroundVisible = true;

    public MergedAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof NestedScrollView) {
            try {
                BottomSheetBehaviorGoogleMapsLike.from(dependency);
                return true;
            }
            catch (IllegalArgumentException e){}
        }
        return false;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {

        if (!mInit) {
            init(parent, child);
        }
        /**
         * Following docs we should return true if the Behavior changed the child view's size or position, false otherwise
         */
        boolean childMoved = false;

        if(isDependencyYBelowAnchorPoint(parent, dependency)){

            childMoved = setToolbarVisible(false,child);

        }else if(isDependencyYBetweenAnchorPointAndToolbar(parent, child,dependency)){

            childMoved = setToolbarVisible(true,child);
            setFullBackGroundColor(android.R.color.transparent, child);
            setButtonBackgrounds(true);
            setPartialBackGroundHeight(0);

        } else if(isDependencyYBelowToolbar(child, dependency) && ! isDependencyYReachTop(dependency)){

            childMoved = setToolbarVisible(true,child);
            /*if(isStatusBarVisible())
                setStatusBarBackgroundVisible(false);*/
            setTitleVisible(false);
            setFullBackGroundColor(android.R.color.transparent, child);
            setButtonBackgrounds(true);
            setPartialBackGroundHeight((int)((child.getHeight() + child.getY()) - dependency.getY()));

        } else if(isDependencyYBelowStatusToolbar(child, dependency) || isDependencyYReachTop(dependency)){

            childMoved = setToolbarVisible(true,child);
            /*if(!isStatusBarVisible())
                setStatusBarBackgroundVisible(true);*/
            setTitleVisible(true);
            setFullBackGroundColor(R.color.colorPrimary, child);
            setButtonBackgrounds(false);
            setPartialBackGroundHeight(0);
        }
        return childMoved;
    }

    private void init(@NonNull CoordinatorLayout parent, @NonNull View child){

        if (!(child instanceof MergedAppBarLayout)) throw new IllegalArgumentException("The view is not a MergedAppBarLayout");

        MergedAppBarLayout appBarLayout = (MergedAppBarLayout) child;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        }

        mToolbar = appBarLayout.toolbar;
        mBackground = appBarLayout.background;
        mBackGroundLayoutParams = (FrameLayout.LayoutParams) mBackground.getLayoutParams();
        getBottomSheetBehavior(parent);

        //TODO: modify the way we get this TextView with the MergedAppBarLayout.java
        mTitleTextView = findTitleTextView(mToolbar);
        if (mTitleTextView == null)
            return;

        mInitialY = child.getY();

        child.setVisibility(mVisible ? View.VISIBLE : View.INVISIBLE);
//        setStatusBarBackgroundVisible(mVisible);

        boolean fullBackground = mVisible && mCurrentTitleAlpha == 1;
        setFullBackGroundColor(fullBackground ? R.color.colorPrimary: android.R.color.transparent, child);
        setButtonBackgrounds(!fullBackground);
        setPartialBackGroundHeight(0);
        mTitleTextView.setText(mToolbarTitle);
        mTitleTextView.setAlpha(mCurrentTitleAlpha);
        mInit = true;
        //setToolbarVisible(false,child);
    }

    /**
     * Look into the CoordiantorLayout for the {@link BottomSheetBehaviorGoogleMapsLike}
     * @param coordinatorLayout with app:layout_behavior= {@link BottomSheetBehaviorGoogleMapsLike}
     */
    private void getBottomSheetBehavior(@NonNull CoordinatorLayout coordinatorLayout) {

        for (int i = 0; i < coordinatorLayout.getChildCount(); i++) {
            View child = coordinatorLayout.getChildAt(i);

            if (child instanceof NestedScrollView) {

                try {
                    BottomSheetBehaviorGoogleMapsLike temp = BottomSheetBehaviorGoogleMapsLike.from(child);
                    mBottomSheetBehaviorRef = new WeakReference<>(temp);
                    break;
                }
                catch (IllegalArgumentException e){}
            }
        }
    }

    private boolean isDependencyYBelowAnchorPoint(@NonNull CoordinatorLayout parent, @NonNull View dependency){
        if (mBottomSheetBehaviorRef == null || mBottomSheetBehaviorRef.get() == null)
            getBottomSheetBehavior(parent);
        return dependency.getY() > mBottomSheetBehaviorRef.get().getAnchorPoint();
    }

    private boolean isDependencyYBetweenAnchorPointAndToolbar(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency){
        if (mBottomSheetBehaviorRef == null || mBottomSheetBehaviorRef.get() == null)
            getBottomSheetBehavior(parent);
        return dependency.getY() <= mBottomSheetBehaviorRef.get().getAnchorPoint() && dependency.getY() > child.getY() + child.getHeight();
    }

    private boolean isDependencyYBelowToolbar(@NonNull View child, @NonNull View dependency){
        return dependency.getY() <= child.getY() + child.getHeight() && dependency.getY() > child.getY();
    }

    private boolean isDependencyYBelowStatusToolbar(@NonNull View child, @NonNull View dependency){
        return dependency.getY() <= child.getY();
    }

    private boolean isDependencyYReachTop(@NonNull View dependency){
        return dependency.getY() == 0;
    }

    private void setPartialBackGroundHeight(int height){
        mBackGroundLayoutParams.height = height;
        mBackground.setLayoutParams(mBackGroundLayoutParams);
    }

    private void setFullBackGroundColor(@ColorRes int colorRes, View child){
        child.setBackgroundColor(ContextCompat.getColor(mContext,colorRes));
    }

    private void setButtonBackgrounds(boolean b) {
        if (transitionDrawables == null) {
            transitionDrawables = new ArrayList<>();
            for (int i = 0; i < mToolbar.getChildCount(); i++) {
                View it = mToolbar.getChildAt(i);
                if (it instanceof ImageButton) {
                    it.setBackgroundResource(R.drawable.bottomsheetbehavior_circle_bg);
                    transitionDrawables.add((LayerDrawable) it.getBackground());
                } else if (it instanceof ActionMenuView) {
                    ActionMenuView menu = (ActionMenuView) it;
                    for (int j = 0; j < menu.getChildCount(); j++) {
                        View ab = menu.getChildAt(j);
                        ab.setBackgroundResource(R.drawable.bottomsheetbehavior_circle_bg);
                        transitionDrawables.add((LayerDrawable) ab.getBackground());
                    }
                }
            }
        }

        if (b == buttonBackgroundVisible) return;
        buttonBackgroundVisible = b;

        for (LayerDrawable d : transitionDrawables) {
            ObjectAnimator.ofInt(d.getDrawable(0), "alpha", b ? 255 : 0)
                    .setDuration(200).start();
        }
    }

    private TextView findTitleTextView(Toolbar toolbar){
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View toolBarChild = toolbar.getChildAt(i);
            if (toolBarChild instanceof TextView &&
                    ((TextView)toolBarChild).getText() != null &&
                    ((TextView)toolBarChild).getText().toString().contentEquals(mContext.getResources().getString(R.string.key_binding_default_toolbar_name))) {
                return (TextView) toolBarChild;
            }
        }
        return null;
    }

    private boolean setToolbarVisible(boolean visible, final View child){
        ViewPropertyAnimator mAppBarLayoutAnimation;
        boolean childMoved = false;
        if(visible && !mVisible){
            childMoved = true;
            child.setY(-child.getHeight()/3);
            mAppBarLayoutAnimation = child.animate().setDuration(mContext.getResources().getInteger(android.R.integer.config_shortAnimTime));
            mAppBarLayoutAnimation.setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    child.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    /*if (mContext instanceof AppCompatActivity) {
                        ((AppCompatActivity)mContext).setSupportActionBar(mToolbar);
                    }
                    mToolbar.setNavigationOnClickListener(mOnNavigationClickListener);
                    if (mContext instanceof AppCompatActivity) {
                        ActionBar actionBar = ((AppCompatActivity) mContext).getSupportActionBar();
                        if (actionBar != null) {
                            actionBar.setDisplayHomeAsUpEnabled(true);
                        }
                    }*/
                    mVisible = true;
                }
            });
            mAppBarLayoutAnimation.alpha(1).y(mInitialY).start();
        }else if(!visible && mVisible){
            mAppBarLayoutAnimation = child.animate().setDuration(mContext.getResources().getInteger(android.R.integer.config_shortAnimTime));
            mAppBarLayoutAnimation.setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    child.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    /*if (mContext instanceof AppCompatActivity) {
                        ((AppCompatActivity) mContext).setSupportActionBar(null);
                    }*/
                    mVisible = false;
                }
            });
            mAppBarLayoutAnimation.alpha(0).start();
        }

        return childMoved;
    }

    private void setTitleVisible(boolean visible){

        if((visible && mTitleTextView.getAlpha() == 1)||
                (!visible && mTitleTextView.getAlpha() == 0))
            return;

        if(mTitleAlphaValueAnimator != null) {
            mTitleAlphaValueAnimator.cancel();
            mTitleAlphaValueAnimator = null;
        }
        mToolbar.setTitle(mToolbarTitle);
        int startAlpha = visible ? 0 : 1;
        int endAlpha = mCurrentTitleAlpha = visible ? 1 : 0;

        mTitleAlphaValueAnimator = ValueAnimator.ofFloat(startAlpha,endAlpha);
        mTitleAlphaValueAnimator.setDuration(mContext.getResources().getInteger(android.R.integer.config_shortAnimTime));
        mTitleAlphaValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTitleTextView.setAlpha((Float) animation.getAnimatedValue());
            }
        });
        mTitleAlphaValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mTitleAlphaValueAnimator = null;
            }
        });
        mTitleAlphaValueAnimator.start();
    }

    /*private boolean isStatusBarVisible(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mContext instanceof Activity){
            return ((Activity)mContext).getWindow().getStatusBarColor() == statusBarColor;
        }
        return true;
    }

    private void setStatusBarBackgroundVisible(boolean visible){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mContext instanceof Activity){
            if(visible){
                Window window = ((Activity)mContext).getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                //window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(statusBarColor);
            }else {
                Window window = ((Activity)mContext).getWindow();
                //window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(mContext,android.R.color.transparent));
            }
        }
    }*/

    public void setNavigationOnClickListener(View.OnClickListener listener){
        this.mOnNavigationClickListener = listener;
    }

    public void setToolbarTitle(String title) {
        this.mToolbarTitle = title;
        if(this.mToolbar!=null)
            this.mToolbar.setTitle(title);
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, View child) {
        return new SavedState(super.onSaveInstanceState(parent, child),
                mVisible,
                mToolbarTitle,
                mCurrentTitleAlpha);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, View child, Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        this.mVisible = ss.mVisible;
        this.mToolbarTitle = ss.mToolbarTitle;
        this.mCurrentTitleAlpha = ss.mTitleAlpha;
    }

    protected static class SavedState extends View.BaseSavedState {

        final boolean mVisible;
        final String mToolbarTitle;
        final int mTitleAlpha;

        public SavedState(Parcel source) {
            super(source);
            mVisible = source.readByte() != 0;
            mToolbarTitle = source.readString();
            mTitleAlpha = source.readInt();
        }

        public SavedState(Parcelable superState, boolean visible, String toolBarTitle, int titleAlpha) {
            super(superState);
            this.mVisible = visible;
            this.mToolbarTitle = toolBarTitle;
            this.mTitleAlpha = titleAlpha;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (mVisible ? 1 : 0));
            out.writeString(mToolbarTitle);
            out.writeInt(mTitleAlpha);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public static <V extends View> MergedAppBarLayoutBehavior from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params)
                .getBehavior();
        if (!(behavior instanceof MergedAppBarLayoutBehavior)) {
            throw new IllegalArgumentException("The view is not associated with " +
                    "MergedAppBarLayoutBehavior");
        }
        return (MergedAppBarLayoutBehavior) behavior;
    }
}