package com.planetkershaw.heatingtest.utils;
/*
Copyright 2014 Stephan Tittel and Yahoo Inc.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.planetkershaw.heatingtest.R;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Widget that lets users select a minimum and maximum value on a given numerical range.
 * The range value types can be one of Long, Double, Integer, Float, Short, Byte or BigDecimal.<br>
 * <br>
 * Improved {@link MotionEvent} handling for smoother use, anti-aliased painting for improved aesthetics.
 *
 * @param <T> The Number type of the range values. One of Long, Double, Integer, Float, Short, Byte or BigDecimal.
 * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
 * @author Peter Sinnott (psinnott@gmail.com)
 * @author Thomas Barrasso (tbarrasso@sevenplusandroid.org)
 * @author Alex Florescu (florescu@yahoo-inc.com)
 * @author Michael Keppler (bananeweizen@gmx.de)
 */
public class MultiSeekBar<T extends Number> extends ImageView {

    public static final Integer DEFAULT_MINIMUM = 0;
    public static final Integer DEFAULT_MAXIMUM = 100;
    private final int LINE_HEIGHT_IN_DP = 40;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int SEEKBAR_WIDTH = 40*24;

    private Bitmap thumbImage;

    private float thumbWidth;
    private float thumbHalfWidth;
    private float thumbHalfHeight;

    private T absoluteMinValue, absoluteMaxValue;
    private NumberType numberType;
    private double absoluteMinValuePrim, absoluteMaxValuePrim;

    private int pressedThumbIndex = -1;
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener<T> listener;
    private Fragment frag;

    public static final int DEFAULT_COLOR = Color.argb(0xFF, 0x33, 0xB5, 0xE5);
    public static final int GREEN_TEMP = Color.argb(0xff,0x99,0xff,0x66);
    /**
     * An invalid pointer id.
     */
    public static final int INVALID_POINTER_ID = 255;

    // Localized constants from MotionEvent for compatibility
    // with API < 8 "Froyo".
    public static final int ACTION_POINTER_UP = 0x6, ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;

    private float mDownMotionX;

    private int mActivePointerId = INVALID_POINTER_ID;

    private int mScaledTouchSlop;

    private boolean mIsDragging;

    private RectF mRect;

    private int startColor = DEFAULT_COLOR;

    private int id;

    private class ThumbNail {
        int index;
        double normalizedValue;
        String label;
        int prevColor = DEFAULT_COLOR;
        int color = DEFAULT_COLOR;
        Bitmap bmp;

        ThumbNail(int index) {
            this.index = index;
        }

        void createThumbnail() {
            Bitmap rightThumb = changeImageColor(BitmapFactory.decodeResource(getResources(), R.drawable.right_halfthumb), color);
            Bitmap leftThumb = changeImageColor(BitmapFactory.decodeResource(getResources(), R.drawable.left_halfthumb), prevColor);
            Bitmap hourGlass = overlay(leftThumb, rightThumb);
            Bitmap thumbNail = overlay(changeImageColor(BitmapFactory.decodeResource(getResources(), R.drawable.whitethumb),Color.WHITE),hourGlass);
            bmp = getResizedBitmap(thumbNail,40,40);
        }
}

    private ArrayList<ThumbNail>thumbs = new ArrayList();

    public MultiSeekBar(Context context, int id) {
        super(context);
        this.id = id;
        thumbs = new ArrayList<ThumbNail>();
        init();
    }

    public int addMarker () {
        int index = thumbs.size();
        thumbs.add(new ThumbNail(index));
        return index;
    }

    public int numTimers () {
        return thumbs.size();
    }

/*    public void createMarkers (int numMarkers)
    {

        for (int i=0; i<numMarkers; i++) {
            thumbs.add(new ThumbNail(thumbs.size()));
        }

    }*/

    public void setStartColor (int color) {
        startColor = color;
    }

/*    public void updateTimerColor (int index, int color) {
        thumbs.get(index).color = color;
        thumbs.get(index).createThumbnail();
    }
*/
    public void setTimerColors (int index, int prev, int color) {
        if (prev != 0) thumbs.get(index).prevColor = prev;
        if (color != 0) thumbs.get(index).color = color;
        thumbs.get(index).createThumbnail();
        invalidate();
    }

