package edu.uni.lu.serval.par;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.Test;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.main.Main;
import edu.lu.uni.serval.utils.ShellUtils;
import junit.framework.TestCase;

public class MatchTest extends TestCase{
	private Logger log = Logger.getLogger(this.getClass().getName());
	
	// /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/ 		/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/ 		/home/dale/d4j/fixed_bugs_dir/Chart/ ~/env/defects4j/        Chart_3   Ochiai /home/dale/eclipse-projs/codesearch/search-log/chart/3
//	@Test
//	public void test_chart3() throws FileNotFoundException, IOException{
//		log.info("test chart 3.");
//		
//		String[] args = new String[]{
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/",
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/",
//				"/home/dale/d4j/fixed_bugs_dir/Chart/", 
//				"~/env/defects4j/",
//				"Chart_3",
//				"Ochiai",
//				"./search-log-fortest/chart/3"};
//		
//		Main.main(args);
//		// the result can be found in /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/kPAR/match-log/Chart/3/org.jfree.chart.axis.DateAxis_767-778
//	}
    
//     /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/ 
//    /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/
//	/home/dale/d4j/fixed_bugs_dir/Time/ 	~/env/defects4j/ 	Time_2
//	Ochiai 	./search-log-fortest/time/2
//    @Test
//	public void test_time2() throws FileNotFoundException, IOException{
//		log.info("test math 2.");
//		
//		String[] args = new String[]{
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/",
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/",
//				"/home/dale/d4j/fixed_bugs_dir/Time/", 
//				"~/env/defects4j/",
//				"Time_2",
//				"Ochiai",
//				"./search-log-fortest/time/2"};
//		
//		Main.main(args);
//		// the result can be found in /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/kPAR/match-log/Chart/3/org.jfree.chart.axis.DateAxis_767-778
//	}
    
//    @Test
//	public void test_mock4() throws FileNotFoundException, IOException{
//		log.info("run test_mock4.");
//		
//		String[] args = new String[]{
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/",
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/",
//				"/home/dale/d4j/fixed_bugs_dir/Mockito/", 
//				"~/env/defects4j/",
//				"Mockito_4",
//				"Ochiai",
//				"./search-log-fortest/mockito/4"};
//		
//		Main.main(args);
//		// the result can be found in /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/kPAR/match-log/Chart/3/org.jfree.chart.axis.DateAxis_767-778
//	}
	
//	@Test
//	public void test_chart1() throws FileNotFoundException, IOException{
//		log.info("run test_chart1.");
//		String proj = "Chart";
//    	String id = "1";
//    	String projId = proj + "_" + id;
//		String[] args = new String[]{
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/",
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/",
//				"/home/dale/d4j/fixed_bugs_dir/" + proj + "/", 
//				"~/env/defects4j/",
//				projId,
//				"Ochiai",
//				"/home/dale/eclipse-projs/codesearch/search-log/" + proj.toLowerCase() + "/" + id};
//		
//		for (String arg : args){
//			System.out.println(arg);
//		}
//		
//		Main.main(args);
//		// the result can be found in /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/kPAR/match-log/Chart/3/org.jfree.chart.axis.DateAxis_767-778
//	}
	
//	@Test
//	public void test_chart2() throws FileNotFoundException, IOException{
//		String proj = "Chart";
//    	String id = "2";
//    	String projId = proj + "_" + id;
//		String[] args = new String[]{
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/",
//				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/",
//				"/home/dale/d4j/fixed_bugs_dir/" + proj + "/", 
//				"~/env/defects4j/",
//				projId,
//				"Ochiai",
//				"/home/dale/eclipse-projs/codesearch/search-log/" + proj.toLowerCase() + "/" + id};
//		
//		for (String arg : args){
//			System.out.println(arg);
//		}
//		
//		Main.main(args);
//		// the result can be found in /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/kPAR/match-log/Chart/3/org.jfree.chart.axis.DateAxis_767-778
//	}

	public void testProjId(String proj, String id) throws FileNotFoundException, IOException{
//		String proj = "Chart";
//    	String id = "10";
    	String projId = proj + "_" + id;
    	
    	String repoBuggy = "/home/dale/d4j/";
		String repoFixed = "/home/dale/d4j/fixed_bugs_dir/";
		String[] cmd = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "/bin/bash single-download.sh "
				+ proj + " " + id + " 1"};
		ShellUtils.shellRun2(cmd);
		
		String[] cmd2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash  fixed_single_download.sh "
				+ proj + " " + id + " 1"};
		ShellUtils.shellRun2(cmd2);
