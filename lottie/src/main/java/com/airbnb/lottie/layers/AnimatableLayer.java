package com.airbnb.lottie.layers;

import android.graphics.PointF;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import com.airbnb.lottie.animation.KeyframeAnimation;
import com.airbnb.lottie.utils.ScaleXY;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnimatableLayer extends JComponent {

    private final KeyframeAnimation.AnimationListener<Integer> integerChangedListener = new KeyframeAnimation.AnimationListener<Integer>() {
        @Override
        public void onValueChanged(Integer progress) {
            repaint();
        }
    };
    private final KeyframeAnimation.AnimationListener<Float> floatChangedListener = new KeyframeAnimation.AnimationListener<Float>() {
        @Override
        public void onValueChanged(Float progress) {
            repaint();
        }
    };
    private final KeyframeAnimation.AnimationListener<ScaleXY> scaleChangedListener = new KeyframeAnimation.AnimationListener<ScaleXY>() {
        @Override
        public void onValueChanged(ScaleXY progress) {
            repaint();
        }
    };
    private final KeyframeAnimation.AnimationListener<PointF> pointChangedListener = new KeyframeAnimation.AnimationListener<PointF>() {
        @Override
        public void onValueChanged(PointF progress) {
            repaint();
        }
    };

    final List<AnimatableLayer> layers = new ArrayList<>();
    @Nullable
    private AnimatableLayer parentLayer;

    private KeyframeAnimation<PointF> position;
    private KeyframeAnimation<PointF> anchorPoint;
    /**
     * This should mimic CALayer#transform
     */
    private KeyframeAnimation<ScaleXY> transform;
    private KeyframeAnimation<Integer> alpha = null;
    private KeyframeAnimation<Float> rotation;

    private Color backgroundColor = new Color(0);
    private final List<KeyframeAnimation<?>> animations = new ArrayList<>();
    @FloatRange(from = 0f, to = 1f)
    private float progress;

    void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    void addAnimation(KeyframeAnimation<?> newAnimation) {
        animations.add(newAnimation);
    }

    void removeAnimation(KeyframeAnimation<?> animation) {
        animations.remove(animation);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D canvas = (Graphics2D) g;
        int saveCount = canvas.save();
        applyTransformForLayer(canvas, this);

        int backgroundAlpha = backgroundColor.getAlpha();
        if (backgroundAlpha != 0) {
            int alpha = backgroundAlpha;
            if (this.alpha != null) {
                alpha = alpha * this.alpha.getValue() / 255;
            }
            if (alpha > 0) {
                canvas.setPaint(backgroundColor);
                Rectangle bounds = getBounds();
                canvas.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }
        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).paintComponent(canvas);
        }
        canvas.restoreToCount(saveCount);
    }

    int saveCanvas(@Nullable Graphics2D canvas) {
        if (canvas == null) {
            return 0;
        }
        return canvas.save();
    }

    void restoreCanvas(@Nullable Graphics2D canvas, int count) {
        if (canvas == null) {
            return;
        }
        canvas.restoreToCount(count);
    }

    void applyTransformForLayer(@Nullable Graphics2D canvas, AnimatableLayer layer) {
        if (canvas == null) {
            return;
        }
        // TODO: Determine if these null checks are necessary.
        if (layer.position != null) {
            PointF position = layer.position.getValue();
            if (position.x != 0 || position.y != 0) {
                canvas.translate(position.x, position.y);
            }
        }

        if (layer.rotation != null) {
            float rotation = layer.rotation.getValue();
            if (rotation != 0f) {
                canvas.rotate(rotation);
            }
        }

        if (layer.transform != null) {
            ScaleXY scale = layer.transform.getValue();
            if (scale.getScaleX() != 1f || scale.getScaleY() != 1f) {
                canvas.scale(scale.getScaleX(), scale.getScaleY());
            }
        }

        if (layer.anchorPoint != null) {
            PointF anchorPoint = layer.anchorPoint.getValue();
            if (anchorPoint.x != 0 || anchorPoint.y != 0) {
                canvas.translate(-anchorPoint.x, -anchorPoint.y);
            }
        }
    }


    public void setAlpha(int alpha) {
        throw new IllegalArgumentException("This shouldn't be used.");
    }

    void setAlpha(KeyframeAnimation<Integer> alpha) {
        if (this.alpha != null) {
            removeAnimation(this.alpha);
            this.alpha.removeUpdateListener(integerChangedListener);
        }
        this.alpha = alpha;
        addAnimation(alpha);
        alpha.addUpdateListener(integerChangedListener);

        repaint();
    }

    public int getAlpha() {
        float alpha = this.alpha == null ? 1f : (this.alpha.getValue() / 255f);
        float parentAlpha = parentLayer == null ? 1f : (parentLayer.getAlpha() / 255f);
        return (int) (alpha * parentAlpha * 255);
    }

    void setAnchorPoint(KeyframeAnimation<PointF> anchorPoint) {
        if (this.anchorPoint != null) {
            removeAnimation(this.anchorPoint);
            this.anchorPoint.removeUpdateListener(pointChangedListener);
        }
        this.anchorPoint = anchorPoint;
        addAnimation(anchorPoint);
        anchorPoint.addUpdateListener(pointChangedListener);
    }

    void setPosition(KeyframeAnimation<PointF> position) {
        if (this.position != null) {
            removeAnimation(this.position);
            this.position.removeUpdateListener(pointChangedListener);
        }
        this.position = position;
        addAnimation(position);
        position.addUpdateListener(pointChangedListener);
    }

    void setTransform(KeyframeAnimation<ScaleXY> transform) {
        if (this.transform != null) {
            removeAnimation(this.transform);
            this.transform.removeUpdateListener(scaleChangedListener);
        }
        this.transform = transform;
        addAnimation(this.transform);
        transform.addUpdateListener(scaleChangedListener);
    }

    void setRotation(KeyframeAnimation<Float> rotation) {
        if (this.rotation != null) {
            removeAnimation(this.rotation);
            this.rotation.removeUpdateListener(floatChangedListener);
        }
        this.rotation = rotation;
        addAnimation(this.rotation);
        rotation.addUpdateListener(floatChangedListener);
    }

    void addLayer(AnimatableLayer layer) {
        layer.parentLayer = this;
        layers.add(layer);
        layer.setProgress(progress);
        if (this.alpha != null) {
            layer.setAlpha(this.alpha);
        }
        repaint();
    }

    void clearLayers() {
        layers.clear();
        repaint();
    }

    public void setProgress(@FloatRange(from = 0f, to = 1f) float progress) {
        this.progress = progress;
        for (int i = 0; i < animations.size(); i++) {
            animations.get(i).setProgress(progress);
        }

        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).setProgress(progress);
        }
    }

    public float getProgress() {
        return progress;
    }
}
