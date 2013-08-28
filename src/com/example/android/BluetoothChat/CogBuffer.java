package com.example.android.BluetoothChat;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Cognionics Buffer Class
 * takes raw samples from CogDAQ, filters, processes and stores in circular buffer
 * provides methods to retrieve data from circular buffer
 * @author mikechi
 */
public class CogBuffer {
    //total buffer length
    final static private int BUFLEN = 5000;
    
    //filter parameters and sample rate
    protected double HPFreq;
    protected double sampleRate;
    
    //device gain settings
    protected double GAIN;
    protected double VREF;
    protected double ISTIM;
    protected double ADC_TO_VOLTS;
    protected double TO_Z;
    
    //number of channels in the device
    protected int cogCHS;
    
    //memory buffers for data
    private double[][] rawData;
    private double[][] filData;
    private double[][] last4Samples;
    private double[][] impData;
            
    private int[] packetCounter;
    private int[] triggerData;
    
    //pointers for circular buffers
    private volatile int readIndex;
    private volatile int writeIndex;
    
    //state variables for HPF
    private double xprev_hp[];
    private double yprev_hp[];
    
    
    public int getCHS(){
    	return cogCHS;
    }
    
    /*
     * Class constructor, sets up the buffers for the specified number of channels
     */
    public CogBuffer(int CHS)
    {
        //init buffer arrays
        rawData = new double[BUFLEN][CHS];     //Buffer Length 5000 by 64 channels
        filData = new double[BUFLEN][CHS];
        last4Samples = new double[4][CHS];
        impData = new double[BUFLEN][CHS];
        
        packetCounter = new int[BUFLEN];
        triggerData = new int[BUFLEN];
        
        //init device parameters
        HPFreq = 1;
        sampleRate = 300;
        cogCHS = CHS;
        
        GAIN = 3.0;
        VREF = 3.0;
        ISTIM = 0.000000024;
        
        ADC_TO_VOLTS = (VREF/(4294967296.0*GAIN));
        TO_Z = 1.4/(ISTIM*2.0);
        
        //init filter state variables
        xprev_hp = new double[CHS];
        yprev_hp = new double[CHS];
    }
    
    /*
     * Query to see how many unread samples are in the buffer
     */
    public int samplesReady()
    {
        if( (writeIndex-readIndex) >= 0)
        {
            return writeIndex-readIndex;
        }
        else
        {
            return (BUFLEN-readIndex+writeIndex);
        }
    }
    
    /*
     * Write new set of samples to buffer, performs basic filtering and calculation of electrode impedance
     * This should be called by the Bluetooth/USB interface when new samples from the device are ready
     * newData is a double array length CHS
     */
    
    
    int[] remap = {46, 45, 44, 52, 51, 35, 48, 47, 43, 50, 49, 62, 37, 36, 33, 34, 42, 63, 64, 61, 60, 25, 40, 39, 38, 53, 59, 58, 57, 8, 21, 22, 23, 24, 54, 9, 10, 11, 12, 17, 18, 19, 20, 55, 13, 14, 15, 16, 29, 30, 31, 32, 56, 1, 2, 3, 4, 26, 27, 28, 41, 5, 6, 7}; 

