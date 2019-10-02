package edu.lu.uni.serval.bug.fixer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.main.FileOp;
import edu.lu.uni.serval.main.Triple;
import edu.lu.uni.serval.par.templates.FixTemplate;
import edu.lu.uni.serval.par.templates.fix.ClassCastChecker;
import edu.lu.uni.serval.par.templates.fix.CollectionSizeChecker;
import edu.lu.uni.serval.par.templates.fix.ExpressionAdder;
import edu.lu.uni.serval.par.templates.fix.ExpressionRemover;
import edu.lu.uni.serval.par.templates.fix.ExpressionReplacer;
import edu.lu.uni.serval.par.templates.fix.MethodReplacer;
import edu.lu.uni.serval.par.templates.fix.NullPointerChecker;
import edu.lu.uni.serval.par.templates.fix.ParameterAdder;
import edu.lu.uni.serval.par.templates.fix.ParameterRemover;
import edu.lu.uni.serval.par.templates.fix.ParameterReplacer;
import edu.lu.uni.serval.par.templates.fix.RangeChecker;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.context.ContextReader2;
import edu.lu.uni.serval.utils.Checker;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.ShellUtils;
import edu.lu.uni.serval.utils.SuspiciousPosition;
import edu.lu.uni.serval.utils.SuspiciousCodeParser;

/**
 * This is to create a variable mapping/code matching version.
 * @author dale
 *
 */
public class ParFixer2 extends AbstractFixer {
	
	private static Logger log = LoggerFactory.getLogger(ParFixer2.class);
	public static String extraLog = "./match-log/" + Configuration.proj + '/' + Configuration.id + "/extra-info.log";
	
	// dale
	private static String logPath = null; // output path
	private int startLineNo = 0; 
	private int endLineNo = 0;
	private int matchedStartLineNo = 0;
	private int matchedEndLineNo = 0;
	private String fullPath = null;
	private String dpPath = null;
	private String projId = null;
	private String matchedFlag = null;
	private String matchedLogPath = null;
	private List<String> fixed_code = new ArrayList<>();
	private List<String> similar_code = new ArrayList<>();
	private String classPath = "null";
	
	// code improvement: add HashMap for patch_match
	private Map<Integer, String> patchLinesMap = new HashMap<Integer, String>(); 
	
	private int patchStartLineNo = 0;
	private int patchEndLineNo = 0;
	// bug fix: I forget to set patchClassPath
	private String patchClassPath = "";
	// code improve
	private String patchMethod = null;
	private String matchMethod = null;
	
	// code improve: only save the same fixCode once. (do not repeat)
	private int fixCodeSaveFlag = 0;
	
	// code improve:
	// private File bkupFolder;
	private int parsedLinesCnt = 0;
	
