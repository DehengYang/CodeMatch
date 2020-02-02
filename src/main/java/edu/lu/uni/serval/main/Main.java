package edu.lu.uni.serval.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.lu.uni.serval.bug.fixer.AbstractFixer;
import edu.lu.uni.serval.bug.fixer.ParFixer;
import edu.lu.uni.serval.bug.fixer.ParFixer2;
import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.utils.SuspiciousPosition;

/**
 * Fix bugs with Fault Localization results.
 * 
 * @author kui.liu
 *
 */
public class Main {
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
//		if (args.length != 7) {
//			System.out.println("Arguments: <Failed_Test_Cases_File_Path> <Suspicious_Code_Positions_File_Path> <Buggy_Project_Path> <defects4j_Path> <Project_Name> <FL_Metric>");
//			System.exit(0);
//		}
		
//		Configuration.failedTestCasesFilePath = args[0]; // "./data/FailedTestCases/"
//		Configuration.suspPositionsFilePath = args[1];//"./FaultLocalization/GZoltar-0.1.1/SuspiciousCodePositions/"
		String buggyProjectsPath = args[0];// "./Defecst4JBugs/Defects4JData/"
		String defects4jPath = args[1]; // "~/environment/defects4j"  or "~/environment/defects4j/framework/bin/"
		String projectName = args[2]; // "Lang_24"
		Configuration.faultLocalizationMetric = "Ochiai"; // Ochiai
		Configuration.linesFilePath = args[3]; ///home/dale/eclipse-projs/codesearch/search-log/chart/3
		Configuration.outputPath += "FL/";
		
		Configuration.proj = projectName.split("_")[0];
		Configuration.id = projectName.split("_")[1];
		System.out.println(projectName);
		
		// for parsing line node
//		String classpath = 
//		int lineNo = 
//		SuspiciousPosition suspiciousCode = new SuspiciousPosition();
		
		fixBug(buggyProjectsPath, defects4jPath, projectName);
	}

	public static void fixBug(String buggyProjectsPath, String defects4jPath, String buggyProjectName) throws FileNotFoundException, IOException {
		String suspiciousFileStr = Configuration.suspPositionsFilePath;
		
		String dataType = "PAR";
		String[] elements = buggyProjectName.split("_");
		String projectName = elements[0];
		int bugId;
		try {
			bugId = Integer.valueOf(elements[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please input correct buggy project ID, such as \"Chart_1\".");
			return;
		}
		
		AbstractFixer fixer = new ParFixer2(buggyProjectsPath, projectName, bugId, defects4jPath);
		fixer.metric = Configuration.faultLocalizationMetric;
		fixer.dataType = dataType;
		fixer.suspCodePosFile = new File(suspiciousFileStr);
		// FIXME
//		if (Integer.MAX_VALUE == fixer.minErrorTest) {
//			System.out.println("Failed to defects4j compile bug " + buggyProjectName);
//			return;
//		}
		
		//FIXME
//		fixer.fixProcess();
		fixer.matchLines();
		
		int fixedStatus = fixer.fixedStatus;
		switch (fixedStatus) {
		case 0:
//			System.out.println("Failed to fix bug " + buggyProjectName);
			System.out.println("Finish code matching for bug : " + buggyProjectName);
			break;
		case 1:
//			System.out.println("Succeeded to fix bug " + buggyProjectName);
			break;
		case 2:
//			System.out.println("Partial succeeded to fix bug " + buggyProjectName);
			break;
		}
	}

}
