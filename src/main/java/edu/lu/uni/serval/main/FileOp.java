package edu.lu.uni.serval.main;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class FileOp {
	private static File bkupFolder;
	/**
	 * backup and restore
	 * @param dpSrcPath
	 * @param flag
	 * @throws IOException
	 */
	public static void backup(String dpSrcPath, String flag) throws IOException{
		if (flag.equals("backup")){
			// first backup
			// without "/"
			String srcPath = null;
			if (dpSrcPath.endsWith("/")){
				srcPath = dpSrcPath.substring(0, dpSrcPath.length() - 1);
			}else{
				srcPath = dpSrcPath;
			}
			// this.bkupFolder
			bkupFolder = new File(srcPath + "-ori");
			if (bkupFolder.exists() && bkupFolder.isDirectory()){
				// already have a backup folder
			}else{
				FileUtils.copyDirectory(new File(dpSrcPath), bkupFolder);
			}
		}else{
			// restore
			FileUtils.deleteDirectory(new File(dpSrcPath));
			FileUtils.copyDirectory(bkupFolder, new File(dpSrcPath));
		}
	}
}
