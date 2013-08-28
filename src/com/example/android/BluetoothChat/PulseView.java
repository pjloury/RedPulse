package com.example.android.BluetoothChat;

//Used with DaQThread
import java.util.Random;

import com.example.android.BluetoothChat.BluetoothChat.Outer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;


public class PulseView extends View{
		//private CogBuffer cogBuffer;
	
	
		public Canvas canvas;
		private int pCounter;	

		private double[] newData;
		
	  private int[][] dispDataBuffer; //main display circular sample buffer
	  private double[][] dispDataBuffer1; //main display circular sample buffer
    private int dispDataBufferIndex; //current index in circular buffer
    final static private int dispDataBufferDepth = 20000; //maximum depth of display buffer
    private int dispNewSamplesNotDrawn; //number of new samples in buffer that has never been displayed yet
    private int dispXLoc; //current position of the display cursor
    private int dispXDim;
    private int dispYDim;
    private int XSCALE = 1;
//    private int YSCALE = 5000;
    private int YSCALE = 10000;
    
    private double temp[] = new double[Outer.cogBuffer.getCHS()]; 
    private int chCur = 0;
		
		private double[] filDataOut;
//		private GUIThread gui;
		private DAQThread daq;
		
		private int degrees = 0;
		private double radians = 0;
		
		private long startDraw;
		private long endDraw;
		
		private Thread t;
		private double drawTime;
		private double drawTimeAverage = 0;

		private int drawCounter;
		
		private int daqCounter;
		private double daqAverageTime;
		private long startDaq;
		private long endDaq;
		
		private Paint linePaint;
		private Paint cursorPaint;
		private Paint cursorPaint1;		
		
		private LinearLayout lnr;
		private Button xButton;
				
		public PulseView(Context context){
			super(context);

			newData = new double [Outer.cogBuffer.getCHS()];		
			dispDataBuffer1 = new double[dispDataBufferDepth][Outer.cogBuffer.getCHS()];

/////////////////
			//daq = new DAQThread();
			//daq.start();
			
			 //lnr = (LinearLayout) findViewById(R.id.lulz);
			 //xButton = new Button (context);
			//this
			
			init();
			//init(this.canvas);
		  //lnr.addView(xButton);
			
		}
				
		public void init(){
			//Canvas canvasPARAM
			dispXDim = 400;			
			readData();
      linePaint = new Paint();
			cursorPaint = new Paint();
			cursorPaint.setColor(Color.BLUE);
			cursorPaint.setStrokeWidth(1);
			
			cursorPaint1 = new Paint();
			cursorPaint1.setColor(Color.RED);
			cursorPaint1.setStrokeWidth(1);
					
			 //used in DAQThread
			
//			dispDataBuffer = new int[dispXDim][getCHS()]; //
//			int min = 5;
//			int max = 20;
//			for (int i = 0; i < dispXDim; i++){	
//				for(int j = 0; j<getCHS();j++){
//					dispDataBuffer[i][j] = (int)(min + Math.random()*max);
//				}
//			}

		}
			
		
		private class DAQThread extends Thread implements Runnable{
			public void run(){
				while(!Thread.interrupted()){
					startDaq = System.nanoTime();
					radians = degrees *(Math.PI/180);
					
					makeData(radians);
					degrees = degrees+1;
					if( degrees>180) degrees = 0;					
					try {
	          Thread.sleep(50);
	        } catch (Exception e) {
	        }
					endDaq = System.nanoTime();
					double daqTime = (double)(endDaq - startDaq)/1000000000;
					daqCounter++;
					daqAverageTime = (daqTime + daqAverageTime)/daqCounter;
					
					if (daqCounter>50){
						daqCounter = 0;
/////						//Log.d("DAQ_AVG"," "+ daqAverageTime);
					}
					
				}
			}		
		}

		public void makeData(double x){			
			//System.out.println(generateData(x)[3]);
			Outer.cogBuffer.writeSamples(generateData(x), (Integer) 1, (Integer) 1);			//were null
		}
		
