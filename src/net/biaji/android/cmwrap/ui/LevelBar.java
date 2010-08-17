package net.biaji.android.cmwrap.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;

public class LevelBar extends View {

	private Paint barslot = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint barProgress = new Paint(Paint.ANTI_ALIAS_FLAG);

	LinearGradient slot, progress;
	/**
	 * 总级别
	 */
	private int totaLevel = 2;

	/**
	 * 当前级别
	 */
	private int level = 0;
	
	/**
	 * 空槽
	 */
	private final int[] baseColors = new int[] { Color.LTGRAY,
			Color.rgb(150, 150, 150), Color.DKGRAY, Color.LTGRAY };

	

	public LevelBar(Context context) {
		super(context);

	}

	public LevelBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private void initUI(){
		slot = new LinearGradient(25, 50, 25, 58, baseColors, null,
				TileMode.CLAMP);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		// canvas.drawColor(Color.WHITE);
		barslot.setShader(slot);
		progress = new LinearGradient(25, 50, getWidth() - 35, 55, new int[] {
				Color.GREEN, Color.rgb(255, 150, 0), Color.RED }, null,
				TileMode.CLAMP);
		barProgress.setAlpha(150);
		barProgress.setShader(progress);
		canvas.drawRoundRect(new RectF(25, 50.0f, getWidth() - 25.0f, 58.0f),
				3.0f, 3.0f, barslot);
		canvas.drawRoundRect(new RectF(25, 50.0f,
				(getWidth() - 25.0f - 25) / 2 + 25, 58.0f), 3.0f, 3.0f,
				barProgress);
		super.onDraw(canvas);
	}
}
