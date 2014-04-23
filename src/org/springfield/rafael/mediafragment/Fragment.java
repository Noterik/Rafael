/* 
* Fragment.java
* 
* Copyright (c) 2013 Noterik B.V.
* 
* This file is part of Rafael, related to the Noterik Springfield project.
*
* Rafael is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Rafael is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Rafael.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.springfield.rafael.mediafragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.restlet.data.Status;
import org.springfield.rafael.mediafragment.Fragment;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

/**
 * Media fragment representation
 * 
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2013
 * @package org.springfield.rafael.mediafragment
 * 
 */

public class Fragment {
	private double start;
	private double end;
	private double duration;
	private double totalVideoDuration;
	
	public String uri;
	private String filename;
	private String basePath;
	
	private Status status;

	private static final String NPT = "npt";
	private static final Logger LOG = Logger.getLogger(Fragment.class);
	
	/**
	 * Construct media fragment
	 * 
	 * @param t - temporal dimension
	 * @param basePath - base path where the videos are located, configured in config.xml
	 * @param tempPath - temporary path to store fragments for caching. configured in config.xml
	 * @param video - the video file name
	 */
	public Fragment(String t, String basePath, String tempPath, String video) {
		this.basePath = basePath;
		
		totalVideoDuration = getVideoDuration(video);
		
		//check for optional ntp: prefix, no others allowed in 1.0 basic
		if (t.startsWith(NPT+":")) {
			t = t.substring(NPT.length()+1);
		}
		
		//try to split query value for start and endtime
		if (t.contains(",")) {
			int commaPosition = t.indexOf(",");
			if (commaPosition == 0) {
				start = 0.0;
				end = parseNTP(t.substring(commaPosition+1));
			} else if (t.length()>commaPosition+1) {						
				start = parseNTP(t.substring(0, commaPosition));
				end = parseNTP(t.substring(commaPosition+1));
			} else {
				// nothing set after comma
				status = Status.CLIENT_ERROR_BAD_REQUEST;
				return;
			}
		} else {
			//only starttime was set, endtime is set to the end of the video
			start = parseNTP(t);
			end = totalVideoDuration;	
		}

		//check for error in parsing NPT
		if (status != null) {
			return;
		}
		
		//check if endtime is greater then the starttime
		if (start >= end) {
			status = Status.CLIENT_ERROR_BAD_REQUEST;
			return;
		}

		//check if starttime is smaller then duration of the video
		if (start > totalVideoDuration) {
			status = Status.CLIENT_ERROR_BAD_REQUEST;
			return;
		}

		//endtime cannot be greater then duration of the video
		if (end > totalVideoDuration) {
			end = totalVideoDuration;
		}
		
		duration = end - start;
		
		//create temporary fragment
		filename = tempPath+video.substring(1).replace("/", "_")+"_t="+start+","+end;
		File resource = new File(filename);

		//don't generate if the fragment already exists in cache, instead we will use that one
		if (!resource.exists()) {
			generateFragment(video);
		}
		
		status = status == null ? Status.SUCCESS_OK : status;		
	}
	
	/**
	 * Get the start of the fragment
	 * 
	 * @return start of the fragment
	 */
	public double getStart() {
		return start;
	}
	
	/**
	 * Get the end of the fragment
	 * 
	 * @return end of the fragment
	 */
	public double getEnd() {
		return end;
	}
	
	/**
	 * Get the duration of the fragment
	 * 
	 * @return duration of the fragment
	 */
	public double getDuration() {
		return duration;
	}
	
