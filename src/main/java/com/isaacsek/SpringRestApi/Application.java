package com.isaacsek.SpringRestApi;

import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.UByteBufferIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage;
import org.bytedeco.javacv.VideoInputFrameGrabber;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class Application {
	//private final static String FRAMES_FILE_PATH = "static/videos/A4_VideoFrames.avi";
	private final static String FRAMES_FILE_PATH = "static/videos/frames.avi";
	private final static String HISTOGRAM_FILE_PATH = "static/data/histogram.json";
	private final static int TOR = 2;
	
	public static void main(String[] args) throws IOException{
		//int[][] initialHistogram = getHistogramFromFrames(FRAMES_FILE_PATH);
//		int[][] histogram = deserializeJson("static/data/histogram.json");
//		int[] differences = getDifferenceArray(histogram);
//		List<Integer> results = detectShots(differences, TOR);
//		
//		int c = 1;
//		for(int x : results) {
//			System.out.println("Shot " + c + ": " + (x + 1000));
//			c++;
//		}
//		for(int i = 0; i < results.size(); i++) {
//			splitVideoIntoShots(results, i);
//		}
//		convertToMp4(results);
		SpringApplication.run(Application.class, args);
	}
	
	public static void splitVideoIntoShots(List<Integer> shots, int index) throws IOException {
		String videoPath= new ClassPathResource(FRAMES_FILE_PATH).getFile().getPath();   
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
		grabber.start();
		
	    FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("output/shot" + (index + 1) + ".mp4", grabber.getImageWidth(), grabber.getImageHeight(), 2);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setInterleaved(true);
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");
        recorder.setVideoBitrate(2000000);
        recorder.setFrameRate(grabber.getFrameRate()); 
        recorder.start();
        
        int shotStart = shots.get(index);
	    int shotEnd = index == shots.size() - 1 ? 3995 : shots.get(index+ 1) - 1;
	    System.out.println("Shot #" + index + ", start = " + shotStart + ", end = " + shotEnd);
        for(int frame = 0; frame < shotEnd; frame++){
        	Frame f = grabber.grabImage();
			if(frame >= shotStart) {
		        recorder.setTimestamp(grabber.getTimestamp());
	            recorder.record(f);
			}
		}
        
        recorder.stop();
        grabber.stop();
        recorder.close();
        grabber.close();
	}
	
	public static void convertToMp4(List<Integer> shots) throws IOException {
		String videoPath= new ClassPathResource(FRAMES_FILE_PATH).getFile().getPath();   
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
		grabber.start();
		
	    FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("source.mp4", grabber.getImageWidth(), grabber.getImageHeight(), 2);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setInterleaved(true);
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");
        recorder.setVideoBitrate(2000000);
        recorder.setFrameRate(grabber.getFrameRate()); 
        recorder.start();
        
        for(int i = 0; i < grabber.getLengthInFrames(); i++){
        	Frame frame = grabber.grabImage();
	        recorder.setTimestamp(grabber.getTimestamp());
            recorder.record(frame);
		}
        
        recorder.stop();
        grabber.stop();
        recorder.close();
        grabber.close();
	}

	public static List<Integer> detectShots(int[] nums, int tor) {
		double avg = getAvg(nums);
		double std = getStd(nums, avg); //7904.672622;
		
		double cut = avg + (std * 11);
		double transition = avg * 2;
		
		System.out.println("average = " + avg + ", std = " + std + ", cut = " + cut + ", transition = " 
				+ transition + ", tor = " + tor + ", length = " + nums.length);
		
		LinkedList<Integer> results = new LinkedList<Integer>();
		results.add(0);
		int i = 0;
		while(i < nums.length - tor) {
			if(nums[i] < transition) {
				i++;
				continue;
			}
			// difference >= Tb
			if(nums[i] >= cut) {
				System.out.println("cut found at frame: " + (i + 1));
				results.add(i + 1); // Cs + 1 || Ce
				i++;
				continue;
			}
			
			// difference > transition && difference < cut
			else if (nums[i] > transition) {
				int j = i + 1, torCount = 0, sum = nums[i];
				while(j < nums.length) {
					if(nums[j] >= cut) {
						if(sum >= cut) {
							System.out.println("gradual cut found at frame: " + (i + 1) + " to " + (j));
							results.add(i + 1); // Fs + 1
							i = j;
							break;
						}
						break;
					}
					else if(torCount == tor) {
						sum -= nums[j - 1];
						System.out.println("count: " + torCount);
						if(sum >= cut) {
							System.out.println("gradual found at frame: " + (i + 1) + " to " + (j - tor));
							results.add(i + 1); // Fs + 1
							i = j - tor;
							break;
						}
						break;
					}
					torCount = nums[j] < transition ? torCount + 1 : 0;
					sum += nums[j];
					j++;
				}
			}
			i++;
		}
		return results;
	}
	
	public static int[][] deserializeJson(String pathToFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		String hPath= new ClassPathResource(pathToFile).getFile().getPath();  
		return mapper.readValue(new File(hPath), int[][].class);
	}
	
	public static double getAvg(int[] nums) {
		int sum = 0;
		for(int x : nums) 
			sum += x;
		return Math.round((double)sum / nums.length);
	}
	
	public static double getStd(int[] nums, double avg) {
		double squaredSum = 0;
		for(int x : nums)
			squaredSum += ((x - avg) * (x - avg));
		squaredSum = squaredSum / nums.length;
		return Math.round(Math.sqrt(squaredSum));
	}
	
	public static int[] getDifferenceArray(int[][] histogram) {
		int[] differences = new int[histogram.length - 1];
		for(int i = 1; i < differences.length; i++) {
			differences[i] = getManhattanDistance(histogram[i + 1], histogram[i]);
		}
		return differences;
	}
	
	public static int getManhattanDistance(int[] a, int[] b) {
		int sum = 0;
		for(int i = 0; i < a.length; i++) {
			sum += Math.abs(a[i] - b[i]);
		}
		return sum;
	}
	
	public static int[][] getHistogramFromFrames(String filePath) throws IOException {
		String videoPath= new ClassPathResource(filePath).getFile().getPath();   
		FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath);
	    
	    frameGrabber.start();
	    int frames = frameGrabber.getLengthInFrames();
	    int[][] allImageFeatures = new int[frames][25];
	    //System.out.println("length = " + frames + " " + allImageFeatures.length);
	    for(int i = 0; i < frameGrabber.getLengthInFrames(); i++){
	    	//System.out.println(i);
	    	Frame frame = frameGrabber.grabImage();
	    	int[] imageFeatures;
	    	if(frame != null) {
	    		imageFeatures = getRgbFromBuffer2(frame);
	    	}
	    	else {
	    		imageFeatures = new int[25];
	    	}
	    	//imageFeatures = getRgbFromBuffer2(frame);
	    	allImageFeatures[i] = imageFeatures;
	    }
	    frameGrabber.flush();
	    frameGrabber.close();
	    
	    
	    ObjectMapper objectMapper = new ObjectMapper();
	    
	    
	    String fileName = "histogram.json";
	    String fileLocation = new File("").getAbsolutePath() + "\\" + fileName;
	    
	    
	    //String path= new ClassPathResource("static/histogram.json").getFile().getPath();   
	    //System.out.println(fileLocation);
	    objectMapper.writeValue(new File("histogram.json"), allImageFeatures);
	    return allImageFeatures;
	}
	
	public static int[] getRgbFromBuffer(Frame frame) {
		Java2DFrameConverter converter = new Java2DFrameConverter();
		BufferedImage image = converter.convert(frame);
		final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		int[] buckets = new int[25];
		for(int i = 0; i < pixels.length; i += 3) {
			int blue = ((int) pixels[i] & 0xff); // blue
			int green = (((int) pixels[i + 1] & 0xff) << 8); // green
			int red = (((int) pixels[i + 2] & 0xff) << 16); // red
			
			int intensity = getIntesity(red, green, blue);
			int bucket = intensity / 10;

			if (bucket >= 24)  
				buckets[24]++;
            else  
            	buckets[bucket]++;
		}
		return buckets;
	}

	
	public static int[] getRgbFromBuffer2(Frame frame) {
		Java2DFrameConverter converter = new Java2DFrameConverter();
		BufferedImage image = converter.convert(frame);
		int[] buckets = new int[25];
		for(int row = 0; row < image.getHeight(); row++) {
    		for(int col = 0; col < image.getWidth(); col++) {
    			int rgb = image.getRGB(col, row);
    			
    			Color color = new Color(rgb);
    			int intensity = getIntesity(color.getRed(), color.getGreen(), color.getBlue());
    			int bucket = intensity / 10;

    			if (bucket >= 24)  
    				buckets[24]++;
                else  
                	buckets[bucket]++;
    		}
    	} 
		return buckets;
	}
	
	public static int getIntesity(int r, int g, int b) {
		return (int) Math.round((.299 * 4) + (.578 * g) + (.114 * b));
	}
}
