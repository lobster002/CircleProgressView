package com.guangyu.ptcrile;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * 如果觉得进度太快请设置{@link #EVERY_INVALIDATE_INTERVAL} 单位是毫秒 <br>
 * 要修改文字内容请调用{@link #changeNeedStr(String)}<br>
 * <code>Activity onDestroy</code>的时候,请调用{@link #setShouldStopAnim(boolean)},防止内存泄露<br>
 */
public class CircleProgressView extends View {
    /**
     * 每次自动刷新时间,可以自己修改(请暂不要自己修改)
     */
    private static final long EVERY_INVALIDATE_INTERVAL = 10L;
    /**
     * 完成一圈进度时间(请暂不要自己修改)
     */
    private static final long COMPLETE_PROGRESS_TIME = 30 * 1000L;
    /**
     * 旋转总的进度
     */
    private static final float COMPLETE_PROGRESS_DEGREE = 360F;
    /**
     * 最开始的弧度
     */
    private static final float DEFAULT_PROGRESS_DEGREE = -90F;
    /**
     * 圆环画笔
     */
    private Paint mRingPaint;
    /**
     * 圆环画笔
     */
    private Paint mBgCirclePaint;
    /**
     * 圆环上面小圆环画笔
     */
    private Paint mSmallCirclePaint;
    /**
     * 圆环上面文字画笔
     */
    private Paint mSmallTextPaint;
    /**
     * 中间文字画笔
     */
    private Paint mTextPaint;
    private static final int COLOR_GREEN = Color.GREEN;
    private static final int COLOR_RED = Color.RED;
    private volatile String mTextStr;
    private float mTextSize;
    private float mSmallTextSize;
    private float mSmallCircleRadius;
    private int mSmallTextColor;
    private float mRingStrokeWidth;
    private RectF mRectF;
    private int mWidth;
    private int mHeight;
    private float sweepAngle;
    /**
     * 走完一圈需要的总的刷新次数
     */
    private float radius;
    /**
     * 当前已经刷新的次数
     */
    private float mTextX;
    private float mTextY;
    /**
     * 可以考虑在Activity的onDestroy方法里面调用,防止内存泄露(这里比较重要)
     */
    private boolean mShouldStopAnim;


    private OnDrawCircleListener listener = null;

    private volatile long startTime = 0l;
    private volatile long theStartTime = 0l;

    private Object lock = new Object();

    public CircleProgressView(Context context) {
        super(context);
        init(context, null);
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView);
        mTextSize = ta.getDimensionPixelSize(R.styleable.CircleProgressView_textSize, sp2px(context, 20));
        mSmallTextSize = ta.getDimensionPixelSize(R.styleable.CircleProgressView_smallTextSize, sp2px(context, 10));
        mRingStrokeWidth = ta.getDimension(R.styleable.CircleProgressView_ringStrokeWidth, dip2px(5));
        mTextStr = ta.getString(R.styleable.CircleProgressView_textStr);
        mSmallCircleRadius = ta.getDimension(R.styleable.CircleProgressView_smallCircleRadius, dip2px(8));
        mSmallTextColor = ta.getColor(R.styleable.CircleProgressView_smallTextColor, Color.WHITE);
        ta.recycle();

        mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(mRingStrokeWidth);
        mRingPaint.setColor(COLOR_GREEN);

        mBgCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSmallCirclePaint = new Paint(mBgCirclePaint);
        mSmallCirclePaint.setColor(COLOR_RED);
        mBgCirclePaint.setColor(Color.WHITE);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mSmallTextPaint = new Paint(mTextPaint);
        mSmallTextPaint.setTextSize(mSmallTextSize);
        mSmallTextPaint.setColor(mSmallTextColor);

        startTime = theStartTime = System.currentTimeMillis();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mRectF = new RectF(mRingStrokeWidth, mRingStrokeWidth, mWidth - mRingStrokeWidth, mHeight - mRingStrokeWidth);
    }

    private void calculateNeedSomething() {
        Rect rect = new Rect();
        if (TextUtils.isEmpty(mTextStr)) {
            mTextPaint.getTextBounds(" ", 0, 1, rect);
        } else {
            mTextPaint.getTextBounds(mTextStr, 0, mTextStr.length(), rect);
        }
        mTextX = mWidth / 2F - rect.width() / 2F;
        mTextY = mHeight / 2F + rect.height() / 2F;
        radius = Math.min(mWidth, mHeight) / 2F - 3 * mRingStrokeWidth / 2F;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 这里让颜色渐变
        synchronized (lock) {
            startTime = System.currentTimeMillis();
        }
        long deltaTime = startTime - theStartTime;
        sweepAngle = (float) (deltaTime * 0.012);//偏转角度
        float fraction = sweepAngle / COMPLETE_PROGRESS_DEGREE;
        mRingPaint.setColor(getCurrentColor(fraction));

        calculateNeedSomething();
        canvas.drawCircle(mRectF.centerX(), mRectF.centerY(), radius, mBgCirclePaint);
        if (!TextUtils.isEmpty(mTextStr)) {
            canvas.drawText(mTextStr, mTextX, mTextY, mTextPaint);
        }
        canvas.drawArc(mRectF, DEFAULT_PROGRESS_DEGREE, sweepAngle, false, mRingPaint);

        if (sweepAngle > 300.0f) {
            int index = (int) ((360 - sweepAngle) / 12 + 1);
            String text = String.valueOf(index);
            // 第四象限(先转换为角度,然后根据圆里面的关系去换算为坐标)
            double angle = Math.toRadians(sweepAngle - 270);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            float x1 = (float) (mRectF.centerX() - radius * cos);
            float y1 = (float) (mRectF.centerY() - radius * sin) - dip2px(5);
            // 调节位置,因为可能超过显示区域
            if (index > 1) {
                canvas.drawCircle(x1, y1, mSmallCircleRadius, mSmallCirclePaint);
                Rect rect = new Rect();
                mSmallTextPaint.getTextBounds(text, 0, text.length(), rect);
                canvas.drawText(text, x1 - rect.width() / 2F, y1 + rect.height() / 2F, mSmallTextPaint);
            } else if (index == 1) {
                // 调节"1"的位置
                dip2px((sweepAngle - 348.0f) / 2);
                canvas.drawCircle(x1, y1, mSmallCircleRadius - dip2px((sweepAngle - 348.0f) / 2), mSmallCirclePaint);
                Rect rect = new Rect();
                mSmallTextPaint.getTextBounds(text, 0, text.length(), rect);
                canvas.drawText(text, x1 - rect.width() / 2F, y1 + rect.height() / 2F, mSmallTextPaint);
            }
        }

        if (sweepAngle > 360.0f) {
            if (null != listener) {
                listener.callback();
            }
            synchronized (lock) {
                theStartTime = startTime = System.currentTimeMillis();
            }
        }
        if (!mShouldStopAnim) {
            postInvalidateDelayed(EVERY_INVALIDATE_INTERVAL);
        }
    }


    public void setCurrentProgress(long value) {
        if (value < 0) {
            return;
        }
        if (value > COMPLETE_PROGRESS_TIME) {
            value %= COMPLETE_PROGRESS_TIME;
        }
        synchronized (lock) {
            startTime = theStartTime = System.currentTimeMillis();
            theStartTime -= value;
        }
    }

    public boolean isShouldStopAnim() {
        return mShouldStopAnim;
    }

    public void setShouldStopAnim(boolean mShouldStopAnim) {
        this.mShouldStopAnim = mShouldStopAnim;
    }

    /**
     * 这里根据需要去改变中间需要显示的字符串
     *
     * @param str
     */
    public void changeNeedStr(String str) {
        mTextStr = str;
    }

    /**
     * 将dip或dp值转换为px值，保证尺寸大小不变
     *
     * @param dipValue
     * @return
     */
    public int dip2px(float dipValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param context
     * @param spValue
     * @return
     */
    public int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public int evaluate(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 0xff;
        int startR = (startValue >> 16) & 0xff;
        int startG = (startValue >> 8) & 0xff;
        int startB = startValue & 0xff;

        int endA = (endValue >> 24) & 0xff;
        int endR = (endValue >> 16) & 0xff;
        int endG = (endValue >> 8) & 0xff;
        int endB = endValue & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }


    private int getCurrentColor(float progress) {
        if (progress < 0.5f) {
            return evaluate(progress * 2, Color.GREEN, Color.YELLOW);
        }
        return evaluate((progress - 0.5f) * 2, Color.YELLOW, Color.RED);
    }


    public void setOnDrawCircleListener(OnDrawCircleListener onDrawCircleListener) {
        listener = onDrawCircleListener;
    }
}