 //   private void init(Context context, AttributeSet attrs) {
    private void init() {

        thumbImage = createThumb();
        thumbWidth = thumbImage.getWidth();
        thumbHalfWidth = 0.5f * thumbWidth;
        thumbHalfHeight = 0.5f * thumbImage.getHeight();

        setRangeToDefaultValues();
        setValuePrimAndNumberType();

        float lineHeight = LINE_HEIGHT_IN_DP;
        mRect = new RectF(0, thumbHalfHeight - lineHeight / 2, SEEKBAR_WIDTH, thumbHalfHeight + lineHeight / 2);

        // make RangeSeekBar focusable. This solves focus handling issues in case EditText widgets are being used along with the RangeSeekBar within ScollViews.
        setFocusable(true);
        setFocusableInTouchMode(true);
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public void setRangeValues(T minValue, T maxValue) {
        this.absoluteMinValue = minValue;
        this.absoluteMaxValue = maxValue;
        setValuePrimAndNumberType();
    }

    @SuppressWarnings("unchecked")
    // only used to set default values when initialised from XML without any values specified
    private void setRangeToDefaultValues() {
        this.absoluteMinValue = (T) DEFAULT_MINIMUM;
        this.absoluteMaxValue = (T) DEFAULT_MAXIMUM;
        setValuePrimAndNumberType();
    }

    private void setValuePrimAndNumberType() {
        absoluteMinValuePrim = absoluteMinValue.doubleValue();
        absoluteMaxValuePrim = absoluteMaxValue.doubleValue();
        numberType = NumberType.fromNumber(absoluteMinValue);
    }

/*    public void resetSelectedValues() {
        setSelectedValue(0,absoluteMinValue);
        setSelectedValue(1,absoluteMaxValue);
    }*/

    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    /**
     * Should the widget notify the listener callback while the user is still dragging a thumb? Default is false.
     *
     * @param flag
     */
    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    /**
     * Returns the absolute minimum value of the range that has been set at construction time.
     *
     * @return The absolute minimum value of the range.

    public T getAbsoluteMinValue() {
        return absoluteMinValue;
    }

    /**
     * Returns the absolute maximum value of the range that has been set at construction time.
     *
     * @return The absolute maximum value of the range.

    public T getAbsoluteMaxValue() {
        return absoluteMaxValue;
    }


    /**
     * Returns the currently selected min value.
     *
     * @return The currently selected min value.
     */
    /*
    public T getSelectedMinValue() {
        return normalizedToValue(normalizedMinValue);
    }*/

    public T getSelectedValue (int index) {
        return normalizedToValue(thumbs.get(index).normalizedValue);
    }

    /**
     * Sets the currently selected value. The widget will be invalidated and redrawn.
     *
     * @param value The Number value to set the minimum value to. Will be clamped to given absolute minimum/maximum range.
     */


    public void setSelectedValue (int index, T value) {
        //TODO: need to assert values
        thumbs.get(index).normalizedValue = valueToNormalized(value);
        Log.d("test","value: "+value.toString()+" normalized: "+thumbs.get(index).normalizedValue);
    }

    public void setLabel (int index, String text) {
        thumbs.get(index).label = text;
    }

    /**
     * Returns the currently selected max value.
     *
     * @return The currently selected max value.
     */

    /*public T getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValue);
    }*/

    /**
     * Sets the currently selected maximum value. The widget will be invalidated and redrawn.
     *
     * @param value The Number value to set the maximum value to. Will be clamped to given absolute minimum/maximum range.
     */
    /*public void setSelectedMaxValue(T value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero when normalizing.
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMaxValue(1d);
        } else {
            setNormalizedMaxValue(valueToNormalized(value));
        }
    }*/

    /**
     * Registers given listener callback to notify about changed selected values.
     *
     * @param listener The listener to notify about changed selected values.
     */
    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener<T> listener) {
        this.listener = listener;
    }

