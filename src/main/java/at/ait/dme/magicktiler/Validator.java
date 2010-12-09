package at.ait.dme.magicktiler;

import java.io.File;

/**
 * Interface for file and tiling scheme validators. 
 * 
 * @author magicktiler@gmail.com
 */
public interface Validator {

	/**
	 * Methods shall return true if the directory is (potentially) a tileset
	 * directory, e.g. because it contains a tileset descriptor file. 
	 * @param dir the directory
	 * @return if the directory is a tileset directory
	 */
	public boolean isTilesetDir(File dir);
	
	/**
	 * Validate a tileset
	 * @param dir the tileset dir
	 * @throws ValidationFailedException if any tileset failed validation
	 */
	public void validate(File dir) throws ValidationFailedException;

}
