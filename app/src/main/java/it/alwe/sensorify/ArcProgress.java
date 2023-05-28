package it.alwe.sensorify;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import java.text.DecimalFormat;

public class ArcProgress extends View {
    private Paint paint;
    protected Paint textPaint;
    private final RectF rectF = new RectF();

    private float strokeWidth;
    private float bottomTextSize;
    private String bottomText;
    private String text;
    private float textSize;
    private int textColor;
    private int currentProgress = 0;
    private float progress = 0;
    private int max;
    private int fillStrokeColor;
    private int unfilledStrokeColor;
    private float arcAngle;
    private String suffixText;
    private float suffixTextSize;
    private float suffixTextPadding;
    private final String defaultSuffixText = "%";
    private final float defaultSuffixTextSize;
    private final float defaultSuffixTextPadding;
    private String belowText;
    private float belowTextSize;
    private float belowTextPadding;
    private final String defaultBelowText = "Example";
    private final float defaultBelowTextSize;
    private final float defaultBelowTextPadding;
    private float arcBottomHeight;
    private final int defaultFillColor = getResources().getColor(R.color.colorPrimary);
    private final int defaultUnfilledColor = getResources().getColor(R.color.totalBlack);
    private final int defaultTextColor = getResources().getColor(R.color.totalBlack);
    private final float defaultStrokeWidth;
    private final String defaultBottomText = "Example";
    private final float defaultBottomTextSize;
    private final int defaultMax = 100;
    private final float defaultAngle = 360 * 0.75f;
    private final float defaultTextSize;
    private final int defaultMinSize;

    private static final String INSTANCE_STATE = "savedInstance";
    private static final String INSTANCE_STROKE_WIDTH = "strokeWidth";
    private static final String INSTANCE_SUFFIX_TEXT_SIZE = "suffixTextSize";
    private static final String INSTANCE_SUFFIX_TEXT_PADDING = "suffixTextPadding";
    private static final String INSTANCE_BELOW_TEXT_SIZE = "belowTextSize";
    private static final String INSTANCE_BELOW_TEXT_PADDING = "belowTextPadding";
    private static final String INSTANCE_BOTTOM_TEXT_SIZE = "bottomTextSize";
    private static final String INSTANCE_BOTTOM_TEXT = "bottomText";
    private static final String INSTANCE_TEXT_SIZE = "textSize";
    private static final String INSTANCE_TEXT_COLOR = "textColor";
    private static final String INSTANCE_FILL_STROKE_COLOR = "fillStrokeColor";
    private static final String INSTANCE_UNFILLED_STROKE_COLOR = "unfilledStrokeColor";
    private static final String INSTANCE_ARC_ANGLE = "angle";
    private static final String INSTANCE_PROGRESS = "progress";
    private static final String INSTANCE_MAX = "max";
    private static final String INSTANCE_SUFFIX = "suffix";
    private static final String INSTANCE_BELOW = "below";

    public ArcProgress(Context context) { this(context, null); }

    public ArcProgress(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public ArcProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        defaultMinSize = (int) ArcUtils.dp2px(getResources(), 100);
        defaultTextSize = ArcUtils.sp2px(getResources(), 40);
        defaultSuffixTextSize = ArcUtils.sp2px(getResources(), 15);
        defaultSuffixTextPadding = ArcUtils.dp2px(getResources(), 10);
        defaultBelowTextSize = ArcUtils.sp2px(getResources(), 12);
        defaultBelowTextPadding = ArcUtils.dp2px(getResources(), 10);
        defaultBottomTextSize = ArcUtils.sp2px(getResources(), 12);
        defaultStrokeWidth = ArcUtils.dp2px(getResources(), 6);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ArcProgress, defStyleAttr, 0);
        initByAttributes(attributes);
        attributes.recycle();

