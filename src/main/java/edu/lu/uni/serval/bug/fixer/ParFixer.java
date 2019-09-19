package edu.lu.uni.serval.bug.fixer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.jdt.tree.ITree;
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
import edu.lu.uni.serval.parser.JavaFileParser;
import edu.lu.uni.serval.parser.JavaFileParser.MyUnit;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.utils.Checker;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.SuspiciousPosition;
import edu.lu.uni.serval.utils.SuspiciousCodeParser;

/**
 * Automated Program Repair Tool: PAR.
 * 
 * All fix templates are introduced in paper "Automatic patch generation learned from human-written patches".
 * https://dl.acm.org/citation.cfm?id=2486893
 * 
 * @author kui.liu
 *
 */
public class ParFixer extends AbstractFixer {
	
	private static Logger log = LoggerFactory.getLogger(ParFixer.class);
	
	public ParFixer(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	public ParFixer(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}

	public void print(String str){
		System.out.println(str);
	}
	
	public void matchProcess() {
		// FIXME: this is a test code snippet
		// dale test here
		
		// a specified line
		SuspiciousPosition sc = new SuspiciousPosition();
		sc.classPath = "org.jfree.data.time.TimeSeries";
		sc.lineNumber = 1057;
		
		// get the AST node for the line
		SuspCodeNode scn1 = parseSuspiciousCode(sc);
		ITree scan = scn1.suspCodeAstNode;
		// assignment
//		int type = scan.getType();
//		if (Checker.isAssignment(type) || Checker.isExpressionStatement(type)){
		for (ITree itree : scan.getChildren()){
			print("itree in scan: " + itree.toTreeString());
		}
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		ITree simpleName = scp.findSimpleName(scan);	
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
		// 
		List<String> vars = new ArrayList<>();
		List<ITree> suspVars = new ArrayList<>();
		ContextReader.identifySuspiciousVariables(scan, suspVars, new ArrayList<String>());
		ContextReader.readAllVariablesAndFields(scan, ft.allVarNamesMap, ft.varTypesMap, ft.allVarNamesList, ft.sourceCodePath, ft.dic);
		
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
		int type = scan.getType();
//		if (Checker.isAssignment(type) || Checker.isExpressionStatement(type)){
		for (ITree itree : scan.getChildren()){
			print("itree in scan: " + itree.toTreeString());
		}
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		ITree simpleName = scp.findSimpleName(scan);	
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

}
