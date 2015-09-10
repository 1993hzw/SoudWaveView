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
	
	private  int FRAME_TIME=80;//һ֡������ͣ��ʱ�䣬ms
	private  int SUM_WAVES=50;//�ܲ�����
	private  int MIN_SPEED=3;//���Ʊ仯����С�ٶ�
	private  int MAX_SPEED=7;//���Ʊ仯������ٶ�
	private  int SPACE=5;//���Ƽ��
	
	private boolean isStop;//����������־
	private ExecutorService executor=Executors.newFixedThreadPool(1);
	private Handler mHandler=new Handler();
	private Paint mPaint;
	private float waveWidth;//���ƵĿ��
	private int waveHeight=-1;//���Ƶ����߶�
	private int width,height;//view�ߴ�
	private int centerX,centerY;//view���ĵ�
	private Random random=new Random();
	private boolean startUpNow=false;//�Ƿ�������������
	
	private int rotate=0;//��ת�Ƕȣ���λ�ڵײ�ʱrotate=0�����ʱrotate=90������ʱrotate=180���ұ�ʱrotate=270��
	
	private Runnable invalidateRunnable=new Runnable() {//ˢ��
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
			public void onGlobalLayout() {//��ȡview�Ŀ�ߣ���������ƿ��
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
                width=getMeasuredWidth();
                height=getMeasuredHeight();
                if(rotate==90||rotate==270){
                	 waveWidth=(height-(SUM_WAVES-1f)*SPACE)/SUM_WAVES;
                	 if(waveHeight<=0||waveHeight>waveHeight)//�߶Ȳ��ܳ���view
                		 waveHeight=width;//Ĭ�ϸ߶�
                }else{
                	 waveWidth=(width-(SUM_WAVES-1f)*SPACE)/SUM_WAVES;
                	 if(waveHeight<=0||waveHeight>height)//�߶Ȳ��ܳ���view
                		 waveHeight=height*2/3;//Ĭ�ϸ߶�
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
		canvas.rotate(rotate,centerX,centerY);//xy��Ҳ������ת
		if(rotate==90||rotate==270){
			canvas.translate((width-height)/2,(width-height)/2);
		}		
		for(Wave w:waves)
			w.drawItself(canvas);
		canvas.restore();
	}
	
	private class Wave{
		private RectF rect;
		private int speed;//�仯�ٶ�
		private int bottom;//�ײ�
		private int top;//����
		public Wave(int id) {
			//�����ʼ��
			top=height-waveHeight+random.nextInt(waveHeight);
			bottom=height-random.nextInt(Math.abs(top)/2+1);
			speed=random.nextInt(MAX_SPEED-MIN_SPEED+1)+MIN_SPEED;
			
			rect=new RectF(id*waveWidth+id*SPACE, top, id*waveWidth+id*SPACE+waveWidth, height);
		}
		public void change(){
			rect.top=rect.top+speed;
			if(rect.top>bottom){//�½����ײ�
				//�����ʼ���ƶ���
				top=height-waveHeight+random.nextInt(waveHeight);
				//�����ʼ�ٶ�
				speed=-(random.nextInt(MAX_SPEED-MIN_SPEED+1)+MIN_SPEED);
			  }else if(rect.top<top){//����������
				  //�����ʼ���Ƶײ�
				  bottom=height-random.nextInt(Math.abs(top)/2+1);
				  //�����ʼ�ٶ�
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
	
	//��ɫ
	public void setColor(int color){
		mPaint.setColor(color);
	}
	
	//ֹͣ����
	public void stop(){
		isStop=true;
	}
	//��ʼ����
	public void start(){
		if(!isStop) return;
		isStop=false;
		executor.execute(new DrawThread());
	}
	//��ͼ�߳�
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
