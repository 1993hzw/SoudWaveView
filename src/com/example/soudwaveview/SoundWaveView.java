package com.example.soudwaveview;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class SoundWaveView extends View{
	private List<Wave> waves;
	
	private  int FRAME_TIME=80;//一帧动画的停留时间，ms
	private  int SUM_WAVES=50;//总波纹数
	private  int MIN_SPEED=3;//波纹变化的最小速度
	private  int MAX_SPEED=7;//波纹变化的最大速度
	private  int SPACE=5;//波纹间距
	
	private boolean isStop;//动画开启标志
	private ExecutorService executor=Executors.newFixedThreadPool(1);
	private Handler mHandler=new Handler();
	private Paint mPaint;
	private float waveWidth;//波纹的宽度
	private int waveHeight=-1;//波纹的最大高度
	private int width,height;//view尺寸
	private int centerX,centerY;//view中心点
	private Random random=new Random();
	private boolean startUpNow=false;//是否立即开启动画
	
	private int rotate=0;//旋转角度，当位于底部时rotate=0；左边时rotate=90；顶部时rotate=180；右边时rotate=270；
	
	private Runnable invalidateRunnable=new Runnable() {//刷新
			public void run() {
				invalidate();
			}
	}; 
	public SoundWaveView(Context context) {
		super(context,null);
	}
	public SoundWaveView(Context context, AttributeSet attrs,int arg3) {
		super(context,attrs,arg3);
	}
	public SoundWaveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {//获取view的宽高，计算出波纹宽度
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
                width=getMeasuredWidth();
                height=getMeasuredHeight();
                if(rotate==90||rotate==270){
                	 waveWidth=(height-(SUM_WAVES-1f)*SPACE)/SUM_WAVES;
                	 if(waveHeight<=0||waveHeight>waveHeight)//高度不能超出view
                		 waveHeight=width;//默认高度
                }else{
                	 waveWidth=(width-(SUM_WAVES-1f)*SPACE)/SUM_WAVES;
                	 if(waveHeight<=0||waveHeight>height)//高度不能超出view
                		 waveHeight=height*2/3;//默认高度
                }
                centerX=width/2;
                centerY=height/2;
                for(int i=0;i<SUM_WAVES;i++){
        			waves.add(new Wave(i));
        		}
                if(startUpNow)
                   start();
			}
		});
		init(attrs);
	}
	
	private void init(AttributeSet  attrs){
		isStop=true;
		waves=new CopyOnWriteArrayList<SoundWaveView.Wave>();
		mPaint=new Paint();
		mPaint.setStyle(Style.FILL);
		mPaint.setTextSize(24);
		
        if(attrs!=null){
        	TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SoundWaveView);
            mPaint.setColor(a.getColor(R.styleable.SoundWaveView_color, Color.GREEN));
            SUM_WAVES=a.getInt(R.styleable.SoundWaveView_sumWaves, SUM_WAVES);
            MIN_SPEED=a.getInt(R.styleable.SoundWaveView_minSpeed, MIN_SPEED);
            MAX_SPEED=a.getInt(R.styleable.SoundWaveView_maxSpeed, MAX_SPEED);
            SPACE=a.getDimensionPixelSize(R.styleable.SoundWaveView_space, SPACE);
            FRAME_TIME=a.getInt(R.styleable.SoundWaveView_frameTime, FRAME_TIME);
            startUpNow=a.getBoolean(R.styleable.SoundWaveView_startUpNow, startUpNow);
            rotate=a.getInt(R.styleable.SoundWaveView_location, rotate);
            waveHeight=a.getDimensionPixelSize(R.styleable.SoundWaveView_waveHeight, waveHeight);
        }
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(getMeasuredHeight()==0||getMeasuredWidth()==0||waves==null){
			return;
		}
		canvas.save();
		canvas.rotate(rotate,centerX,centerY);//xy轴也跟着旋转
		if(rotate==90||rotate==270){
			canvas.translate((width-height)/2,(width-height)/2);
		}		
		for(Wave w:waves)
			w.drawItself(canvas);
		canvas.restore();
	}
	
	private class Wave{
		private RectF rect;
		private int speed;//变化速度
		private int bottom;//底部
		private int top;//顶部
		public Wave(int id) {
			//随机初始化
			top=height-waveHeight+random.nextInt(waveHeight);
			bottom=height-random.nextInt(Math.abs(top)/2+1);
			speed=random.nextInt(MAX_SPEED-MIN_SPEED+1)+MIN_SPEED;
			
			rect=new RectF(id*waveWidth+id*SPACE, top, id*waveWidth+id*SPACE+waveWidth, height);
		}
		public void change(){
			rect.top=rect.top+speed;
			if(rect.top>bottom){//下降到底部
				//随机初始波纹顶度
				top=height-waveHeight+random.nextInt(waveHeight);
				//随机初始速度
				speed=-(random.nextInt(MAX_SPEED-MIN_SPEED+1)+MIN_SPEED);
			  }else if(rect.top<top){//上升到顶部
				  //随机初始波纹底部
				  bottom=height-random.nextInt(Math.abs(top)/2+1);
				  //随机初始速度
				  speed=random.nextInt(MAX_SPEED-MIN_SPEED+1)+MIN_SPEED;
			  }
		}
		public void drawItself(Canvas canvas){
			canvas.drawRect(rect,mPaint);
		}
	}
	
	public enum ROTATE{
		ROTATE_90,ROTATE_180,ROTATE_270,
	}
	
	//颜色
	public void setColor(int color){
		mPaint.setColor(color);
	}
	
	//停止动画
	public void stop(){
		isStop=true;
	}
	//开始动画
	public void start(){
		if(!isStop) return;
		isStop=false;
		executor.execute(new DrawThread());
	}
	//绘图线程
	 private class DrawThread implements Runnable{	
	    	public void run(){
	    		try {
		    		while(!isStop&&!Thread.interrupted()){
		    			for(Wave w:waves){
		    				w.change();
		    			}
	//	    			 Log.i("my","draw "+frame);
		    			mHandler.post(invalidateRunnable);
		    			Thread.sleep(FRAME_TIME);
		    		}
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    			isStop=true;
				}
	    	}
	    }
}
