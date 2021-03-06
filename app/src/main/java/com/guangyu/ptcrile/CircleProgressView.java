
package com.guangyu.ptcrile;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * 要修改文字内容请调用{@link #setCenterText(String)}
 * 修改进度 调用{@link #setCurrentProgress(long)} 单位为毫秒  取值为 0 ~ 30 * 1000
 * 父布局被销毁时  建议调用一下  {@link #stopAnim()} 防止内存泄露
 * 关于暂停刷新界面 {@link #stopAnim()}  返回当前的 进度 progress  需要保存进度  调用 {@link #startAnim(long)} 参数为返回的进度值
 */
public class CircleProgressView extends View {

    private final long COMPLETE_PROGRESS_TIME = 30 * 1000L;//完成一圈进度时间(请暂不要自己修改)

    private final float DEFAULT_PROGRESS_DEGREE = -90F;//最开始的弧度

    private Paint mRingPaint;//圆环画笔
    private Paint mBgCirclePaint;//背景圆
    private Paint mSmallCirclePaint;//圆环上面小圆环画笔
    private Paint mSmallTextPaint;//圆环上面文字画笔
    private Paint insideCirclePaint;//园内环画笔
    private Paint mTextPaint;//中间文字画笔

    private volatile String mTextStr = " ";//中间文本

    private float mTextSize;//中间文本字体大小
    private float mSmallTextSize;//小圆文字字体大小
    private float mSmallCircleRadius;//小圆半径
    private float mRingStrokeWidth;//园内环宽

    private RectF insideRectF;
    private float sweepAngle;//偏转角度

    private int centerX = 0;//中心点X值
    private int centerY = 0;//中心点Y值

    private volatile int currentColor = 0;//当前进度条颜色

    private int deltaCircleValue = 12;//内外圆半径差

    private float radius;//进度半径

    private volatile long startTime = 0l;//本次绘制开始时间
    private volatile long theStartTime = 0l;//本次进度条开始时间

    private int bgColor = Color.parseColor("#33000000");//背景颜色

    //文本字体大小  单位sp
    private float SMALL_TEXT_SIZE = 15.0f;
    //    private float CENTER_TEXT_SIZE = 55.0f;
    private float CENTER_TEXT_SIZE = 40.0f;
    private Paint.FontMetrics fontMetrics = null;

    private Object lock = new Object();

    public CircleProgressView(Context context) {
        super(context);
        init();
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mTextSize = sp2px(CENTER_TEXT_SIZE);
        mSmallTextSize = sp2px(SMALL_TEXT_SIZE);
        mRingStrokeWidth = dip2px(10);
        mSmallCircleRadius = dip2px(11);

        mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(mRingStrokeWidth);
        mRingPaint.setStrokeCap(Paint.Cap.ROUND);
        mRingPaint.setColor(Color.GREEN);

        insideCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        insideCirclePaint.setStyle(Paint.Style.STROKE);
        insideCirclePaint.setStrokeWidth(dip2px(10));
        insideCirclePaint.setColor(bgColor);

        mBgCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgCirclePaint.setColor(bgColor);

        mSmallCirclePaint = new Paint(mBgCirclePaint);
        mSmallCirclePaint.setColor(Color.RED);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setStyle(Paint.Style.STROKE);

        mSmallTextPaint = new Paint(mTextPaint);
        mSmallTextPaint.setTextSize(mSmallTextSize);
        mSmallTextPaint.setColor(Color.WHITE);

        mTextPaint.setTypeface(Typeface.DEFAULT);//默认字体
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        startTime = theStartTime = System.currentTimeMillis();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w >> 1;
        centerY = h >> 1;
        radius = (centerX > centerY ? centerY : centerX);
        insideRectF = new RectF(centerX - radius + dip2px(deltaCircleValue), centerY - radius + dip2px(deltaCircleValue),
                centerX + radius - dip2px(deltaCircleValue), centerY + radius - dip2px(deltaCircleValue));
    }


    private long deltaTime = 0;
    private float fraction = 0.0f;
    @Override
    protected void onDraw(Canvas canvas) {
        doBefore();//绘制之前
        drawBg(canvas);//绘制背景
        drawCircle(canvas);//绘制圆形进度
        drawText(canvas);//绘制 文本
        drawDot(canvas);//绘制小圆点
        doAfter();//绘制之后
    }

    private void doBefore() {//绘制之前的准备工作 在这里处理
        synchronized (lock) {
            startTime = System.currentTimeMillis();
        }
    }

    private void drawBg(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, radius, mBgCirclePaint);
    }

    private void drawCircle(Canvas canvas) {
        deltaTime = startTime - theStartTime;
        sweepAngle = (float) (deltaTime * 0.012);//偏转角度
        fraction = sweepAngle / 360.0f;
        currentColor = getCurrentColor(fraction);
        mRingPaint.setColor(currentColor);

        canvas.drawArc(insideRectF, DEFAULT_PROGRESS_DEGREE, 360.0f, false, insideCirclePaint);
        canvas.drawArc(insideRectF, DEFAULT_PROGRESS_DEGREE, sweepAngle, false, mRingPaint);
    }

    private int baseLine = 0;
    private void drawText(Canvas canvas) {
        if (!TextUtils.isEmpty(mTextStr)) {
            fontMetrics = mTextPaint.getFontMetrics();
            baseLine = (int) (((centerY << 1) - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top);
            canvas.drawText(mTextStr, centerX - mTextPaint.measureText(mTextStr) / 2, baseLine, mTextPaint);
        }
    }

    private int index = 0;
    private double tmpAngle = 0;
    private double sin = 0;
    private double cos = 0;
    private float x1 = 0;
    private float y1 = 0;
    private String text = " ";
    private Rect rect = new Rect();
    private void drawDot(Canvas canvas) {
        if (sweepAngle > 300.0f) {
            index = (int) ((360 - sweepAngle) / 12 + 1);
            // 第四象限(先转换为角度,然后根据圆里面的关系去换算为坐标)
            tmpAngle = Math.toRadians(sweepAngle - 270f);
            sin = Math.sin(tmpAngle);
            cos = Math.cos(tmpAngle);
            x1 = (float) (centerX - (radius - dip2px(deltaCircleValue)) * cos);
            y1 = (float) (centerY - (radius - dip2px(deltaCircleValue)) * sin);
            mSmallCirclePaint.setColor(currentColor);

            // 调节位置,  是为了让文字居中
            text = String.valueOf(index);
            if (sweepAngle > 348.0f) {//1
                float scale = mSmallCircleRadius - dip2px((sweepAngle - 348.0f) / 2);
                canvas.drawCircle(x1, y1, scale, mSmallCirclePaint);
                if (0 != index) {//0的时候显示无意义
                    mSmallTextPaint.setTextSize(mSmallTextSize - sp2px((sweepAngle - 348) / 1.2f));
                    mSmallTextPaint.getTextBounds(text, 0, text.length(), rect);
                    canvas.drawText(text, x1 - mSmallTextPaint.measureText(text) / 2F, y1 + rect.height() / 2F, mSmallTextPaint);
                }
            } else {//大于1
                canvas.drawCircle(x1, y1, mSmallCircleRadius, mSmallCirclePaint);
                mSmallTextPaint.setTextSize(mSmallTextSize);
                mSmallTextPaint.getTextBounds(text, 0, text.length(), rect);
                canvas.drawText(text, x1 -  mSmallTextPaint.measureText(text) / 2F, y1 + rect.height() / 2F, mSmallTextPaint);
            }
        }

    }

    private void doAfter() {//绘制完毕之后 其他的逻辑
        if (sweepAngle > 360.0f) {
            if (null != listener) {
                listener.callback();
            }
            synchronized (lock) {
                theStartTime = startTime = System.currentTimeMillis();
            }
        }
        if (!needStop) {
//            SystemClock.sleep(15);// 需要的时候在这里设置休眠   节省性能
            invalidate();
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

    private boolean needStop = false;//状态标记位

    public boolean isAnimStopped() {//判断是否 停止更新
        return needStop;
    }

    public void startAnim() {
        needStop = false;
        postInvalidate();
    }


    public void startAnim(long currentProgress) {
        needStop = false;
        setCurrentProgress(currentProgress);
        postInvalidate();
    }

    public long stopAnim() {
        needStop = true;
        long currentTime = System.currentTimeMillis();
        return currentTime - theStartTime;
    }

    /*
    * 设置中间文本显示内容
    * */
    public void setCenterText(String str) {
        mTextStr = str.trim();
    }

    //dp转px
    public int dip2px(float dipValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    //sp转px
    public int sp2px(float spValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    //因为该方法调用频繁，变量声明在这里，减少每次调用时，为临时变量分配内存次数
    int startA, startR, startG, startB, endA, endR, endG, endB;

    //颜色渐变算法 写的还不错
    public int evaluate(float fraction, int startValue, int endValue) {
        startA = (startValue >> 24) & 0xff;
        startR = (startValue >> 16) & 0xff;
        startG = (startValue >> 8) & 0xff;
        startB = startValue & 0xff;

        endA = (endValue >> 24) & 0xff;
        endR = (endValue >> 16) & 0xff;
        endG = (endValue >> 8) & 0xff;
        endB = endValue & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }


    private final int COLOR_BEGIN = Color.parseColor("#00ffa8");
    private final int COLOR_MEDUIM = Color.parseColor("#ffdd3c");
    private final int COLOR_END = Color.parseColor("#ff3c61");
    //分段渐变 返回当前进度下 对应的颜色
    private int getCurrentColor(float progress) {
        if (progress <= 0.5f) {
            return COLOR_BEGIN;
        } else if (progress >= 0.5f && progress < 0.75f) {
            return evaluate((progress - 0.5f) * 4, COLOR_BEGIN, COLOR_MEDUIM);
        } else {
            return evaluate((progress - 0.75f) * 4, COLOR_MEDUIM, COLOR_END);
        }
    }

    private OnDrawCircleListener listener = null;   //回调接口

    public void setOnDrawCircleListener(OnDrawCircleListener onDrawCircleListener) {
        listener = onDrawCircleListener;
    }
}