	/**
	 * Get the status
	 * 
	 * @return status
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Get the video filename
	 * 
	 * @return filename
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Parse NTP time to seconds
	 * 
	 * @param time - input NTP time
	 * @return number of seconds from the NTP time
	 */
	private Double parseNTP(String time) {		
		//test if we have to deal with for seconds format
		Pattern p = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");
		Matcher m = p.matcher(time);
		if (m.find()) {
			return Double.parseDouble(time);
		}
		
		//no second format, must be colon separated hours, minutes, seconds
		String[] timeparts = time.split(":");
		
		if (timeparts.length < 2 || timeparts.length > 3) {
			status = Status.CLIENT_ERROR_BAD_REQUEST;
			return 0.0;
		}
		
		String hours = timeparts.length == 3 ? timeparts[0] : "0";
		String minutes = timeparts.length == 3 ? timeparts[1] : timeparts[0];
		String seconds = timeparts.length == 3 ? timeparts[2] : timeparts[1];
		
		//check if it was supplied in a correct format
		if (p.matcher(hours).find() && p.matcher(minutes).find() && p.matcher(seconds).find()) {		
			return Double.parseDouble(hours)*3600+Double.parseDouble(minutes)*60+Double.parseDouble(seconds);
		}

		status = Status.CLIENT_ERROR_BAD_REQUEST;		
		return 0.0;
	}
	
	/**
	 * Get the duration of the video
	 * 
	 * @param video - file to get the duration from
	 * @return video duration in seconds
	 */
	private Double getVideoDuration(String video) {
		double duration = 0.0;
		
		try {
			IsoFile isoFile = new IsoFile(basePath+video);
			duration = (double)	isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
			isoFile.close();
		} catch (IOException e) { 
			status = Status.SERVER_ERROR_INTERNAL;
		}	
		return duration;
	}
	
	/**
	 * Generate the media fragment from the video 
	 * 
	 * @param video - video to generate the fragment from
	 */
	private void generateFragment(String video) {
		Movie movie;
		double movieStart = 0.0;
		double movieEnd = 0.0;
		
		try {
			movie = MovieCreator.build(new FileInputStream(new File(basePath+video)).getChannel());	
		} catch (IOException e) {
			status = Status.SERVER_ERROR_INTERNAL;
			return;
		} 
		
		List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
		
        boolean timeCorrected = false;
    	
        // Here we try to find a track that has sync samples. Since we can only start decoding
        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
        // such a frame
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    // This exception here could be a false positive in case we have multiple tracks
                    // with sync samples at exactly the same positions. E.g. a single movie containing
                    // multiple qualities of the same video (Microsoft Smooth Streaming file)
                	status = Status.SERVER_ERROR_INTERNAL;
                	return;
                }
                movieStart = correctTimeToSyncSample(track, start, false);
                movieEnd = correctTimeToSyncSample(track, end, true);
                timeCorrected = true;
            }
        }

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            long startSample = -1;
            long endSample = -1;

            for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
                for (int j = 0; j < entry.getCount(); j++) {
                    // entry.getDelta() is the amount of time the current sample covers.

                    if (currentTime <= movieStart) {
                        // current sample is still before the new starttime
                        startSample = currentSample;
                    }
                    if (currentTime <= movieEnd) {
                        // current sample is after the new start time and still before the new endtime
                        endSample = currentSample;
                    } else {
                        // current sample is after the end of the cropped video
                        break;
                    }
                    currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                    currentSample++;
                }
            }
            movie.addTrack(new CroppedTrack(track, startSample, endSample));
        }

        IsoFile out = new DefaultMp4Builder().build(movie);
        FileOutputStream fos = null;

        try {
        	fos = new FileOutputStream(new File(filename));
        } catch (FileNotFoundException e) {
        	status = Status.SERVER_ERROR_INTERNAL;
        	return;
        }
        if (fos != null) {
        	try {
        		out.getBox(fos.getChannel());
        		fos.close();
        	} catch (IOException e) { 
        		status = Status.SERVER_ERROR_INTERNAL;
        		return;
        	}
        }  
        LOG.debug("successfully generated "+filename);
	}
	
	/**
	 * Correct time with sync sample
	 * 
	 * @param track
	 * @param cutHere
	 * @param next
	 * @return
	 */
	private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
            for (int j = 0; j < entry.getCount(); j++) {
                if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                    // samples always start with 1 but we start with zero therefore +1
                    timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
                }
                currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
}