//		ShellUtils.shellRun2("cd " + repoFixed 
//				+ " && " + "/bin/bash fixed_single_download.sh "
//				+ proj + " " + id + " 1");
    	
		String[] args = new String[]{
				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/",
				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/",
				"/home/dale/d4j/fixed_bugs_dir/" + proj + "/", 
				"~/env/defects4j/",
				projId,
				"Ochiai",
				"/home/dale/eclipse-projs/codesearch/search-log/" + proj.toLowerCase() + "/" + id};
		
		for (String arg : args){
			System.out.println(arg);
		}
		
		Main.main(args);
		// the result can be found in /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/kPAR/match-log/Chart/3/org.jfree.chart.axis.DateAxis_767-778
	}
	
	public void testProjIdOutSide(String proj, String id, String proj2) throws FileNotFoundException, IOException{
//		String proj = "Chart";
//    	String id = "10";
    	String projId = proj + "_" + id;
    	
    	String repoBuggy = "/home/dale/d4j/";
		String repoFixed = "/home/dale/d4j/fixed_bugs_dir/";
		String[] cmd = {"/bin/sh","-c", "cd " + repoBuggy 
				+ " && " + "/bin/bash single-download.sh "
				+ proj + " " + id + " 1"};
		ShellUtils.shellRun2(cmd);
		
		String[] cmdProj2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash fixed_single_download.sh "
				+ proj2 + " " + "1" + " 1"};
		ShellUtils.shellRun2(cmdProj2);
		
		String[] cmd2 = {"/bin/sh","-c", "cd " + repoFixed 
				+ " && " + "/bin/bash  fixed_single_download.sh "
				+ proj + " " + id + " 1"};
		ShellUtils.shellRun2(cmd2);
//		ShellUtils.shellRun2("cd " + repoFixed 
//				+ " && " + "/bin/bash fixed_single_download.sh "
//				+ proj + " " + id + " 1");
    	
		String[] args = new String[]{
				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/data/FailedTestCases/",
				"/home/dale/ALL_APR_TOOLS/FL-VS-APR-master/FaultLocalization-pr/GZoltar-0.1.1/SuspiciousCodePositions/",
				"/home/dale/d4j/fixed_bugs_dir/" + proj + "/", 
				"~/env/defects4j/",
				projId,
				"Ochiai",
				"/home/dale/eclipse-projs/codesearch/search-log/" + proj.toLowerCase() + "/" + id};
		
		for (String arg : args){
			System.out.println(arg);
		}
		
		Main.main(args);
		// the result can be found in /home/dale/ALL_APR_TOOLS/FL-VS-APR-master/kPAR/match-log/Chart/3/org.jfree.chart.axis.DateAxis_767-778
	}
	
	@Test
    public void test() throws IOException{
		// a simple test.
//		String matchPattern = "[^a-z^A-Z^0-9^!^=^\\-^\\+^<^>^*^/^\\.^&^|]";
////		String matchPattern = "[^a-z^A-Z^0-9^!^=^\\-^\\+^<^>^*^/]";
//		String line = "if (compare >= 0|||){{{ 2/1 2*1 2+1 2-1 2=1 if (type != Type.FUNCTION && aliasingGets > 0) {";
//		String fixLine_replace = line.replaceAll(matchPattern, " ").replaceAll(" +", " ");
//		System.out.print("");
		
//    	testProjId("Time","1");
    	// find a bug: fine-grained-modifications order should be considered.
    	// find a bug: fix varMapping strategy 1.(mapping when the var does not contain "." but is still a class instance.
    	// bug TODO: ZonedDateTimeField other = (ZonedDateTimeField) obj;(time1) the var:"other" can not be found by FixTemplate(ft)
	
//		testProjId("Time","2");
		// find a bug: java.lang.NullPointerException at edu.lu.uni.serval.bug.fixer.ParFixer2.matchFineGrained(ParFixer2.java:574)
		// TODO: org.joda.time.field.UnsupportedDurationField:227-229_fixed.log: here there are some similar possible/potential fixes. but I am not sure if we should consider it.

//		testProjId("Time","3");
//		// TODO: if consider longVar equivalent to intVar, the fix ingredient will be found for line830 (same situation for line797).
//		// only line639 finds a fix ingredient.
//		// after implementing parseContainCheck(), line662 also finds a fix ingredient!
//		
//		testProjId("Time","4");
//		
//		testProjId("Closure","1");
//		
//		testProjId("Closure","2");
		// find a bug: java.lang.NullPointerException at edu.lu.uni.serval.bug.fixer.ParFixer2.compareFixedAndSim(ParFixer2.java:455)
		// 		this bug will be triggered when "lines_xxx:1575-1576_fixed.log" range > "fixed code:/home/dale/d4j/fixed_bugs_dir/xxx-1575-1575" range.
		//      fix: when this happens, change <start_line, end_line> into the larger range
		// code improve: the parseContainCheck() should consider "!=" etc..
		// find a bug: ParFixer2 line712 results = results.trim();
		
//		testProjId("Closure","3");
		
//		testProjId("Closure","89");
		
//		testProjId("Closure","90");
		
//		testProjId("Mockito","1");
		
//		testProjId("Mockito","2");
		
//		testProjId("Mockito","4");
		//code improve TODO: mockito 4 fixed lines are actually 424,434. but current recorded lines are 421, 431
		//					this may involve some complicated modifications on source code. So marked as todo here.
		
//		testProjId("Math","4");
		// code improve TODO: there are actually some potential available fix ingredients(but their classes/datatype are not the same).
		// 					So I am not sure if the modification is needed. 
		// P.S. When I improve code for CodeSearch for Math_5, the Math_4 also gets many ingredients.
		
//		testProjId("Math","5");
		// bug fix: /home/dale/d4j/fixed_bugs_dir/Math/Math_1/src/main/java/org/apache/commons/math3/optim/linear/Relationship.java
		// 			this is a enum, not a class. (error: NPE: private Pair<String, String> getClazzSuperMethod(ITree scan, String flag) {)
	
		// TODO: 2&1 java.lang.NullPointerException
		
		testProjId("Math","6");
	}
}