	public ParFixer2(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	public ParFixer2(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}
	
	public void initAll(){
		patchStartLineNo = 0;
		patchEndLineNo = 0;
		
		patchClassPath = "";
		patchMethod = null;
		matchMethod = null;
		
		fixCodeSaveFlag = 0;
		
		// bug fix: clear again!
		patchLinesMap.clear();
		// code improve: output to a matched file for each fix code.
		matchedLogPath = null;
		// bug fix: logPath should not be null. When fixing, try to change matchLinesFromEachFile method
		logPath = null;
	}
	
	/**
	 * To find matched similar code snippet(s) for the given fixed code snippet(s).
	 */
	public void matchLines() throws IOException{
		// init: delete previous output results 
		init();
				
		// get CodeSearch result output file
		File dir = new File(Configuration.linesFilePath);
		if (!dir.exists()){
			print("File : " + Configuration.linesFilePath + " does not exist!");
			return;
		}
		
		// list all files in the dir, and select what we want.
		File[] files = dir.listFiles(); 
		for (File file : files){
			String fileName = file.getName();
			int len = fileName.length();
			String filePath = null;
			
			// bug fix: exclude xxx.~ file. 
			if(fileName.substring(0,6).equals("lines_") 
					&& !fileName.substring(len-1, len).equals("~")){
				filePath = file.getAbsolutePath();

				// get patch lines
				//e.g., lines_org.joda.time.Partial:218-218_fixed.log
				String[] linesTmp = filePath.split(":")[1].split("_")[0].split("-");
				int startLine = Integer.parseInt(linesTmp[0]);
				int endLine = Integer.parseInt(linesTmp[1]);

				// init all relevant
				initAll();
				for (int line = startLine; line <= endLine; line ++){
					patchLinesMap.put(line, "");
				}
				
				// exclude type1 lines
				excludeTypeOneLines(patchLinesMap);
				
				if(patchLinesMap.isEmpty()){
					print("patchLinesMap is empty, continue for next patch chunk.");
					continue;
				}
				
				matchLinesFromEachFile(filePath, startLine, endLine);
			}
			
			// a simple check.
			if (filePath == null){
				System.out.println("filePath not found in : " + Configuration.linesFilePath);
				continue;
			}
		}
		
		
	}
	
	/**
	 * exlcude type1 lines from pacthLinesMap
	 * @param patchLinesMap2
	 */
	private void excludeTypeOneLines(Map<Integer, String> patchLinesMap2) {
		File dir = new File(Configuration.linesFilePath);
		// list all files in the dir, and select what we want.
		File[] files = dir.listFiles(); 
		for (File file : files){
			String fileName = file.getName();
			int len = fileName.length();
			// e.g., org.jfree.chart.renderer.category.AbstractCategoryItemRenderer:1797_type1.log
			if(len >= 10 && fileName.substring(len-10,len).equals("_type1.log")){
				// get type1 lines
				int line = Integer.parseInt( fileName.split(":")[1].split("_")[0] );

				// exclude type1 lines now
				if (patchLinesMap2.containsKey(line)){
					patchLinesMap2.remove(line);
				}
			}
		}
	}

	public void matchLinesFromEachFile(String filePath, int startLine, int endLine) throws IOException{
		// read lines
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath)));
		SuspiciousPosition sc = new SuspiciousPosition();
		
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			// /home/dale/d4j/fixed_bugs_dir/Chart/Chart_3/source/org/jfree/data/time/TimeSeries
			line = line.trim();
			String line1 = line.split(":")[1];
			String[] lines = line1.split("\\.");
			String fullPath = lines[0];
			
			// set full path. 
			// e.g., /home/dale/d4j/fixed_bugs_dir/Chart/Chart_3/source/org/jfree/data/time/TimeSeries
			this.fullPath = fullPath;
			
			// java-1057-1058
			String lineRanges = line.split(":")[1].split("\\.")[1];
			
			this.startLineNo = Integer.parseInt(lineRanges.split("-")[1]);
			this.endLineNo = Integer.parseInt(lineRanges.split("-")[2]);
			
			// source java src
			int index;
			if (fullPath.contains("/source/")){
				index = fullPath.indexOf("/source/") + 8;
//				classPath = fullPath.substring(index + 8);
			}else if(fullPath.contains("/java/")){
				index = fullPath.indexOf("/java/") + 6;
//				classPath = fullPath.substring(index + 6);
			}else if(fullPath.contains("/src/")){
				index = fullPath.indexOf("/src/") + 5;
//				classPath = fullPath.substring(index + 5);
			}else{
				System.err.println("unexpected classpath: " + fullPath);
				return;
			}
			
			classPath = fullPath.substring(index);
			// /home/dale/d4j/fixed_bugs_dir/Chart/Chart_3/src/xxx
			// get projId
			String id = fullPath.split("_")[ fullPath.split("_").length - 1 ].split("/")[0];
			String[] projTemp = fullPath.split("_")[ fullPath.split("_").length - 2 ].split("/");
			String proj = projTemp[projTemp.length - 1];
			this.projId = proj + "_" + id; // get proj_id
			index = fullPath.indexOf(this.projId);
			this.dpPath = fullPath.substring(0, index);
			
			classPath = classPath.replace("/", ".");
			sc.classPath = classPath;
			if(line.contains("fixed code:")){
				// feature implementation: 
				patchStartLineNo = this.startLineNo;
				patchEndLineNo = this.endLineNo;
				patchClassPath = classPath;
				
				if (logPath == null){
					// bug fix: file name.
					logPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + startLine + "-" + endLine; 
				}
				if (matchedLogPath == null){
					// bug fix: change logPath to matchedLogPath
					matchedLogPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + startLine + "-" + endLine + "_matched.log"; 
				}
				
				writeStringToFile(logPath, "fixed code: \n", true);
				runMatchSingleLine(sc, classPath, "fixed_code");;
			}else if(line.contains("similar code:")){
				// code improve: exclude identical similar code.
				if(patchStartLineNo == startLineNo && patchEndLineNo == endLineNo
						&& patchClassPath == classPath){
					continue;
				}
				
				// TODO:This can be actually deleted. 
				if (logPath == null){
					logPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + this.startLineNo + "-" + this.endLineNo; 
				}
				if (matchedLogPath == null){
					matchedLogPath = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' 
						+ classPath + "_" + this.startLineNo + "-" + this.endLineNo + "_matched.log"; 
				}
				
				writeStringToFile(logPath, "similar code: \n", true);
				runMatchSingleLine(sc, classPath, "sim_code");;
			}else{
				System.err.println("unexpected line format: " + line);
				return;
			}
		}
		br.close();
	}
	
	/**
	 * get before_match_code and after_match_code (if exists).
	 * flag: fixed_code, sim_code.
	 * @param sc
	 * @param classPath
	 * @param flag
	 * @throws IOException
	 */
	public void runMatchSingleLine(SuspiciousPosition sc, String classPath, String flag) throws IOException{
		List<String> beforeMatches = new ArrayList<>();
		List<String> afterMatches  = new ArrayList<>();
		
		// bug fix: re-set matchedStart && end LineNo 
		this.matchedStartLineNo = 0;
		this.matchedEndLineNo = 0;
		this.matchedFlag = "";
		
		for (int lineNo = this.startLineNo; lineNo <= this.endLineNo; lineNo ++){
			sc.lineNumber = lineNo;
			Pair<String, String> pair = matchSingleLine(sc, flag);
			beforeMatches.add(pair.getFirst()+"\n");
			afterMatches.add(pair.getSecond()+"\n");
		}
		
		writeStringToFile(logPath, "---before match--- "
				+ classPath + " <" + this.startLineNo + ", " + this.endLineNo + ">\n", true);
		for (String match : beforeMatches){
			// fix bug: add trim() to wipe out "\n"
			if (match.trim().equals("")){
				continue;
			}
			writeStringToFile(logPath, match, true);
		}
		// code improvement: print matched string only when the code snippet is changed.
		if (!matchedFlag.equals("")){
			writeStringToFile(logPath, "\n---after match--- " + matchedFlag + " \n", true);
			for (String match : afterMatches){
				// fix bug: add trim() to wipe out "\n"
				if (match.trim().equals("")){
					continue;
				}
				writeStringToFile(logPath, match, true);
			}
		}
		
		writeStringToFile(logPath, "\n\n", true);
		
		// get fixed_code
		if (flag.equals("fixed_code")){
			fixed_code.clear();
			if (!matchedFlag.equals("")){
				fixed_code.addAll(afterMatches);
			}else{
				fixed_code.addAll(beforeMatches);
			}
		}else{
			similar_code.clear();
			if (!matchedFlag.equals("")){
				similar_code.addAll(afterMatches);
			}else{
				similar_code.addAll(beforeMatches);
			}
		}
		
		trimCode(fixed_code);
		trimCode(similar_code);
		compareFixedAndSim(fixed_code, similar_code);
	}
	
	private void trimCode(List<String> code){
		// bug fix: should change line <= into line <
		for(int line = 0; line < code.size(); line++){
			String lineCur = code.get(line);
			code.set(line, lineCur.trim());
		}
	}
	
	private void compareFixedAndSim(List<String> fixed_code_ori, List<String> sim_code_ori) {
		// delete all "" string in the two list.
		List<String> fixed_code = filterNullStr(fixed_code_ori);
		List<String> sim_code = filterNullStr(sim_code_ori);
				
		
		if(fixed_code.isEmpty() || sim_code.isEmpty()){
			return;
		}
		
		int equalFlag = 1;
		for (String fixLines : fixed_code){
			// bug fix: (a line may include several lines, so) split into lines
			String[] fix_lines = fixLines.split("\n");
			for (String fixLine : fix_lines){
				fixLine = fixLine.trim();
				
				if (!patchLinesMap.containsValue(fixLine)){
					//writeStringToFile(extraLog, "patchLinesMap does not contain " + fixLine + "\n", true);
					continue;
				}
				
				// check if in sim_code
				int equalFlag2 = 0;
				for (String simLines:sim_code){
					List<String> sim_lines =  Arrays.asList(simLines.split("\n"));
					trimCode(sim_lines);

					// bug fix: change sim_code into sim_lines
					// bug fix
					// if (!sim_lines.contains(fixLine) && patchLinesMap.containsValue(fixLine)){
					if (sim_lines.contains(fixLine) ){
						equalFlag2 = 1;
						break;
					}
				}
				// fail to find fixed_line in similar code. thus equalFlag = 0
				if (equalFlag2 == 0){
					equalFlag = 0;
					break;
				}
			}
			if(equalFlag == 0){ //stop loop once any patched line is not matched.
				break;
			}
		}
		
		if (equalFlag == 1){
			if(fixCodeSaveFlag == 0){
				// bug fix: a copy&paste bug. should change logPath to matchedLogPath
				writeStringToFile(matchedLogPath, "---patch code--- "
						+ patchClassPath + " <" + this.patchStartLineNo + ", " + this.patchEndLineNo + ">\n", true);
				for (String match : fixed_code){
					// fix bug: add trim() to wipe out "\n"
					if (match.trim().equals("")){
						continue;
					}
					writeStringToFile(matchedLogPath, match + "\n", true);
				}
				writeStringToFile(matchedLogPath, "\n", true);	

				fixCodeSaveFlag = 1;
			}
			String str = "";
			if(patchMethod.equals(matchMethod)){
				str = "---fix ingredient---(SameMethod) ";
			}else{
				str = "---fix ingredient--- ";
			}
			writeStringToFile(matchedLogPath, str
					+ classPath + " <" + this.startLineNo + ", " + this.endLineNo + ">\n", true);
			for (String match : sim_code){
				// fix bug: add trim() to wipe out "\n"
				if (match.trim().equals("")){
					continue;
				}
				writeStringToFile(matchedLogPath, match + "\n", true);
			}
			writeStringToFile(matchedLogPath, "\n\n", true);
		}
	}

	private List<String> filterNullStr(List<String> code_ori) {
		List<String> filted_list = new ArrayList<>();
		
		for(String line : code_ori){
			if(!line.equals("")){
				filted_list.add(line);
			}else{
//				print("skip null string in code");
			}
		}
		return filted_list;
	}

	public Pair<String, String> matchSingleLine(SuspiciousPosition sc, String flag) throws IOException {
		// count 
		parsedLinesCnt ++;
		String str = "Parsed Lines Count: " + parsedLinesCnt 
				+ " : " + sc.classPath + ":" + sc.lineNumber ;
		print(str);
		writeStringToFile(extraLog, str + "\n", true);
		
		// reset dp for each line.
		this.dp.reset(this.dpPath, this.projId);
		
		//backup
//		FileOp.backup(dp.srcPath, "backup");
		
		// get the AST node for the line
		SuspCodeNode scn1 = parseSuspiciousCode(sc);
		
		if (scn1 == null){
			// (fix bug) if in matched. return "". else return "null_ast" str.
			if (sc.lineNumber <= this.matchedEndLineNo &&
					sc.lineNumber >= this.matchedStartLineNo){
				return new Pair<String, String>("", "");
			}
			String javaFilePath = fullPath + ".java";
			String results = ShellUtils.shellRunForFileRead(Arrays.asList(
				"cat " + javaFilePath + " | sed -n " + sc.lineNumber + "p" ), buggyProject);
			if (results.trim().equals("")){
				results = "empty line";
			}
			return new Pair<String, String>(results, results);		
		}
		
		ITree scan = scn1.suspCodeAstNode; 
		
		// get clazz and super clazz
		Pair<String, String> clazzAndSuper = getClazzSuperMethod(scan, flag);
		String clazz = clazzAndSuper.getFirst();
		String superClazz = clazzAndSuper.getSecond();
		
		// first initialze a FixTemplate
		FixTemplate ft = null;
		ft = new ExpressionReplacer();
		ft.setSuspiciousCodeStr(scn1.suspCodeStr);
		ft.setSuspiciousCodeTree(scan); //scn1.suspCodeAstNode
		if (scn1.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn1.javaBackup);
		
		// parse classpath.java
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		String suspiciousJavaFile = sc.classPath.replace(".", "/") + ".java";
		String filePath = this.dp.srcPath + suspiciousJavaFile;
		scp.parseJavaFile(new File(filePath));
		int currentStartLineNo = scp.getUnit().getLineNumber(scan.getPos());
		int currentEndLineNo=  scp.getUnit().getLineNumber(scan.getPos()+ scan.getLength());
		
		// exclude repeated ast match
		// bug fix: (math81) change this.matchedEndLineNo >= currentStartLineNo into this.matchedEndLineNo >= currentEndLineNo
		if(this.matchedStartLineNo <= currentStartLineNo && 
				this.matchedEndLineNo >= currentEndLineNo){
			return new Pair<String, String>("", "");
		}else{
			// fix bug: sometimes the line number may match a if() ast that includes
			// even the before ast (thus causing repetition). 
			// In this situation, we want to exlude it.
			// P.S. after re-debugging by inserting a breakpoint here (math 81), I remember that when the line is a "}", it will correspond to a whole if() block. Therefore we need to exclude this situation.
			if (this.matchedStartLineNo != 0 && this.matchedEndLineNo != 0){
				if (currentStartLineNo <= this.matchedStartLineNo &&
						currentEndLineNo >= this.matchedEndLineNo){
					// TODO: change "" to null
					return new Pair<String, String>("", "");
				}
			}
		}
		this.matchedStartLineNo = currentStartLineNo;
		this.matchedEndLineNo = currentEndLineNo;
		
		// get all variables
		List<ITree> suspVars = new ArrayList<>();
		// FIXME: identifySuspiciousVariables method should be modified for my purpose 
		ContextReader.identifySuspiciousVariables(scan, suspVars, new ArrayList<String>());
		ContextReader.readAllVariablesAndFields(scan, ft.allVarNamesMap, ft.varTypesMap, ft.allVarNamesList, ft.sourceCodePath, ft.dic);
		
		// now, find all variables in patchLines (rather than the whole ast code!)
		// this will save much effort, I suppose.
		// now, I change my mind, I can use ContextReader.identifySuspiciousVariables to do this thing.
		int startPosScan = scan.getPos();
		int endPosScan = startPosScan + scan.getLength();
		String before_match = scn1.suspCodeStr; // before match
		String mappedCodeStr = ""; // code str after variable mapping.
		int tmpStartPosScan = startPosScan;
		
		// sort the suspVars list
		Collections.sort(suspVars, new Comparator<ITree>() {
			@Override
			public int compare(ITree  o1, ITree o2) {
				// if clazz1 greater than 2, compareTo return 1. 
				if(o1.getPos() >= o2.getPos()){
					return 1;
				} else{
					return -1;
				}
			}
		});
		
		// first identify whether xxclass.var exists in ft.varTypesMap:
		// chart 2 & math 81: only have this.var or var.
		
		// modifiy ft.varTypesMap
		Map<String, String> varNewTypesMap = new HashMap<>();
		varNewTypesMap.putAll(ft.varTypesMap);
		for(Map.Entry<String, String> entry : ft.varTypesMap.entrySet()){
			String var = entry.getKey();
			String type = entry.getValue();
			if(SuspiciousCodeParser.isClass(type)){ // if is a class
				// find its super class
				
				String[] cmd = {"/bin/sh","-c", " find " 
					+ dp.srcPath + " -name " + type + ".java"
					};
				// e.g., find /home/dale/d4j/Chart/Chart_1/source/ -name AbstractCategoryItemRenderer.java
				String result = ShellUtils.shellRun2(cmd);
				
				// bug fix: chart3: propertyChangeSupport is a class, but I cannot find it... And the result is ""
				if (result.trim().equals("")){
					continue;
				}
				
				SuspiciousCodeParser scp2 = new SuspiciousCodeParser();
				String filePath2 = result.trim();
				ITree rootTree = scp2.getRootTree(new File(filePath2));
				for(ITree itree : rootTree.getChildren()){
					if(Checker.isTypeDeclaration(itree.getType())){		
						// if is class declaration, get class and super class name for "type<which is a class type>"
						String[] clazzStrList = itree.toString().split(",");
						String superClazz2 = null;
						String clazz2 = null;
						for (String clazzStr : clazzStrList){
							if(clazzStr.contains("@@SuperClass:")){
								superClazz2 = clazzStr.split(":")[1];
							}
							if(clazzStr.contains("ClassName:")){
								clazz2 = clazzStr.split(":")[1];
							}
						}
						if(superClazz2 != null){
							varNewTypesMap.put(var, superClazz2);
						}else if(clazz2 != null){
							varNewTypesMap.put(var, clazz2);
						}else{
							varNewTypesMap.put(var, type);
							print("error occurs in modifiying ft.varTypesMap;");
						}
					}
				}
			}else if(type.equals("Integer")){
				varNewTypesMap.put(var, "int"); //change Integer to int
			}else{
				// do nothing.
				varNewTypesMap.put(var, type + "Var"); //add Var, e.g., int to intVar
			}
		}
		
		
		for (ITree var : suspVars){
			// init
			String mappedVar = ""; // just for replace original varibale with mappedVar
			int startPos = var.getPos();
            int endPos =  startPos + var.getLength();
            
            // simple check (if out of bound)
            if (startPos < startPosScan || endPos > endPosScan){
            	writeStringToFile(extraLog, var.getLabel() + "is out of index! \n",true);
            	continue;
            }
            
            // code part 1
            String codePart1 = ft.getSubSuspiciouCodeStr(tmpStartPosScan, startPos);
            mappedCodeStr += codePart1; // first add unchanged code part.
            
            String label = var.getLabel();
            
            // code improve: more complicated var checking and var mapping.
            if(label.contains(".")){ //if var is : this.var || var.length || varA.varB.length
	            // bug fix: change "." into "\\." when spliting
            	String[] subLabels = label.split("\\.");
	            for(String subLabel : subLabels){ // traverse subLabels
	            	if(varNewTypesMap.containsKey(subLabel)){
	            		mappedVar += varNewTypesMap.get(subLabel) + "."; // map variable into <xxxtype>Var
	            	}else if(varNewTypesMap.containsKey("this." + subLabel)){
	            		mappedVar += varNewTypesMap.get("this." + subLabel) + "."; // map variable into <xxxtype>Var
	            	}else{
	            		mappedVar += subLabel + ".";
	            	}
	            }
	            int len = mappedVar.length();
	            mappedVar = mappedVar.substring(0,len-1);  // wipe out last additional "."
            }else{ // condition: var without "."
            	if(varNewTypesMap.containsKey(label)){
                	mappedVar = varNewTypesMap.get(label); // map variable into <xxxtype>Var
                }else if(varNewTypesMap.containsKey("this." + label)){
                	mappedVar = varNewTypesMap.get("this." + label); // map variable into <xxxtype>Var
                }else{
                	writeStringToFile(extraLog, var.getLabel() + "is not in ft.varTypesMap! \n",true);
                	continue;
                }
            }
            // simple check
            // bug fix:org.apache.commons.math.analysis.polynomials.PolynomialFunction:143 (math 81)
//            if(ft.varTypesMap.containsKey(label)){
//            	mappedVar = ft.varTypesMap.get(label) + "Var"; // map variable into <xxxtype>Var
//            }else if(ft.varTypesMap.containsKey("this." + label)){
//            	mappedVar = ft.varTypesMap.get("this." + label) + "Var"; // map variable into <xxxtype>Var
//            }else{
//            	writeStringToFile(extraLog, var.getLabel() + "is not in ft.varTypesMap! \n",true);
//            	continue;
//            }
            
            mappedCodeStr += mappedVar; // add mapped var
            tmpStartPosScan = endPos; // change start pos. (because mapped code str already add varTypeMatched.
		}
		mappedCodeStr += ft.getSubSuspiciouCodeStr(tmpStartPosScan, endPosScan);// add last rest code str
		print(before_match);
		print(mappedCodeStr);
		print("");
		
////		Map<String, String> codeParts = new HashMap<String, String>();
//		ValueComparator bvc = new ValueComparator(ft.varTypesPosMap);
//        TreeMap<String, Triple<String, Integer, Integer>> sortedVarTypesPosMap = new TreeMap<String, Triple<String, Integer, Integer>>(bvc);
//        sortedVarTypesPosMap.putAll(ft.varTypesPosMap);
//        writeStringToFile(extraLog, sortedVarTypesPosMap.toString() + "\n", true);
//        String mappedCodeStr = "";
//        //for(Map.Entry<String, Triple<String, Integer, Integer>> entry : sortedVarTypesPosMap.entrySet()){
//        for (ITree var : suspVars){
////        	String mapKey = entry.getKey();
////            Triple<String, Integer, Integer> mapValue = entry.getValue();
//            int startPos = var.getPos();
//            int endPos =  startPos + var.getLength();
//            
//            // simple check (if out of bound)
//            if (startPos < startPosScan || endPos > endPosScan){
//            	writeStringToFile(extraLog, var.getLabel() + "is out of index! \n",true);
//            	continue;
//            }
//            
//            String codePart1 = ft.getSubSuspiciouCodeStr(startPosScan, startPos);
//			String codePart2 = ft.getSubSuspiciouCodeStr(startPos,endPos);
//			mappedCodeStr += codePart1;
////			mappedCodeStr += mapValue.getFirst();
////			startPosScan = endPos; // prepare to deal with next var
//        }
//        print(mappedCodeStr);
		
		
		
		
		// find class instances
//		scp.findClazzInstance(scan, ft);
//		ITree clazzIns = scp.getClazzInstance();
//		
//		// for each class instance, replace it with its class name
//		String before_match = scn1.suspCodeStr;
//		int whileCnt = 0;
//		while(clazzIns != null && whileCnt <= 30){
//			whileCnt ++;
//			
//			int suspExpStartPos = clazzIns.getPos();
//			int suspExpEndPos = suspExpStartPos + clazzIns.getLength();
//			// specified line start and end pos
//			int suspCodeStartPos = scan.getPos();
//			int suspCodeEndPos = suspCodeStartPos + scan.getLength();
//			// part 1 and 2
//			String suspCodePart1 = ft.getSubSuspiciouCodeStr(suspCodeStartPos, suspExpStartPos);
//			String suspCodePart2 = ft.getSubSuspiciouCodeStr(suspExpEndPos,suspCodeEndPos);
//			
//			// conduct replacement
//			String label = clazzIns.getLabel();
//			// sometimes lalel may be "Name:area"
//			if (label.contains(":")){
//				label = label.split(":")[1];
//			}
//			
//			String replace = "null";
//			if(ft.varTypesMap.containsKey(label)){
//				replace = ft.varTypesMap.get(label);
//				
//				// if is same as current class, then  
//				if (replace.equals(clazz) && superClazz != null){
//					replace = superClazz;
//				}
//				
//				// if not, parse the file and get the class.
////				String[] cmd2 = {"/bin/sh","-c", " find " 
////						+ dp.srcPath + " -name " + replace + ".java"
////						};
////				// e.g., find /home/dale/d4j/Chart/Chart_1/source/ -name AbstractCategoryItemRenderer.java
////				String result = ShellUtils.shellRun2(cmd2);
////				String otherClazz = scp.getSuperClazz(new File(result.trim()));
////				if(otherClazz != null){
////					replace = otherClazz;
////				}
//			}
//			
//			// FIXME: multi replace
//			scn1.suspCodeStr = suspCodePart1  + replace + suspCodePart2;
//			
//			// set matchedFlag
//			this.matchedFlag = "matched";
//			
//			// apply and re-parseAST
//			Patch patch = new Patch();
//			patch.setFixedCodeStr1(scn1.suspCodeStr);
//			this.addPatchCodeToFile(scn1, patch);// Insert the patch.
//
//			// re-parse
//			scn1 = parseSuspiciousCode(sc);
//			if (scn1 == null){
//				System.err.println("scn1 is null. sc:" + sc.lineNumber);
//				return new Pair<String, String>("null_ast", "null_ast");
//			}
//			ft.setSuspiciousCodeStr(scn1.suspCodeStr); // re-set ft.values
//			ft.setSuspiciousCodeTree(scn1.suspCodeAstNode); //scn1.suspCodeAstNode
//			// refind 
//			scan = scn1.suspCodeAstNode; 
//			// refresh scp
//			scp = new SuspiciousCodeParser();
//			suspiciousJavaFile = sc.classPath.replace(".", "/") + ".java";
//			filePath = this.dp.srcPath + suspiciousJavaFile;
//			scp.parseJavaFile(new File(filePath));
//			// bug Fix:
//			scp.resetClazzInstance();
//			scp.findClazzInstance(scan, ft);
//			clazzIns = scp.getClazzInstance();
////			ft.generatePatch(suspCodePart1  + replace + suspCodePart2);
//		}
		// replace this.
		if (mappedCodeStr.contains("this.")){
			if (superClazz != null ){
				mappedCodeStr = mappedCodeStr.replace("this.", superClazz + ".");
			}else if (clazz != null){
				mappedCodeStr = mappedCodeStr.replace("this.", clazz + ".");
			}else{
				print("cannot find both class and super class.");
			}
			
		}
//		
//		// restore
//		FileOp.backup(dp.srcPath, "restore");
//		
//		// set map values
		String[] lines = mappedCodeStr.split("\n");
		int linesNo = lines.length;
		int linesNo2 = currentEndLineNo - currentStartLineNo + 1;
		if (linesNo != linesNo2){
			writeStringToFile("error.log", "linesNo != linesNo2 \n"
					+ currentEndLineNo + " to " + currentStartLineNo
					+ classPath + "\n\n",true);
		}else if(flag != "fixed_code"){
			// bug fix: add else if.
			// do nothing
		}else{
			int cnt = 0;
			for (int lineNo = currentStartLineNo; lineNo <= currentEndLineNo; lineNo ++){
				if (patchLinesMap.containsKey(lineNo)){
					patchLinesMap.put(lineNo, lines[cnt].trim());
				}
				cnt ++;
			}
		}
		
		// set matchedFlag
		if(!before_match.equals(mappedCodeStr)){
			this.matchedFlag = "matched";
		}
		
		return new Pair<String, String>(before_match, mappedCodeStr);
	}

	// get class name, super class, and method name string
	private Pair<String, String> getClazzSuperMethod(ITree scan, String flag) {
		// First: get class and super class (if exists) name 
		ITree parent = scan;
		// TODO: to test whether 1056 TimeSeries copy = (TimeSeries) super.clone(); is typeDecl
		while(!Checker.isTypeDeclaration(parent.getType()) ){
			if (Checker.isMethodDeclaration(parent.getType())){
//				String[] labels = parent.getLabel().split(",");
				// start to analyze method
				// e.g., public, @@LegendItemCollection, MethodName:getLegendItems, @@Argus:null
//				String returnVar = labels[1].trim();
//				String methodName = lables[2].trim();
				// Code improve: to do this in a simpler way!
				if(flag == "fixed_code"){
					this.patchMethod = parent.getLabel();
				}else{
					this.matchMethod = parent.getLabel();
				}
			}
			
			parent = parent.getParent();
			
		}
		// print(parent.toString());
		String[] clazzStrList = parent.toString().split(",");
		String superClazz = null;
		String clazz = null;
		for (String clazzStr : clazzStrList){
			if(clazzStr.contains("@@SuperClass:")){
				superClazz = clazzStr.split(":")[1];
			}
			if(clazzStr.contains("ClassName:")){
				clazz = clazzStr.split(":")[1];
			}
		}
		return new Pair<String, String>(clazz, superClazz);
	}

	class SuspNullExpStr implements Comparable<SuspNullExpStr> {
		public String expStr;
		public Integer startPos;
		public Integer endPos;
		
		public SuspNullExpStr(String expStr, Integer startPos, Integer endPos) {
			this.expStr = expStr;
			this.startPos = startPos;
			this.endPos = endPos;
		}

		@Override
		public int compareTo(SuspNullExpStr o) {
			int result = this.startPos.compareTo(o.startPos);
			 if (result == 0) {
				 result = this.endPos.compareTo(o.endPos);
			 }
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SuspNullExpStr)) return false;
			return this.expStr.equals(((SuspNullExpStr) obj).expStr);
		}
		
	}
	