        initPainters();
    }

    protected void initByAttributes(TypedArray attributes) {
        fillStrokeColor = attributes.getColor(R.styleable.ArcProgress_fill_color, defaultFillColor);
        unfilledStrokeColor = attributes.getColor(R.styleable.ArcProgress_unfilled_color, defaultUnfilledColor);
        textColor = attributes.getColor(R.styleable.ArcProgress_text_color, defaultTextColor);
        textSize = attributes.getDimension(R.styleable.ArcProgress_text_size, defaultTextSize);
        arcAngle = attributes.getFloat(R.styleable.ArcProgress_angle, defaultAngle);
        setMax(attributes.getInt(R.styleable.ArcProgress_max, defaultMax));
        setProgress(attributes.getFloat(R.styleable.ArcProgress_progress, 0));
        strokeWidth = attributes.getDimension(R.styleable.ArcProgress_stroke_width, defaultStrokeWidth);
        suffixTextSize = attributes.getDimension(R.styleable.ArcProgress_suffix_text_size, defaultSuffixTextSize);
        suffixText = TextUtils.isEmpty(attributes.getString(R.styleable.ArcProgress_suffix_text)) ? defaultSuffixText : attributes.getString(R.styleable.ArcProgress_suffix_text);
        suffixTextPadding = attributes.getDimension(R.styleable.ArcProgress_suffix_text_padding, defaultSuffixTextPadding);
        belowTextSize = attributes.getDimension(R.styleable.ArcProgress_below_text_size, defaultBelowTextSize);
        belowText = TextUtils.isEmpty(attributes.getString(R.styleable.ArcProgress_below_text)) ? defaultBelowText : attributes.getString(R.styleable.ArcProgress_below_text);
        belowTextPadding = attributes.getDimension(R.styleable.ArcProgress_below_text_padding, defaultBelowTextPadding);
        bottomTextSize = attributes.getDimension(R.styleable.ArcProgress_bottom_text_size, defaultBottomTextSize);
        bottomText = TextUtils.isEmpty(attributes.getString(R.styleable.ArcProgress_bottom_text)) ? defaultBottomText : attributes.getString(R.styleable.ArcProgress_bottom_text);
    }

    protected void initPainters() {
        textPaint = new TextPaint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);

        paint = new Paint();
        paint.setColor(defaultUnfilledColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void invalidate() {
        initPainters();
        super.invalidate();
    }

    public float getStrokeWidth() { return strokeWidth; }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        this.invalidate();
    }

    public float getSuffixTextSize() { return suffixTextSize; }

    public void setSuffixTextSize(float suffixTextSize) {
        this.suffixTextSize = suffixTextSize;
        this.invalidate();
    }

    public float getBelowTextSize() { return belowTextSize; }

    public void setBelowTextSize(float belowTextSize) {
        this.belowTextSize = belowTextSize;
        this.invalidate();
    }

    public String getBottomText() { return bottomText; }

    public void setBottomText(String bottomText) {
        this.bottomText = bottomText;
        this.invalidate();
    }

    public float getProgress() { return progress; }

    public void setProgress(float progress) {
        this.progress = Float.parseFloat(new DecimalFormat("#.##").format(progress).replace(",", "."));
        if (this.progress > getMax()) this.progress %= getMax();
        currentProgress = 0;
        invalidate();
    }

    public int getMax() { return max; }

    public void setMax(int max) {
        if (max > 0) {
            this.max = max;
            invalidate();
        }
    }

    public float getBottomTextSize() { return bottomTextSize; }

    public void setBottomTextSize(float bottomTextSize) {
        this.bottomTextSize = bottomTextSize;
        this.invalidate();
    }

    public String getText() { return text; }

    public void setText(String text) {
        this.text = text;
        this.invalidate();
    }

    public void setDefaultText() {
        text = String.valueOf(getProgress());
        invalidate();
    }

    public float getTextSize() { return textSize; }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
        this.invalidate();
    }

    public int getTextColor() { return textColor; }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
        this.invalidate();
    }

    public int getFillStrokeColor() { return fillStrokeColor; }

    public void setFinishedStrokeColor(int fillStrokeColor) {
        this.fillStrokeColor = fillStrokeColor;
        this.invalidate();
    }

    public int getUnfilledStrokeColor() { return unfilledStrokeColor; }

    public void setUnfinishedStrokeColor(int unfilledStrokeColor) {
        this.unfilledStrokeColor = unfilledStrokeColor;
        this.invalidate();
    }

    public float getArcAngle() { return arcAngle; }

    public void setArcAngle(float arcAngle) {
        this.arcAngle = arcAngle;
        this.invalidate();
    }

    public String getSuffixText() { return suffixText; }

    public void setSuffixText(String suffixText) {
        this.suffixText = suffixText;
        this.invalidate();
    }

    public float getSuffixTextPadding() { return suffixTextPadding; }

    public void setSuffixTextPadding(float suffixTextPadding) {
        this.suffixTextPadding = suffixTextPadding;
        this.invalidate();
    }

    public String getBelowText() { return belowText; }

    public void setBelowText(String belowText) {
        this.belowText = belowText;
        this.invalidate();
    }

    public float getBelowTextPadding() { return belowTextPadding; }

    public void setBelowTextPadding(float belowTextPadding) {
        this.belowTextPadding = belowTextPadding;
        this.invalidate();
    }

    @Override
    protected int getSuggestedMinimumHeight() { return defaultMinSize; }

    @Override
    protected int getSuggestedMinimumWidth() { return defaultMinSize; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        rectF.set(strokeWidth / 2f, strokeWidth / 2f, width - strokeWidth / 2f, height - strokeWidth / 2f);
        float radius = width / 2f;
        float angle = (360 - arcAngle) / 2f;
        arcBottomHeight = radius * (float) (1 - Math.cos(angle / 180 * Math.PI));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float startAngle = 270 - arcAngle / 2f;
        float finishedSweepAngle = currentProgress / (float) getMax() * arcAngle;
        float finishedStartAngle = startAngle;
        if (progress == 0) finishedStartAngle = 0.01f;
        paint.setColor(unfilledStrokeColor);
        canvas.drawArc(rectF, startAngle, arcAngle, false, paint);
        paint.setColor(fillStrokeColor);
        canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint);

        String text = String.valueOf(currentProgress);
        if (!TextUtils.isEmpty(text)) {
            textPaint.setColor(textColor);
            textPaint.setTextSize(textSize);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            float textHeight = textPaint.descent() + textPaint.ascent();
            float textBaseline = (getHeight() - textHeight) / 2.0f;
            canvas.drawText(text, (getWidth() - textPaint.measureText(text)) / 2.0f, textBaseline, textPaint);
            textPaint.setTextSize(suffixTextSize);
            float suffixHeight = textPaint.descent() + textPaint.ascent();
            canvas.drawText(suffixText, getWidth() / 2.0f  + textPaint.measureText(text) + suffixTextPadding, textBaseline + textHeight - suffixHeight, textPaint);
            textPaint.setTextSize(belowTextSize);
            float belowHeight = textPaint.descent() + textPaint.ascent();
            canvas.drawText(belowText, (getWidth() - textPaint.measureText(belowText)) / 2.0f, textBaseline - textHeight + belowHeight, textPaint);
        }
        if (arcBottomHeight == 0) {
            float radius = getWidth() / 2f;
            float angle = (360 - arcAngle) / 2f;
            arcBottomHeight = radius * (float) (1 - Math.cos(angle / 180 * Math.PI));
        }
        if (!TextUtils.isEmpty(bottomText)) {
            textPaint.setTextSize(bottomTextSize);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            float bottomTextBaseline = getHeight() - arcBottomHeight - (textPaint.descent() + textPaint.ascent()) / 2;
            canvas.drawText(bottomText, (getWidth() - textPaint.measureText(bottomText)) / 2.0f, bottomTextBaseline, textPaint);
        }
        if (currentProgress < progress) {
            currentProgress++;
            invalidate();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putFloat(INSTANCE_STROKE_WIDTH, getStrokeWidth());
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_SIZE, getSuffixTextSize());
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_PADDING, getSuffixTextPadding());
        bundle.putFloat(INSTANCE_BOTTOM_TEXT_SIZE, getBottomTextSize());
        bundle.putString(INSTANCE_BOTTOM_TEXT, getBottomText());
        bundle.putFloat(INSTANCE_TEXT_SIZE, getTextSize());
        bundle.putInt(INSTANCE_TEXT_COLOR, getTextColor());
        bundle.putFloat(INSTANCE_PROGRESS, getProgress());
        bundle.putInt(INSTANCE_MAX, getMax());
        bundle.putInt(INSTANCE_FILL_STROKE_COLOR, getFillStrokeColor());
        bundle.putInt(INSTANCE_UNFILLED_STROKE_COLOR, getUnfilledStrokeColor());
        bundle.putFloat(INSTANCE_ARC_ANGLE, getArcAngle());
        bundle.putString(INSTANCE_SUFFIX, getSuffixText());
        bundle.putString(INSTANCE_BELOW, getBelowText());
        bundle.putFloat(INSTANCE_BELOW_TEXT_SIZE, getBelowTextSize());
        bundle.putFloat(INSTANCE_BELOW_TEXT_PADDING, getBelowTextPadding());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            strokeWidth = bundle.getFloat(INSTANCE_STROKE_WIDTH);
            suffixTextSize = bundle.getFloat(INSTANCE_SUFFIX_TEXT_SIZE);
            suffixTextPadding = bundle.getFloat(INSTANCE_SUFFIX_TEXT_PADDING);
            bottomTextSize = bundle.getFloat(INSTANCE_BOTTOM_TEXT_SIZE);
            bottomText = bundle.getString(INSTANCE_BOTTOM_TEXT);
            textSize = bundle.getFloat(INSTANCE_TEXT_SIZE);
            textColor = bundle.getInt(INSTANCE_TEXT_COLOR);
            setMax(bundle.getInt(INSTANCE_MAX));
            setProgress(bundle.getFloat(INSTANCE_PROGRESS));
            fillStrokeColor = bundle.getInt(INSTANCE_FILL_STROKE_COLOR);
            unfilledStrokeColor = bundle.getInt(INSTANCE_UNFILLED_STROKE_COLOR);
            suffixText = bundle.getString(INSTANCE_SUFFIX);
            belowText = bundle.getString(INSTANCE_BELOW);
            belowTextSize = bundle.getFloat(INSTANCE_BELOW_TEXT_SIZE);
            belowTextPadding = bundle.getFloat(INSTANCE_BELOW_TEXT_PADDING);
            initPainters();
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }
}