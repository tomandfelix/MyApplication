package com.example.tom.stapp3.animation;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;


/**
 * Created by Tom on 16/12/2014.
 */
public class ExpandCollapse {
    private final View view;

    public ExpandCollapse(final View view) {
        this.view = view;
    }

    public boolean isOpen() {
        return (view.getVisibility() == View.VISIBLE);
    }

    public Animation getToggleAnimation() {
        if(view.getVisibility() == View.GONE) {
            view.measure(View.MeasureSpec.makeMeasureSpec(LinearLayout.LayoutParams.MATCH_PARENT, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(LinearLayout.LayoutParams.MATCH_PARENT, View.MeasureSpec.EXACTLY));
            final int targetHeight = view.getMeasuredHeight();
            view.getLayoutParams().height = 0;
            view.setVisibility(View.VISIBLE);
            Animation expand = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    view.getLayoutParams().height = interpolatedTime == 1 ? LinearLayout.LayoutParams.MATCH_PARENT : (int) (targetHeight * interpolatedTime);
                    view.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            expand.setDuration((int) (targetHeight / view.getContext().getResources().getDisplayMetrics().density));
            return expand;
        } else {
            final int initialHeight = view.getMeasuredHeight();
            Animation collapse = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    if(interpolatedTime == 1) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                        view.requestLayout();
                    }
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            collapse.setDuration((int) (initialHeight / view.getContext().getResources().getDisplayMetrics().density));
            return collapse;
        }
    }

    public void toggle() {
        view.startAnimation(getToggleAnimation());
    }
}