    public void writeSamples(double[] newData, int pCounter, int trigger)
    {
        
        //reorder if 64 channel headset
        if(cogCHS == 64)
        {
                double[] tempSample = new double[64];
                System.arraycopy(newData, 0, tempSample, 0, cogCHS);
                
                for(int ch=0; ch<cogCHS; ch++)
                {
                    newData[ch] = tempSample[remap[ch]-1];
                }
        }
        
        //write packet counter and trigger
        packetCounter[writeIndex] = pCounter;
        triggerData[writeIndex] = trigger;
        
        for(int c=0; c<cogCHS; c++)
        {
            
            //save the raw data
            rawData[writeIndex][c] = (newData[c]*ADC_TO_VOLTS);
            
            //remove impedance stimuli
            //push old data out and add latest data point to the 4 point buffer
            last4Samples[0][c] = last4Samples[1][c];
            last4Samples[1][c] = last4Samples[2][c];
            last4Samples[2][c] = last4Samples[3][c];
            last4Samples[3][c] = rawData[writeIndex][c];
            
            //save data with impedance stimuli gone
            filData[writeIndex][c] = (last4Samples[3][c]+last4Samples[2][c]+last4Samples[1][c]+last4Samples[0][c])/4.0;
            //compute impedance and save into impedance buffer
            double diff1, diff2;
            diff1 = Math.abs(last4Samples[3][c]-last4Samples[1][c]);
            diff2 = Math.abs(last4Samples[2][c]-last4Samples[0][c]);
            if(diff2>diff1)
                {diff1 = diff2;}
            impData[writeIndex][c] = diff1*TO_Z;
            
            //perform high-pass filtering
            filData[writeIndex][c] = hpf(c,filData[writeIndex][c]);

        }
        writeIndex++;
        if(writeIndex>=BUFLEN)
        {
            writeIndex = 0;
        }
    }
        

    /*
     * Readsout full data set from buffer into double array(s) of length CHS
     * updates read index pointer
     */
    public void readSamples(double[] rawDataOut, double[] filDataOut, double[] impDataOut, int[] triggerDataOut, int[] packetDataOut)
    {
       
        for(int c=0; c<cogCHS; c++)
        {
            if(rawDataOut!=null)
            {rawDataOut[c] = rawData[readIndex][c];}
            
            if(filDataOut!=null)
            {filDataOut[c] = filData[readIndex][c];}
            
            if(impDataOut!=null)
            {impDataOut[c] = impData[readIndex][c];}
        }
        
                    
        if(triggerDataOut!=null)
        {triggerDataOut[0] = triggerData[readIndex];}

        if(packetDataOut!=null)
        {packetDataOut[0] = packetCounter[readIndex];}
        
        readIndex++;
        if(readIndex>=BUFLEN)
        {
            readIndex=0;
        }
        
    }
    
    /*
     * Reads out impedance values, averaged by the specified number of samples
     */
    public void readImp(double[] impDataOut, int samplesToAverage)
    {
        double[] impDataTemp = new double[cogCHS];
        
        int tempIndex = readIndex;
        
        for(int j=0; j<samplesToAverage; j++)
        {
            for(int c=0; c<cogCHS; c++)
            {
                impDataTemp[c] += impData[tempIndex][c]/((double)samplesToAverage);
            }
            
            tempIndex--;
            if(tempIndex<0)
            {
               tempIndex = BUFLEN-1;
            }
        }
        System.arraycopy(impDataTemp, 0, impDataOut, 0, cogCHS);
    }
    
    /*
     * Updates high pass filter with newSample, updates state variables and retunrs
     * filtered data
     */
    private double hpf(int ch, double newSample)
    {
        //calculate coefficents
        double delta_t = 1.0/sampleRate; //sample interval is 1/current smaple rate
        double RC_HP = 1/(6.28*HPFreq);
        double alpha = RC_HP/(RC_HP+delta_t);
        
        //perform HPF
        double yout = alpha * (yprev_hp[ch] + (double) newSample - xprev_hp[ch]);
        
        //update state variables
        xprev_hp[ch] = (double) newSample;
        yprev_hp[ch] = yout;
        
        return yout;
    }
    
    /*
    public static void main(String args[])
    {
        CogBuffer buf = new CogBuffer(4);
        
        System.out.println(buf.samplesReady());
        double[] testd = {1,2,3,4};
        buf.writeSamples(testd);
        buf.writeSamples(testd);
        System.out.println(buf.samplesReady());
        double[] r = new double[4];
        buf.readSamples(r);
        System.out.println(buf.samplesReady());
        System.out.println(r[3]+"  "+r[2]); 
    }
    */
}
