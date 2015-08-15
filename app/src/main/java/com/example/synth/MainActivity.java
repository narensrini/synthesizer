package com.example.synth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class MainActivity extends FragmentActivity {

	Thread t;
	int sr = 44100;
	boolean isRunning = true;
	int waveshape = 0;
	int width = 0;
	int height = 0;
	int attslider;
	int relslider;
	SeekBar aSlider, rSlider;
	boolean attack;
	boolean release;
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Display display = getWindowManager().getDefaultDisplay();
		width = display.getWidth();
		rSlider = (SeekBar)findViewById(R.id.release);
		aSlider = (SeekBar)findViewById(R.id.attack);
		rSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser) {
					relslider = progress;
				}
				
			}
		});
		aSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				attslider = progress;
				Log.e("Progress", String.valueOf(progress));
				Log.e("max", String.valueOf(seekBar.getMax()));
				Log.e("sliderval", String.valueOf(attslider));
				
			}
		});
		height = display.getHeight();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		waveshape = Integer.parseInt(prefs.getString("wave", "0"));
	}
	
	private void playSound(final MotionEvent ev) {
		final float xPos = 8*ev.getX()/width;
		final float yPos = ev.getY()/height;
		isRunning = true;
		t = new Thread() {
			public void run() {
				setPriority(Thread.MAX_PRIORITY);
				// set the buffer size
				int buffsize = AudioTrack.getMinBufferSize(sr,
						AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				// create an audiotrack object
				AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						sr, AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, buffsize,
						AudioTrack.MODE_STREAM);
				short samples[] = new short[buffsize];
				float maxamp = (float) (10000*Math.pow(yPos, 2.0));
				float amp = 0;
				double twopi = 8.*Math.atan(1.);
				double fr = 440.f;
				double ph = 0.0;
				if(ev.getAction() == MotionEvent.ACTION_UP) {
					if(relslider!=0)
						release = true;
					else {
						release = false;
						stopSound();
						return;
					}
				}
				else
					release = false;
				if(attslider == 0) {
					attack = false;
					amp = maxamp;
				}
				else {
					attack = true;
				}
				// start audio
				audioTrack.play();
				// synthesis loop
				while(isRunning){
					if(Math.floor(xPos)<3)
						fr =  440*Math.pow(2.0, 2*Math.floor(xPos)/12);
					else if(Math.floor(xPos)>=3 && Math.floor(xPos)<7)
						fr =  440*Math.pow(2.0, (2*Math.floor(xPos) - 1)/12);
					else if(Math.floor(xPos) == 7)
						fr =  440*Math.pow(2.0, (2*Math.floor(xPos) - 2)/12);
					Log.e("Here", String.valueOf(Math.floor(xPos)));
					for(int i=0; i < buffsize; i++){
						if (waveshape == 0) {
							samples[i] = (short)(amp*(Math.sin(twopi*ph) + Math.cos(twopi*ph)));
						}
						else if (waveshape == 1) {
							samples[i] = (short) (amp*2*(ph - Math.floor(ph) - 0.5));
						}
						else if (waveshape == 2) {
							if(Math.floor(ph*2)%2 == 0) {
								samples[i] = (short) (amp);
							}
							else {
								samples[i] = (short) (amp*-1);
							}
						}
						else if (waveshape == 3) {
							samples[i] = (short) ((short) amp*Math.random());
						}
						ph += fr/sr;

					}
					audioTrack.write(samples, 0, buffsize);
					if(attack) {
						if(amp < maxamp)
							amp+=maxamp/(attslider + 1);
						if(amp > maxamp)
							amp = maxamp;
						Log.e("amp", String.valueOf(amp));
					}
					if (release){
						if(amp > 0)
							amp-=maxamp/(relslider + 1);
						if(amp<0) {
							amp = 0;
							isRunning = false;
							return;
						}
					}
					
				}
				
				audioTrack.stop();
				audioTrack.release();
			}
		};
		t.start();
	}
	
	private void stopSound() {
		isRunning = false;
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(t!=null)
			t = null;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		int x = 0;
		int newx;
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			playSound(ev);
			x = (int) Math.floor(8*ev.getX()/width);
			Log.e("x", String.valueOf(x));
			break;
		case MotionEvent.ACTION_UP:
			playSound(ev);
			break;
		case MotionEvent.ACTION_MOVE:
			if(relslider==0) {
				newx = (int) Math.floor(8*ev.getX()/width);
				Log.e("newx", String.valueOf(newx));
				if(x != newx){
					stopSound();
					playSound(ev);
				}
				x = newx;
			}
		}
		return true;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent i = new Intent(getApplicationContext(), UserSettingActivity.class);
			startActivity(i);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
	}
}