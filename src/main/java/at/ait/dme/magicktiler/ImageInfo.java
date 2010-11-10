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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.process.OutputConsumer;

/**
 * Image information for a specified file. (Currently width and height only.)
 * 
 * @author magicktiler@gmail.com
 */
public class ImageInfo {
	
	/**
	 * Error message
	 */
	private static final String IDENTIFY_ERROR = "Error reading file information";
		
	/**
	 * The source image
	 */
	private File file;
	
	/**
	 * Image width and height
	 */
	private int width;
	private int height;
	
	public ImageInfo(File image, boolean useGraphicsMagick) throws TilingException {
		this.file = image;
		
		final StringBuffer result = new StringBuffer();
		try {
			IdentifyCmd identify = new IdentifyCmd(useGraphicsMagick);
			identify.setOutputConsumer(new OutputConsumer() {
				public void consumeOutput(InputStream in) throws IOException {
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					String line;
					while ((line = reader.readLine()) != null) {
						result.append(line);
					}
				}
			});
			
			IMOperation op = new IMOperation();
			op.addImage(image.getAbsolutePath());
			identify.run(op);
		} catch (Exception e) {
			throw new TilingException(e.getMessage());
		}
			
		if (result.length() == 0) throw new TilingException(IDENTIFY_ERROR);
		
		// Parse console output
		String[] params = result.toString().split(" ");
		String size = params[2];
		if (size.indexOf('+') > -1) size = size.substring(0, size.indexOf('+'));
		width  = Integer.parseInt(size.substring(0, size.indexOf('x')));
		height = Integer.parseInt(size.substring(size.indexOf('x') + 1));
	}
	
	public File getImageFile() {
		return file;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
}