//	private void identifySuspiciousVariables(FixTemplate ft, ITree suspCodeAst, List<String> allSuspVariables, List<SuspNullExpStr> suspNullExpStrs) {
//		List<ITree> children = suspCodeAst.getChildren();
//		for (ITree child : children) {
//			int childType = child.getType();
//			if (Checker.isSimpleName(childType)) {
//				int parentType = suspCodeAst.getType();
//				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
//						&& suspCodeAst.getChildPosition(child) == 0) {
//					continue;
//				}
//				if ((Checker.isQualifiedName(parentType) || Checker.isFieldAccess(parentType) || Checker.isSuperFieldAccess(parentType)) &&
//						suspCodeAst.getChildPosition(child) == children.size() - 1) {
//					continue;
//				}
//				
//				String varName = ContextReader.readVariableName(child);
//				if (varName != null && !varName.endsWith(".length")) {
//					int startPos = child.getPos();
//					int endPos = startPos + child.getLength();
//					SuspNullExpStr snes = new SuspNullExpStr(varName, startPos, endPos);
//					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//					if (!allSuspVariables.contains(varName)) allSuspVariables.add(varName);
//				}
//				else identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isMethodInvocation(childType)) {
//				List<ITree> subChildren = child.getChildren();
//				if (subChildren.size() > 2) {
//					int startPos = child.getPos();
//					ITree subChild = subChildren.get(subChildren.size() - 2);
//					int endPos = subChild.getPos() + subChild.getLength();
//					String suspExpStr = ft.getSubSuspiciouCodeStr(startPos, endPos);
//					SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
//					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//				}
//				identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isArrayAccess(childType)) {
//				int startPos = child.getPos();
//				int endPos = startPos + child.getLength();
//				String suspExpStr = ft.getSubSuspiciouCodeStr(startPos, endPos);
//				SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
//				if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//						&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//				identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isQualifiedName(childType) || Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
//				int parentType = suspCodeAst.getType();
//				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
//						&& suspCodeAst.getChildPosition(child) == 0) {
//					continue;
//				}
//				int startPos = child.getPos();
//				int endPos = startPos + child.getLength();
//				String suspExpStr = ft.getSubSuspiciouCodeStr(startPos, endPos);
//				
//				if (!suspExpStr.endsWith(".length")) {
//					SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
//					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
//							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
//					if (!allSuspVariables.contains(suspExpStr)) allSuspVariables.add(suspExpStr);
//				}
//				int index1 = suspExpStr.indexOf(".");
//				int index2 = suspExpStr.lastIndexOf(".");
//				if (index1 != index2) identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
//				int parentType = suspCodeAst.getType();
//				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
//						&& suspCodeAst.getChildPosition(child) == 0) {
//					continue;
//				}
//				String nameStr = child.getLabel(); // "this."/"super." + varName
//				if (!allSuspVariables.contains(nameStr)) allSuspVariables.add(nameStr);
//			} else if (Checker.isComplexExpression(childType)) {
//				identifySuspiciousVariables(ft, child, allSuspVariables, suspNullExpStrs);
//			} else if (Checker.isStatement(childType)) break;
//		}
//	}
	
	@Override
	public void fixProcess() {
		// FIXME: this is a test code snippet
		// dale test here
		SuspiciousPosition sc = new SuspiciousPosition();
		sc.classPath = "org.jfree.data.time.TimeSeries";
		sc.lineNumber = 1057;
		SuspCodeNode scn1 = parseSuspiciousCode(sc);
		ITree scan = scn1.suspCodeAstNode;
		// assignment
//		int type = scan.getType();
//		if (Checker.isAssignment(type) || Checker.isExpressionStatement(type)){
		for (ITree itree : scan.getChildren()){
			print("itree in scan: " + itree.toTreeString());
		}
//		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		ITree simpleName = null;//scp.findSimpleName(scan);	
		int suspExpStartPos = simpleName.getPos();
		int suspExpEndPos = suspExpStartPos + simpleName.getLength();
		
		FixTemplate ft = null;
		ft = new ExpressionReplacer();
		ft.setSuspiciousCodeStr(scn1.suspCodeStr);
		ft.setSuspiciousCodeTree(scn1.suspCodeAstNode);
		if (scn1.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn1.javaBackup);
		
		int suspCodeStartPos = scan.getPos();
		int suspCodeEndPos = suspCodeStartPos + scan.getLength();
		String suspCodePart1 = ft.getSubSuspiciouCodeStr(suspCodeStartPos, suspExpStartPos);
		String suspCodePart2 = ft.getSubSuspiciouCodeStr(suspExpEndPos,suspCodeEndPos);
		ft.generatePatch(suspCodePart1  + "TimeSeries" + suspCodePart2);
		
		ITree parent = scan.getParent();
		while( !Checker.isTypeDeclaration(parent.getType()) ){
			parent = parent.getParent();
		}
//		print(parent.toString());
		String[] clazzStrList = parent.toString().split(",");
		String superClazz = "null";
		String clazz = "null";
		for (String clazzStr : clazzStrList){
			if(clazzStr.contains("@@SuperClass:")){
				superClazz = clazzStr.split(":")[1];
			}
			if(clazzStr.contains("ClassName:")){
				clazz = clazzStr.split(":")[1];
			}
		}
		
		ContextReader.readAllVariablesAndFields(scn1.suspCodeAstNode, ft.allVarNamesMap, ft.varTypesMap, ft.allVarNamesList, ft.sourceCodePath, ft.dic);
		
		
		// Read paths of the buggy project.
		if (!dp.validPaths) return;
		
		// Read suspicious positions.
		List<SuspiciousPosition> suspiciousCodeList = readSuspiciousCodeFromFile(metric, buggyProject, dp);
		if (suspiciousCodeList == null) return;
		
		List<SuspCodeNode> triedSuspNode = new ArrayList<>();
		log.info("=======PARFIXER: Start to fix suspicious code======");
		for (SuspiciousPosition suspiciousCode : suspiciousCodeList) {		
			SuspCodeNode scn = parseSuspiciousCode(suspiciousCode);
			if (scn == null) continue;

//			log.debug(scn.suspCodeStr);
			if (triedSuspNode.contains(scn)) continue;
			triedSuspNode.add(scn);
	        // Match fix templates for this suspicious code with its context information.
	        fixWithMatchedFixTemplates(scn);
	        
			if (minErrorTest == 0) break;
        }
		log.info("=======PARFIXER: Finish off fixing======");
		
		FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + this.dataType + "/" + this.buggyProject);
	}
	
	protected void fixWithMatchedFixTemplates(SuspCodeNode scn) {
		
		// Parse context information of the suspicious code.
		List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
		List<Integer> distinctContextInfo = new ArrayList<>();
		for (Integer contInfo : contextInfoList) {
			if (!distinctContextInfo.contains(contInfo)) {
				distinctContextInfo.add(contInfo);
			}
		}
//		List<Integer> distinctContextInfo = contextInfoList.stream().distinct().collect(Collectors.toList());
		
		// generate patches with fix templates.
		FixTemplate ft = null;
		for (int contextInfo : distinctContextInfo) {
			if (Checker.isCastExpression(contextInfo)) {
				ft = new ClassCastChecker();
			} else if (Checker.isMethodInvocation(contextInfo) || Checker.isConstructorInvocation(contextInfo)
					|| Checker.isSuperConstructorInvocation(contextInfo) || Checker.isSuperMethodInvocation(contextInfo)
					|| Checker.isClassInstanceCreation(contextInfo)) {
//				// CollectionSizeChecker: method name must be "get".
				ft = new CollectionSizeChecker();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ParameterAdder();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ParameterRemover();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ParameterReplacer();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new MethodReplacer();
			} else if (Checker.isIfStatement(contextInfo) || Checker.isWhileStatement(contextInfo) || Checker.isDoStatement(contextInfo) 
					|| (Checker.isReturnStatement(contextInfo) && isBooleanReturnMethod(scn.suspCodeAstNode))) {
				ft = new ExpressionReplacer();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ExpressionRemover();
				generatePatches(ft, scn);
				if (this.minErrorTest == 0) break;
				
				ft = new ExpressionAdder();
			} else if (!Checker.withBlockStatement(scn.suspCodeAstNode.getType()) && Checker.isConditionalExpression(contextInfo)) {
				// TODO
			} else if (Checker.isSimpleName(contextInfo)) {
				ft = new NullPointerChecker();
//				generatePatches(ft, scn);
//				if (this.minErrorTest == 0) break;
//				
//				if (!distinctContextInfo.contains(25)) {// Do not re-initialize the variable(s) in IfStatement.
//					ft = new ObjectInitializer();
//				}
			} else if (Checker.isArrayAccess(contextInfo)) {
				ft = new RangeChecker();
//			} else if (Checker.isReturnStatement(contextInfo)) {
				// exchange true with false. TODO
			}
			if (ft == null) continue;
			generatePatches(ft, scn);
			if (this.minErrorTest == 0) break;
			ft = null;
		}
	}
	
	private boolean isBooleanReturnMethod(ITree suspCodeAstNode) {
		ITree parent = suspCodeAstNode.getParent();
		while (true) {
			if (parent == null) return false;
			if (Checker.isMethodDeclaration(parent.getType())) {
				break;
			}
			parent = parent.getParent();
		}
		
		String label = parent.getLabel();
		int indexOfMethodName = label.indexOf("MethodName:");

		// Read return type.
		String returnType = label.substring(label.indexOf("@@") + 2, indexOfMethodName - 2);
		int index = returnType.indexOf("@@tp:");
		if (index > 0) returnType = returnType.substring(0, index - 2);
		
		if ("boolean".equalsIgnoreCase(returnType))
			return true;
		
		return false;
	}

	private void generatePatches(FixTemplate ft, SuspCodeNode scn) {
		ft.setSuspiciousCodeStr(scn.suspCodeStr);
		ft.setSuspiciousCodeTree(scn.suspCodeAstNode);
		if (scn.javaBackup == null) ft.setSourceCodePath(dp.srcPath);
		else ft.setSourceCodePath(dp.srcPath, scn.javaBackup);
		ft.generatePatches();
		List<Patch> patchCandidates = ft.getPatches();
		
		// Test generated patches.
		if (patchCandidates.isEmpty()) return;
		testGeneratedPatches(patchCandidates, scn);
	}

	private List<Integer> readAllNodeTypes(ITree suspCodeAstNode) {
		List<Integer> nodeTypes = new ArrayList<>();
		nodeTypes.add(suspCodeAstNode.getType());
		List<ITree> children = suspCodeAstNode.getChildren();
		for (ITree child : children) {
			if (Checker.isStatement(child.getType())) break;
			nodeTypes.addAll(readAllNodeTypes(child));
		}
		return nodeTypes;
	}
	
	
	
	private void init() {
		// clear map
		this.patchLinesMap.clear();
		
		String logfile = "./match-log/" + Configuration.proj + '/' + Configuration.id + '/' ;
		File dir = new File(logfile);
		if (dir.exists() && dir.isDirectory()) {
			for (File file : dir.listFiles()){
				file.delete();
				print(file.getName() + " exists, and now clear it at the beginning of the process.");
			}
		}
		
		File file = new File(extraLog);
		if (file.exists()){
			file.delete();
			print(file.getName() + " exists, and now clear it at the beginning of the process.");
		}
	}

	/**
	 * use print() to replace System.out.println()
	 * @param str
	 */
	public static void print(String str){
		System.out.println(str);
	}
}

class ValueComparator implements Comparator<String> {
    Map<String, Triple<String, Integer, Integer>> base;

    public ValueComparator(Map<String, Triple<String, Integer, Integer>> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with
    // equals.
    public int compare(String a, String b) {
        if (base.get(a).getSecond() >= base.get(b).getSecond()) {
            return 1;
        } else {
            return -1;
        } // returning 0 would merge keys
    }
}