    /**
     * Handles thumb selection and movement. Notifies listener callback on certain events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled()) {
            return false;
        }

        int pointerIndex;

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                pressedThumbIndex = getPressedThumbIndex(mDownMotionX);

                if (pressedThumbIndex == -1) {
                    // no thumb was pressed - is anyone listening?
                    if (listener != null) {
                        int index = getOwningThumbIndex(mDownMotionX);
                        listener.onRangeSeekBarPressed(id, index);
                        return super.onTouchEvent(event);
                    }
                }
                else {
                    // thumb was pressed
                    setPressed(true);
                    invalidate();
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    attemptClaimDrag();
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumbIndex != -1) {

                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);

                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                            setPressed(true);
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }

                    if (notifyWhileDragging && listener != null) {
                        listener.onRangeSeekBarValuesChanged(this,pressedThumbIndex, id,event.getX());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold
                    // should be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                invalidate();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,pressedThumbIndex,id,event.getX());
                    listener.onRangeSeekBarStop();
                }
                pressedThumbIndex = -1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = event.getPointerCount() - 1;
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index);
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private final void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose
            // a new active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private final void trackTouchEvent(MotionEvent event) {
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        final float x = event.getX(pointerIndex);

        Log.d("test","x : "+x);

        if (pressedThumbIndex != -1) {
            setNormalizedValue(pressedThumbIndex, screenToNormalized(x));
        }
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is canceled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    /**
     * Ensures correct size of the widget.
     */
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 200;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }

        int height = thumbImage.getHeight() + 10;
        //+ PixelUtil.dpToPx(getContext(), HEIGHT_IN_DP);
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(width, height);
    }

    /**
     * Draws the widget on the given canvas.
     */
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setStyle(Style.FILL);
        paint.setAntiAlias(true);

        // draw initial bar
        paint.setColor(startColor);
        mRect.left = 0;
        mRect.right = SEEKBAR_WIDTH;
        canvas.drawRect(mRect, paint);

        // draw each timer's bar and thumbnail
        for (int i=0; i<thumbs.size(); i++) {
            mRect.left = normalizedToScreen(thumbs.get(i).normalizedValue);
            mRect.right = (i + 1 == thumbs.size()) ? SEEKBAR_WIDTH : normalizedToScreen(thumbs.get(i + 1).normalizedValue);
            paint.setColor(thumbs.get(i).color);
            canvas.drawRect(mRect, paint);
            mRect.left = normalizedToScreen(thumbs.get(i).normalizedValue) - thumbHalfWidth;
            mRect.right = Math.min(mRect.left + thumbWidth, SEEKBAR_WIDTH);
            Rect srcRect = new Rect(0, 0, (int) (mRect.right - mRect.left), (int) (thumbWidth));
            drawThumb(i, normalizedToScreen(thumbs.get(i).normalizedValue), srcRect, mRect, canvas);
        }
        for (int i=0; i<thumbs.size(); i++) {
            paint.setColor(Color.WHITE);
            paint.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics()));
            Paint.FontMetrics metric = paint.getFontMetrics();
            int textHeight = (int) Math.ceil(metric.descent - metric.ascent);
            int y = (int)(textHeight - metric.descent + metric.bottom);
            canvas.drawText(thumbs.get(i).label, normalizedToScreen(thumbs.get(i).normalizedValue), y, paint);
        }
    }


    //TODO: need to fix these
    /**
     * Overridden to save instance state when device orientation changes. This method is called automatically if you assign an id to the RangeSeekBar widget using the {@link #setId(int)} method. Other members of this class than the normalized min and max values don't need to be saved.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", thumbs.get(0).normalizedValue);
        bundle.putDouble("MAX", thumbs.get(1).normalizedValue);
        return bundle;
    }

    /**
     * Overridden to restore instance state when device orientation changes. This method is called automatically if you assign an id to the RangeSeekBar widget using the {@link #setId(int)} method.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        thumbs.get(0).normalizedValue = bundle.getDouble("MIN");
        thumbs.get(1).normalizedValue = bundle.getDouble("MAX");
    }

    /**
     * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
     *
     * @param screenCoord The x-coordinate in screen space where to draw the image.
     * @param canvas      The canvas to draw upon.
     */


    private void drawThumb(int index, float screenCoord, Rect srcRect, RectF rect, Canvas canvas) {
        Log.d("screen", "draw thumb at: " + screenCoord);
        Bitmap buttonToDraw = thumbs.get(index).bmp;
        canvas.drawBitmap(buttonToDraw, srcRect, rect, paint);
    }

    /**
     * Decides which (if any) thumb is touched by the given x-coordinate.
     *
     * @param touchX The x-coordinate of a touch event in screen space.
     * @return The pressed thumb index or -1 if none has been touched.
     */

    private int getPressedThumbIndex (float touchX) {
        int result = -1;
        for (int i=0; i<thumbs.size(); i++) {
            if (isInThumbRange (touchX,thumbs.get(i).normalizedValue)) {
                result = i;
                break;
            }
        }
        Log.d("test", "pressed " + result);
        return result;
    }

    private int getOwningThumbIndex (float touchX) {
        int result = -1;
        for (int i=thumbs.size()-1; i>=0; i--) {
            if (touchX > normalizedToScreen(thumbs.get(i).normalizedValue)+thumbWidth) {
                result = i;
                break;
            }
        }
        return result;
    }

    /**
     * Decides if given x-coordinate in screen space needs to be interpreted as "within" the normalized thumb x-coordinate.
     *
     * @param touchX               The x-coordinate in screen space to check.
     * @param normalizedThumbValue The normalized x-coordinate of the thumb to check.
     * @return true if x-coordinate is in thumb range, false otherwise.
     */
    private boolean isInThumbRange(float touchX, double normalizedThumbValue) {
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth;
    }

    /**
     * Sets normalized value to value so that 0 <= value <= normalized max value <= 1.
     * The View will get invalidated when calling this method.
     *
     * @param value The new normalized value to set.
     */
    private void setNormalizedValue (int index, double value) {
        ThumbNail thumb = thumbs.get(index);
        Integer hourSlot = new Integer(4);

        double leftBound;
        double rightBound;

        if (index==0) {
            leftBound = 0d;
            if (index+1 < thumbs.size())
                rightBound = thumbs.get(index+1).normalizedValue-valueToNormalized((T) hourSlot);
            else
                rightBound = 1d;
        }
        else if (index==thumbs.size()-1) {
            leftBound = thumbs.get(index-1).normalizedValue+valueToNormalized((T) hourSlot);
            rightBound = 1d;
        }
        else {
            leftBound = thumbs.get(index-1).normalizedValue+valueToNormalized((T) hourSlot);
            rightBound = thumbs.get(index+1).normalizedValue-valueToNormalized((T) hourSlot);
        }

        thumb.normalizedValue = Math.min(value,rightBound);
        thumb.normalizedValue = Math.max(leftBound,thumb.normalizedValue);

        invalidate();
    }

    /**
     * Converts a normalized value to a Number object in the value space between absolute minimum and maximum.
     *
     * @param normalized
     * @return
     */
    @SuppressWarnings("unchecked")
    private T normalizedToValue(double normalized) {
        double v = absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim);
        // TODO parameterize this rounding to allow variable decimal points
        return (T) numberType.toNumber(Math.round(v * 100) / 100d);
    }

    /**
     * Converts the given Number value to a normalized double.
     *
     * @param value The Number value to normalize.
     * @return The normalized double.
     */
    private double valueToNormalized(T value) {
        if (0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            // prevent division by zero, simply return 0.
            return 0d;
        }
        return (value.doubleValue() - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    /**
     * Converts a normalized value into screen space.
     *
     * @param normalizedCoord The normalized value to convert.
     * @return The converted value in screen space.
     */
    private float normalizedToScreen(double normalizedCoord) {
        return (float) (normalizedCoord * SEEKBAR_WIDTH);
    }

    /**
     * Converts screen space x-coordinates into normalized values.
     *
     * @param screenCoord The x-coordinate in screen space to convert.
     * @return The normalized value.
     */
    private double screenToNormalized(float screenCoord) {
        int width = SEEKBAR_WIDTH;

        double result = screenCoord / width;
        result = Math.min(1d, Math.max(0d, result));
        Log.d("test","screen to normal : "+result);
        return result;
    }

    /**
     * Callback listener interface to notify about changed range values.
     *
     * @param <T> The Number type the RangeSeekBar has been declared with.
     * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
     */
    public interface OnRangeSeekBarChangeListener<T> {

        public void onRangeSeekBarValuesChanged(MultiSeekBar<?> bar, int index, int id, float x);
        public void onRangeSeekBarStop();
        public void onRangeSeekBarPressed (int id, int index);
    }


    /**
     * Utility enumeration used to convert between Numbers and doubles.
     *
     * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
     */
    private static enum NumberType {
        LONG, DOUBLE, INTEGER, FLOAT, SHORT, BYTE, BIG_DECIMAL;

        public static <E extends Number> NumberType fromNumber(E value) throws IllegalArgumentException {
            if (value instanceof Long) {
                return LONG;
            }
            if (value instanceof Double) {
                return DOUBLE;
            }
            if (value instanceof Integer) {
                return INTEGER;
            }
            if (value instanceof Float) {
                return FLOAT;
            }
            if (value instanceof Short) {
                return SHORT;
            }
            if (value instanceof Byte) {
                return BYTE;
            }
            if (value instanceof BigDecimal) {
                return BIG_DECIMAL;
            }
            throw new IllegalArgumentException("Number class '" + value.getClass().getName() + "' is not supported");
        }

        public Number toNumber(double value) {
            switch (this) {
                case LONG:
                    return Long.valueOf((long) value);
                case DOUBLE:
                    return value;
                case INTEGER:
                    return Integer.valueOf((int) value);
                case FLOAT:
                    return Float.valueOf((float)value);
                case SHORT:
                    return Short.valueOf((short) value);
                case BYTE:
                    return Byte.valueOf((byte) value);
                case BIG_DECIMAL:
                    return BigDecimal.valueOf(value);
            }
            throw new InstantiationError("can't convert " + this + " to a Number object");
        }
    }

    private Bitmap createThumb () {
        Bitmap leftThumb = changeImageColor(BitmapFactory.decodeResource(getResources(), R.drawable.white_halfthumb), GREEN_TEMP);
        Bitmap rightThumb = flip(leftThumb, Direction.HORIZONTAL);
        Bitmap hourGlass = overlay(leftThumb, rightThumb);
        Bitmap thumbNail = overlay(changeImageColor(BitmapFactory.decodeResource(getResources(), R.drawable.whitethumb),Color.GREEN),hourGlass);
        return getResizedBitmap(thumbNail,40,40);
    }

    public enum Direction { VERTICAL, HORIZONTAL };

    /**
     Creates a new bitmap by flipping the specified bitmap
     vertically or horizontally.
     @param src        Bitmap to flip
     @param type       Flip direction (horizontal or vertical)
     @return           New bitmap created by flipping the given one
     vertically or horizontally as specified by
     the <code>type</code> parameter or
     the original bitmap if an unknown type
     is specified.

     Code from StackOverflow 11609695
     **/
    public static Bitmap flip(Bitmap src, Direction type) {
        Matrix matrix = new Matrix();

        if(type == Direction.VERTICAL) {
            matrix.preScale(1.0f, -1.0f);
        }
        else if(type == Direction.HORIZONTAL) {
            matrix.preScale(-1.0f, 1.0f);
        } else {
            return src;
        }


        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    // code from StackOverflow 10616777
    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {

        int width = bm.getWidth();

        int height = bm.getHeight();

        float scaleWidth = ((float) newWidth) / width;

        float scaleHeight = ((float) newHeight) / height;

// CREATE A MATRIX FOR THE MANIPULATION

        Matrix matrix = new Matrix();

// RESIZE THE BIT MAP

        matrix.postScale(scaleWidth, scaleHeight);

// RECREATE THE NEW BITMAP

        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);

        return resizedBitmap;

    }

    public static Bitmap changeImageColor(Bitmap sourceBitmap, int color) {
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                sourceBitmap.getWidth() - 1, sourceBitmap.getHeight() - 1);
        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(color, 1);
        p.setColorFilter(filter);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, p);
        return resultBitmap;
    }

    public static int getSeekbarWidth () {
        return SEEKBAR_WIDTH;
    }
}