		public double[] generateData(double x){	
			double min = 100000000;
		  double max = 500000000;
			for(int i=0; i<Outer.cogBuffer.getCHS(); i++){
			  int amplitude = (int)(min + Math.random()*max);
			  newData[i] = amplitude*Math.sin(x);
				
			}
//			Log.d("DATA_WRITE"," "+newData[0]);
			return newData;
			//
		}		

			
		
		public void readData(){		
			filDataOut = new double[Outer.cogBuffer.getCHS()];
			
			int newRdy = Outer.cogBuffer.samplesReady();	
			
			Log.d("Samples Ready"," "+newRdy);
										
			for(int nS = 0; nS< newRdy; nS++) {       
				Outer.cogBuffer.readSamples((double[]) null, filDataOut, (double[])null, (int[]) null, (int[]) null);
//				Log.d("DATA_READ"," "+filDataOut[0]);

				addDispData(filDataOut); //THIS IS ALL ZEROS				
				
				//USE THIS WITH DAQ THREAD
				//int min = 500;
				//int max = 10000;				
				//for(int j = 0; j<getCHS();j++){				
					//temp[j] = (double)(min + Math.random()*max*Math.sin(nS));			
				//}				
				//addDispData(temp);// THIS WORKS			
				
			}        
  	}
		
		 //method to add data elements to display buffer
    private void addDispData(double[] newData1) {
        System.arraycopy(newData1, 0, dispDataBuffer1[dispDataBufferIndex], 0, Outer.cogBuffer.getCHS());
        //copies from newData, starting at 0, transfers it to dispData (with a specific index, starting
        //at 0, cogDaq.getChs = 64
        dispDataBufferIndex++;
        dispNewSamplesNotDrawn++;
        //Log.d("Samples Drawn"," "+dispNewSamplesNotDrawn);
        if (dispDataBufferIndex >= dispDataBufferDepth) {
            dispDataBufferIndex = 0;
        }
    }

