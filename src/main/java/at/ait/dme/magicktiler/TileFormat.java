/*
 * Copyright 2010 Austrian Institute of Technology
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package at.ait.dme.magicktiler;

/**
 * Supported tile file formats. Please note that not all tiling schemes
 * may support all tile file formats!
 * 
 * @author aboutgeo@no5.at
 */
public class TileFormat {
	
	/**
	 * Supported tile formats
	 */
	public static final TileFormat JPEG = new TileFormat();
	public static final TileFormat PNG = new TileFormat("image/png", "png");
	
	/**
	 * The format mime type (default: image/jpeg)
	 */
	private String mimeType = "image/jpeg";
	
	/**
	 * The format extension (default: jpg)
	 */
	private String extension = "jpg";
	
	private TileFormat() { }
	
	private TileFormat(String mimeType, String extension) {
		this.mimeType = mimeType;
		this.extension = extension;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public String getExtension() {
		return extension;
	}

}
