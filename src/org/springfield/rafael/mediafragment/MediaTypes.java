/* 
* MediaTypes.java
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
package org.springfield.rafael.mediafragment;

import java.util.HashMap;
import java.util.Map;

import org.restlet.data.MediaType;

/**
 * MediaTypes.java
 *
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2017
 * @package org.springfield.rafael.mediafragment
 * 
 */
public final class MediaTypes {

    private String name;
    private MediaType restletMediaType;
    
    private static Map<String, MediaTypes> types = null;
    
    public static final MediaTypes AUDIO = init("audio", MediaType.AUDIO_ALL);

    public static final MediaTypes VIDEO = init("video", MediaType.VIDEO_MP4);
    
    public MediaTypes(String name, MediaType mediatype) {
	this.name = name;
	this.restletMediaType = mediatype;
    }
    
    public static MediaTypes init(String name, MediaType mediatype) {
	if (types == null) {
            types = new HashMap<String, MediaTypes>();
        }
	
	final MediaTypes type = new MediaTypes(name, mediatype);
	
	types.put(name, type);
	
	return types.get(name);
    }
    
    public MediaType getType() {
	return restletMediaType;
    }
}