    //method to grab past data element from display buffer
    private double grabDispData(int pastSample, int ch) {
        int index;
        if (dispDataBufferIndex - pastSample >= 0) {
            index = dispDataBufferIndex - pastSample;
        } else {
            index = dispDataBufferDepth - (pastSample - dispDataBufferIndex);
        }
        return dispDataBuffer1[index][ch];
    }
		

		
		@Override
		public void onDraw(Canvas canvas) {
			
			readData();
						
			//startDraw = System.nanoTime();
			// TODO Auto-generated method stub
			super.onDraw(canvas);			
			
      dispYDim = canvas.getHeight();
      dispXDim = canvas.getWidth();   
			
      //Log.d("canvas width","debug");
			 
 
	//Commented Out May 24th	
/*			
			double spacing = canvas.getHeight()/getCHS();//old
			int grabIndex;//old
			double yOld = 0;//old
*/

			
			//dispXLoc = 0; //It works when dispXLoc is zero
			
			////			
			
		  
			
      while (dispNewSamplesNotDrawn > XSCALE) { // also works!
//      	Log.d("Samples Drawn"," "+dispNewSamplesNotDrawn);
      	dispXLoc++;
        if (dispXLoc >= dispXDim) {
            dispXLoc = 0;
        }
        dispNewSamplesNotDrawn-=XSCALE;
      }			
		

      //pixels behind cursor			
      for (int xPos = dispXLoc; xPos > 0; xPos--) {
        //from the new width downward
        chCur = 0;
        
        for (int ch = 0; ch < Outer.cogBuffer.getCHS(); ch++) {
            double point1 = 0;
            double point2 = 0;
            
            //compute offset for each channel relative to the display height and total number of channels
            double offset = (1.0 + (double) chCur) / (1.0 + (double) Outer.cogBuffer.getCHS()) * (double) dispYDim;
            	//this getCHS() used to be chTotal
            
            //average XSCALE previous cahnnels
            for(int c=0; c<XSCALE; c++)
            {
                point1 += grabDispData((dispXLoc - xPos)*XSCALE+c+dispNewSamplesNotDrawn, ch);
                point2 += grabDispData((dispXLoc - (xPos - 1))*XSCALE+c+dispNewSamplesNotDrawn, ch); 
            }
             point1 = YSCALE * point1 / ((double) XSCALE) + offset;
             point2 = YSCALE * point2 / ((double) XSCALE) + offset;
            
             canvas.drawLine(xPos, (int) point1, xPos - 1, (int) point2,cursorPaint); //need to change
             chCur++;      
        }
    }
		
    //paint pixels in front of cursor
      for (int xPos = dispXDim; xPos > dispXLoc ; xPos--) {
          chCur = 0;        
          for (int ch = 0; ch < Outer.cogBuffer.getCHS(); ch++) {
              double point1 = 0;
              double point2 = 0;
              
              //compute offset for each channel relative to the display height and total number of channels
              double offset = (1.0 + (double) chCur) / (1.0 + (double) Outer.cogBuffer.getCHS()) * (double) dispYDim;
                          
              //average XSCALE previous channels
              for(int c=0; c<XSCALE; c++)
              { //was + (dispXDim - xPos))
                point1 += grabDispData((dispXLoc + (dispXDim - xPos))*XSCALE+c+dispNewSamplesNotDrawn, ch);
                point2 += grabDispData((dispXLoc + (dispXDim - xPos + 1))*XSCALE+c+dispNewSamplesNotDrawn, ch);  
              }
              point1 = YSCALE * point1 / ((double) XSCALE) + offset;
              point2 = YSCALE * point2 / ((double) XSCALE) + offset;
                  
              canvas.drawLine(xPos, (int) point1, xPos - 1, (int) point2,cursorPaint1);
              chCur++;
          }
      }



      
      //draw cursor
          
      canvas.drawLine(dispXLoc, 0, dispXLoc, dispYDim,linePaint);
      canvas.drawLine(dispXLoc+1, 0, dispXLoc+1, dispYDim,linePaint);
      canvas.drawLine(dispXLoc-1, 0, dispXLoc-1, dispYDim,linePaint);
      canvas.drawLine(dispXLoc+2, 0, dispXLoc+2, dispYDim,linePaint);
      canvas.drawLine(dispXLoc-2, 0, dispXLoc-2, dispYDim,linePaint);     
      
//			for(int thisCh = 0; thisCh < getCHS(); thisCh++){ //go through the channels
//				double yBaseline = thisCh*spacing;
//				grabIndex = 0;
//				for (int x = 0; x<canvas.getWidth(); x=x+5){				  
//				  //there is no yOld initially
//				  if(x==0){
//				  	yOld =  Math.round((yBaseline + Math.sin(x+5)));				 
//				  	//yOld =  Math.round((yBaseline + grabDispData(grabIndex,thisCh)));				  	
//				  	canvas.drawLine(0, (int)yBaseline, 5, (float) yOld, cursorPaint);
//				  	grabIndex++;
//				  }
//				  else{ 					
//				  	//Log.d("The Data"," "+grabDispData(grabIndex,thisCh));
//				  	canvas.drawLine(x, (int)yOld, x+5, (float)(yOld = Math.round(yBaseline + 20*Math.random()*Math.sin(x+5))),cursorPaint);				  		
//				  	//canvas.drawLine(x, (int)yOld, x+5, (float) (yOld =  Math.round(yBaseline + grabDispData(grabIndex,thisCh))),cursorPaint );	
//				  	grabIndex++;
//				  }
//				}					
//			}
      
			
      //init(this.canvas);
      endDraw = System.nanoTime();
      
      drawTime = (double) (endDraw - startDraw)/(1000000000);
      drawCounter++;
      
      drawTimeAverage = (drawTime+drawTimeAverage)/drawCounter; 
      
      if(drawCounter >=5){
      	drawCounter = 0;
      	
//      	//Log.d("DRAW_AVG"," "+drawTimeAverage);
      }
      
			invalidate();
		}    
}
