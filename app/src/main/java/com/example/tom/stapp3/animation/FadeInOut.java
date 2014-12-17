package com.example.tom.stapp3.animation;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * Created by Tom on 17/12/2014.
 */
public class FadeInOut {
    private final View view;

    public FadeInOut(final View view) {
        this.view = view;
    }

    public boolean isOpen() {
        return (view.getVisibility() == View.VISIBLE);
    }

    public Animation getToggleAnimation() {
        if(view.getVisibility() == View.INVISIBLE) {
            Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setInterpolator(new AccelerateInterpolator());
            fadeIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            return fadeIn;
        } else {
            Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setInterpolator(new AccelerateInterpolator());
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            return fadeOut;
        }
    }

    public void toggle() {
        view.startAnimation(getToggleAnimation());
    }
}
