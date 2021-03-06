package com.github.keub.maven.plugin.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.github.keub.maven.plugin.exception.InvalidSourceException;
import com.github.keub.maven.plugin.model.Resource;
import com.github.keub.maven.plugin.resources.CopyResourcesMojo;
import com.github.keub.maven.plugin.utils.Constants;
import com.github.keub.maven.plugin.utils.FileUtils;
import com.github.keub.maven.plugin.utils.PathUtils;

public class FileService {

	/**
	 * <p>
	 * copy files into sourceFolder into destination folder
	 * </p>
	 * 
	 * @param copyResourcesMojo
	 * @param sourceFolder
	 * @param destinationFolder
	 * @param resource
	 * @throws InvalidSourceException
	 * @throws IOException
	 */
	static void copyFilesIntoOutputDirectory(CopyResourcesMojo copyResourcesMojo, File sourceFolder,
			File destinationFolder, Resource resource) throws InvalidSourceException, IOException {
		// check
		if (sourceFolder.isFile()) {
			throw new InvalidSourceException("Expected folder as source");
		}
		if (destinationFolder.isFile()) {
			throw new InvalidSourceException("Expected destination as source");
		}
		copyResourcesMojo.getLog().debug("Find file into '" + sourceFolder + "'");
		// find files into source folder
		Set<String> files = findFiles(sourceFolder);
		copyResourcesMojo.getLog().debug("files before processing :" + files);
		// process with optional include/exclude options
		files = processIncludeExclude(copyResourcesMojo, resource, files);

		if (files != null) {
			for (String file : files) {
				// source
				BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
				// destination
				StringBuilder finalFile = buildAbsoluteFinalFile(file, sourceFolder.getAbsolutePath(),
						destinationFolder);

				// prepare writer
				FileUtils.createIntermediateFolders(String.valueOf(finalFile));

				BufferedOutputStream writer = new BufferedOutputStream(
						new FileOutputStream(finalFile.toString(), false));

				// copy
				try {
					FileUtils.writeFile(reader, writer);
				}
				catch (IOException e) {
					throw e;
				}
				finally {
					// close all
					writer.close();
					reader.close();
				}
			}
			copyResourcesMojo.getLog().info(files.size() + " files in outputDirectory.");
		}
	}

	/**
	 * <p>
	 * return a file path list by applying the inclusion filters and exclusion
	 * configured
	 * </p>
	 * 
	 * @param copyResourcesMojo
	 * @param resource
	 * @param files
	 * @return
	 */
	private static Set<String> processIncludeExclude(CopyResourcesMojo copyResourcesMojo, Resource resource,
			Set<String> files) {
		Set<String> retval = new HashSet<String>(files);
		// process with include parameters
		retval = IncludeService.process(resource.getIncludes(), files);
		copyResourcesMojo.getLog().debug("files after include processing :" + files);
		// process with exclude parameters
		retval = ExcludeService.process(resource.getExcludes(), retval);
		copyResourcesMojo.getLog().debug("files after exclude processing :" + files);
		return retval;
	}

	/**
	 * <p>
	 * returns the absolute path to the final based file output folder where the
	 * resource is configured
	 * </p>
	 * 
	 * @param file
	 * @param basePath
	 * @param destinationFolder
	 * @return
	 */
	private static StringBuilder buildAbsoluteFinalFile(String file, String basePath, File destinationFolder) {
		StringBuilder retval = new StringBuilder(destinationFolder.getAbsolutePath());
		PathUtils.addEndingSlashIfNeeded(retval);
		String relativePath = file.replace(basePath, "");
		retval.append(relativePath);
		return retval;
	}

	/**
	 * <p>
	 * returns the list of files present in a folder
	 * </p>
	 * 
	 * @param sourceFolder
	 * @return
	 */
	public static Set<String> findFiles(File sourceFolder) {
		return findFiles(sourceFolder, sourceFolder);
	}

	/**
	 * <p>
	 * returns the list of files found in a folder. Does not return hidden files
	 * , unreadable , folder ' .git '
	 * </p>
	 * 
	 * @param sourceFolder
	 * @param baseFile
	 * @return
	 */
	private static Set<String> findFiles(File sourceFolder, File baseFile) {
		Set<String> retval = new HashSet<String>();
		if (!sourceFolder.exists() || sourceFolder.isHidden() || !sourceFolder.isDirectory() || !sourceFolder.canRead()
				|| isGitMetaDataFolder(sourceFolder)) {
			return retval;
		}
		for (File file : sourceFolder.listFiles()) {
			if (file.isDirectory()) {
				retval.addAll(findFiles(file, sourceFolder));
			}
			else {
				retval.add(file.getAbsolutePath());
			}
		}
		return retval;
	}

	/**
	 * <p>
	 * standardization of a path by replacing the slash and backslash by a file
	 * separator
	 * </p>
	 * 
	 * @param absolutePath
	 * @return
	 */
	static String normalizePath(String absolutePath) {
		String retval = absolutePath;
		retval = retval.replace(PathUtils.MULTIPLE_SLASH, PathUtils.SLASH);
		retval = retval.replace(PathUtils.BACKSLASH, PathUtils.SLASH);
		return retval;
	}

	/**
	 * <p>
	 * check if folder is '.git' folder
	 * </p>
	 * 
	 * @param file
	 * @return
	 */
	private static boolean isGitMetaDataFolder(File file) {
		return Constants.EXTENSION_GIT.equals(file.getName());
	}
}
