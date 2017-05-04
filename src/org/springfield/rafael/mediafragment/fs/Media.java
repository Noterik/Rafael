/* 
* Media.java
* 
* Copyright (c) 2017 Noterik B.V.
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
package org.springfield.rafael.mediafragment.fs;

import org.springfield.rafael.mediafragment.MediaTypes;

/**
 * Media.java
 *
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2017
 * @package org.springfield.rafael.mediafragment.fs
 * 
 */
public class Media {
    
    MediaTypes type;
    Audio audio;
    Video video;
    
    public Media(String identifier, MediaTypes type) {
	this.type = type;
	
	if (type == MediaTypes.AUDIO) {
	    audio = new Audio(identifier);
	} 
	if (type == MediaTypes.VIDEO) {
	    video = new Video(identifier);
	}
    }
    
    public boolean isPrivate() {
	if (type == MediaTypes.AUDIO) {
	    return audio.isPrivate();
	}
	if (type == MediaTypes.VIDEO) {
	    return video.isPrivate();
	}
	return true;
    }
}
