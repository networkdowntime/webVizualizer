package edu.utdallas.cs6301_502.javaAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Package;
import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Block;
import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Class;
import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.DependentBase;
import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Method;
import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Project;
import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.DiagramType;
import edu.utdallas.cs6301_502.javaAnalyzer.viewFilter.JavaFilter;
import japa.parser.ast.*;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.VoidType;
import japa.parser.ast.type.WildcardType;


// ToDo: Need to research and handle MemberValuePair
// ToDo: Need to research and handle BreakStmt
// ToDo: Need to research and handle EmptyStmt
// ToDo: Need to research and handle ContinueStmt
// ToDo: Glossing over the modifiers and name for TypeDefinitionStmt for now

public class JavaAnalyzer {

	private static final boolean LOG = true;

	public static void main(String[] args) {
		Project prj = new Project();

		long time = System.currentTimeMillis();
		
		prj.addFile(new File("src/test/java/testClasses"));
		
		System.out.println("Time to parse files (ms): " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		
		prj.validate();
		System.out.println("Time to validate (ms): " + (System.currentTimeMillis() - time));

		File graphFile = new File("graphFile.gv");
		try {
			FileWriter fw = new FileWriter(graphFile);
			JavaFilter filter = new JavaFilter();
			filter.setDiagramType(DiagramType.PACKAGE_DIAGRAM);
			filter.setFromFile(true);
			fw.write(prj.createGraph(filter));
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static String modifiersToString(int i) {
		String retval = "";
		retval += ModifierSet.isPublic(i) ? "public" : "";
		retval += ModifierSet.isProtected(i) ? "protected" : "";
		retval += ModifierSet.isPrivate(i) ? "private" : "";

		retval += (retval.isEmpty()) ? "" : " ";

		retval += ModifierSet.isStatic(i) ? "static" : "";

		return retval.trim();
	}

	private static void processBodyDeclarations(int depth, DependentBase base, List<BodyDeclaration> members) {
		if (members != null) {
			for (BodyDeclaration member : members) {
				processBodyDeclaration(depth, base, member);
			}
		}
	}

	private static void processExpressions(int depth, DependentBase base, List<Expression> expressions) {
		if (expressions != null) {
			for (Expression expression : expressions) {
				processExpression(depth, base, expression);
			}
		}
	}

	private static void processImports(int depth, Class clazz, List<ImportDeclaration> imports) {

		if (imports != null) {
			for (ImportDeclaration id : imports) {
				clazz.addImport(id.getName().toString());
			}
		}
	}

	private static void processMemberValuePairs(int depth, DependentBase base, List<MemberValuePair> pairs) {
		if (pairs != null) {
			for (MemberValuePair mvp : pairs) {
				processExpression(depth + 1, base, mvp.getValue());
			}
		}

	}

	private static void processStatements(int depth, Block method, List<Statement> statements) {
		if (statements != null) {
			for (Statement stmt : statements) {
				processStatement(depth, method, stmt);
			}
		}
	}

	public static void processTypeDeclarations(int depth, Project prj, Class base, CompilationUnit cu, List<TypeDeclaration> typeDeclarations) {
		if (typeDeclarations != null) {
			for (TypeDeclaration typeDeclaration : typeDeclarations) {
				processTypeDeclaration(depth, prj, base, cu, typeDeclaration);
			}
		}
	}

	private static void processTypeParameters(int depth, DependentBase base, List<TypeParameter> parameters) {
		if (parameters != null) {
			for (TypeParameter parameter : parameters) {
				processTypeParameter(depth, base, parameter);
			}
		}
	}

	private static void processTypes(int depth, DependentBase base, List<Type> typeArgs) {
		if (typeArgs != null) {
			for (Type type : typeArgs) {
				processType(depth, base, type);
			}
		}

	}

	private static void processVariableDeclarators(int depth, DependentBase base, List<VariableDeclarator> vars) {
		if (vars != null) {
			for (VariableDeclarator vd : vars) {
				processVariableDeclarator(depth, base, vd);
			}
		}
	}

	private static void processBodyDeclaration(int depth, DependentBase base, BodyDeclaration member) {
		if (member != null) {

			log(depth, "[" + member.getClass().getName() + "]");

			if (member instanceof AnnotationMemberDeclaration) {
				AnnotationMemberDeclaration field = (AnnotationMemberDeclaration) member;

				if (field.getAnnotations() != null) {
					processExpressions(depth + 1, base, new ArrayList<Expression>(field.getAnnotations()));
				}
				processType(depth + 1, base, field.getType());

				log(depth + 1, field.getName());

				processExpression(depth + 1, base, field.getDefaultValue());

			} else if (member instanceof ConstructorDeclaration) {
				ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) member;
				constructorDeclaration.getModifiers();

				String params = "";
				LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>();

				if (constructorDeclaration.getParameters() != null) {
					for (Parameter param : constructorDeclaration.getParameters()) {
						params += (params.isEmpty()) ? "" : ", ";
						params += param.getType();// + " " + param.getId().getName();

						paramMap.put(param.getId().getName(), param.getType().toString());
						// TODO: Add in method/class dependencies based on param Types
					}
				}

				Method method = ((Class) base).getOrCreateAndGetMethod(constructorDeclaration.getName() + "(" + params + ")");
				method.setParamMap(paramMap);

				if (constructorDeclaration.getBlock() != null) {
					processStatement(depth + 1, method, constructorDeclaration.getBlock());
				}
			} else if (member instanceof EmptyMemberDeclaration) {
				log(0, "!!! Not implemented EmptyMemberDeclaration");

			} else if (member instanceof EnumConstantDeclaration) {
				EnumConstantDeclaration field = (EnumConstantDeclaration) member;

				log(depth, field.getName());

				if (field.getAnnotations() != null) {
					processExpressions(depth + 1, base, new ArrayList<Expression>(field.getAnnotations()));
				}
				if (field.getArgs() != null) {
					processExpressions(depth + 1, base, field.getArgs());
				}
				if (field.getClassBody() != null) {
					processBodyDeclarations(depth + 1, base, field.getClassBody());
				}

			} else if (member instanceof FieldDeclaration) {
				FieldDeclaration field = (FieldDeclaration) member;

				log(depth, "[" + member.getClass().getName() + "]");

				String type = processType(depth + 1, base, field.getType());

				if (type != null) {
					// log(depth +1, "!!!FieldDeclaration " + type.toString());
					//
					// base.addUnresolvedClass(type);
					if (field.getVariables() != null) {
						for (VariableDeclarator vd : field.getVariables()) {
							base.addVariable(vd.getId().getName(), type);
						}
					}
				}

				for (VariableDeclarator variable : field.getVariables()) {
					processVariableDeclarator(depth + 1, base, variable);
				}

			} else if (member instanceof InitializerDeclaration) {
				log(0, "!!! Not implemented InitializerDeclaration");

			} else if (member instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration = (MethodDeclaration) member;
				methodDeclaration.getModifiers();

				String params = "";
				LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>();

				if (methodDeclaration.getParameters() != null) {
					for (Parameter param : methodDeclaration.getParameters()) {
						params += (params.isEmpty()) ? "" : ", ";
						params += param.getType();// + " " + param.getId().getName();

						paramMap.put(param.getId().getName(), param.getType().toString());
						// TODO: Add in method/class dependencies based on param Types
					}
				}

				Method method = ((Class) base).getOrCreateAndGetMethod(methodDeclaration.getName() + "(" + params + ")");
				method.setParamMap(paramMap);

				if (methodDeclaration.getBody() != null) {
					processStatement(depth + 1, method, methodDeclaration.getBody());
				}
			} else if (member instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) member;

				if (typeDeclaration.getAnnotations() != null) {
					processExpressions(depth + 1, base, new ArrayList<Expression>(typeDeclaration.getAnnotations()));
				}
				processBodyDeclarations(depth + 1, base, typeDeclaration.getMembers());
			}
		}
		log(depth, "leaving [" + member.getClass().getName() + "]");
	}

	private static void processCatchClause(int depth, Block block, List<CatchClause> catchs) {
		if (catchs != null) {
			for (CatchClause clause : catchs) {
				// glossing over the catch parameter for now
				processStatement(depth + 1, block, clause.getCatchBlock());
			}
		}

	}

	private static String processExpression(int depth, DependentBase base, Expression expression) {
		if (expression != null) {
			log(depth, "[" + expression.getClass().getName() + "] - " + expression.toString());

			System.out.println("[" + expression.getClass().getName() + "] - " + expression.toString());

			if (expression instanceof ArrayAccessExpr) {
				ArrayAccessExpr ex = ((ArrayAccessExpr) expression);

				processExpression(depth + 1, base, ex.getName());
				processExpression(depth + 1, base, ex.getIndex());

			} else if (expression instanceof ArrayCreationExpr) {
				ArrayCreationExpr ex = ((ArrayCreationExpr) expression);

				processExpressions(depth + 1, base, ex.getDimensions());
				processExpression(depth + 1, base, ex.getInitializer());

			} else if (expression instanceof ArrayInitializerExpr) {
				ArrayInitializerExpr ex = ((ArrayInitializerExpr) expression);

				processExpressions(depth + 1, base, ex.getValues());

			} else if (expression instanceof AssignExpr) {
				AssignExpr ex = ((AssignExpr) expression);

				processExpression(depth + 1, base, ex.getTarget());
				processExpression(depth + 1, base, ex.getValue());

			} else if (expression instanceof BinaryExpr) {
				BinaryExpr ex = ((BinaryExpr) expression);

				processExpression(depth + 1, base, ex.getLeft());
				processExpression(depth + 1, base, ex.getRight());

			} else if (expression instanceof BooleanLiteralExpr) {
				BooleanLiteralExpr ex = ((BooleanLiteralExpr) expression);

				// Nothing to do here

			} else if (expression instanceof CastExpr) {
				CastExpr ex = ((CastExpr) expression);

				// System.out.println("Type: " + ex.getType().toString());
				processExpression(depth + 1, base, ex.getExpr());

			} else if (expression instanceof CharLiteralExpr) {
				CharLiteralExpr ex = ((CharLiteralExpr) expression);

				// Nothing to do here

			} else if (expression instanceof ClassExpr) {
				ClassExpr ex = ((ClassExpr) expression);

				// System.out.println("Type: " + ex.getType().toString());

			} else if (expression instanceof ConditionalExpr) {
				ConditionalExpr ex = ((ConditionalExpr) expression);

				processExpression(depth + 1, base, ex.getCondition());
				processExpression(depth + 1, base, ex.getThenExpr());
				processExpression(depth + 1, base, ex.getElseExpr());

			} else if (expression instanceof DoubleLiteralExpr) {
				DoubleLiteralExpr ex = ((DoubleLiteralExpr) expression);

				// Nothing to do here

			} else if (expression instanceof EnclosedExpr) {
				EnclosedExpr ex = ((EnclosedExpr) expression);

				processExpression(depth + 1, base, ex.getInner());

			} else if (expression instanceof FieldAccessExpr) {
				FieldAccessExpr ex = ((FieldAccessExpr) expression);
				// System.out.println(ex.getField());
				// System.out.println(ex.getScope());

				// base.addUnresolvedClass(ex.getScope().toString());
				processExpression(depth + 1, base, ex.getScope());
				// System.out.println(ex.getField());

			} else if (expression instanceof InstanceOfExpr) {
				InstanceOfExpr ex = ((InstanceOfExpr) expression);

				// System.out.println(ex.getType());
				processExpression(depth + 1, base, ex.getExpr());

			} else if (expression instanceof IntegerLiteralExpr) {
				IntegerLiteralExpr ex = ((IntegerLiteralExpr) expression);

				// Nothing to do here

			} else if (expression instanceof IntegerLiteralMinValueExpr) {
				IntegerLiteralMinValueExpr ex = ((IntegerLiteralMinValueExpr) expression);

				// Nothing to do here

			} else if (expression instanceof MarkerAnnotationExpr) {
				MarkerAnnotationExpr ex = ((MarkerAnnotationExpr) expression);

				processExpression(depth + 1, base, ex.getName());

			} else if (expression instanceof MethodCallExpr) {
				MethodCallExpr ex = ((MethodCallExpr) expression);
				//System.out.println("method call scope: " + ex.getScope());
//				base.addUnresolvedClass(ex.getScope().toString()); // Name of the class
				String typeOrVarName = processExpression(depth + 1, base, ex.getScope());
				System.out.println("method call name: " + typeOrVarName + " -> " + ex.getName());

				base.addUnresolvedMethodCall(typeOrVarName, ex.getName());
				
				processExpressions(depth + 1, base, ex.getArgs());

			} else if (expression instanceof NameExpr) {
				NameExpr ex = ((NameExpr) expression);

				base.addPotentialClass(ex.toString());
				return ex.toString();
				// Nothing to do here

			} else if (expression instanceof NormalAnnotationExpr) {
				NormalAnnotationExpr ex = ((NormalAnnotationExpr) expression);

				base.addUnresolvedAnnotations(ex.getName().getName());

				processExpression(depth + 1, base, ex.getName());
				processMemberValuePairs(depth + 1, base, ex.getPairs());

			} else if (expression instanceof NullLiteralExpr) {
				NullLiteralExpr ex = ((NullLiteralExpr) expression);

				// Nothing to do here

			} else if (expression instanceof ObjectCreationExpr) {
				ObjectCreationExpr ex = ((ObjectCreationExpr) expression);

				processExpression(depth + 1, base, ex.getScope());
				processExpressions(depth + 1, base, ex.getArgs());
				processType(depth + 1, base, ex.getType());
				processTypes(depth + 1, base, ex.getTypeArgs());

			} else if (expression instanceof QualifiedNameExpr) {
				QualifiedNameExpr ex = ((QualifiedNameExpr) expression);

				processExpression(depth + 1, base, ex.getQualifier());

			} else if (expression instanceof SingleMemberAnnotationExpr) {
				SingleMemberAnnotationExpr ex = ((SingleMemberAnnotationExpr) expression);

				processExpression(depth + 1, base, ex.getName());
				processExpression(depth + 1, base, ex.getMemberValue());

			} else if (expression instanceof StringLiteralExpr) {
				StringLiteralExpr ex = ((StringLiteralExpr) expression);

				base.addUnresolvedClass("String");
				return "String";

			} else if (expression instanceof SuperExpr) {
				SuperExpr ex = ((SuperExpr) expression);

				processExpression(depth + 1, base, ex.getClassExpr());
				return "super";

			} else if (expression instanceof ThisExpr) {
				ThisExpr ex = ((ThisExpr) expression);

				processExpression(depth + 1, base, ex.getClassExpr());
				return base.findClass().getName();

			} else if (expression instanceof UnaryExpr) {
				UnaryExpr ex = ((UnaryExpr) expression);

				processExpression(depth + 1, base, ex.getExpr());

			} else if (expression instanceof VariableDeclarationExpr) {
				VariableDeclarationExpr ex = ((VariableDeclarationExpr) expression);

				// System.out.println(ex.getType());

				String type = processType(depth + 1, base, ex.getType());

				if (type != null) {
					log(depth + 1, "!!!VariableDeclarationExpr " + ex.getType().toString());
					base.addUnresolvedClass(ex.getType().toString());
					if (ex.getVars() != null) {
						for (VariableDeclarator vd : ex.getVars()) {
							base.addVariable(vd.getId().getName(), type);
						}
					}
				}

				processVariableDeclarators(depth + 1, base, ex.getVars());

			} else {
				log(0, "!!! Unknown - [" + expression.getClass().getName() + "] - " + expression.toString());
			}
		}
		return null;
	}

	private static void processStatement(int depth, Block block, Statement stmt) {
		if (stmt != null) {
			log(depth, stmt.getClass().getName());
			if (stmt instanceof AssertStmt) {
				processExpression(depth + 1, block, ((AssertStmt) stmt).getCheck());

			} else if (stmt instanceof BlockStmt) {
				Block childBlock = new Block(block);
				block.addChildBlock(childBlock);
				processStatements(depth + 1, childBlock, ((BlockStmt) stmt).getStmts());

			} else if (stmt instanceof BreakStmt) {
				// skipping

			} else if (stmt instanceof ContinueStmt) {
				// skipping

			} else if (stmt instanceof DoStmt) {
				processExpression(depth + 1, block, ((DoStmt) stmt).getCondition());
				processStatement(depth + 1, block, ((DoStmt) stmt).getBody());

			} else if (stmt instanceof EmptyStmt) {
				// skipping

			} else if (stmt instanceof ExplicitConstructorInvocationStmt) {
				processExpression(depth + 1, block, ((ExplicitConstructorInvocationStmt) stmt).getExpr());

			} else if (stmt instanceof ExpressionStmt) {
				processExpression(depth + 1, block, ((ExpressionStmt) stmt).getExpression());

			} else if (stmt instanceof ForeachStmt) {
				processExpression(depth + 1, block, ((ForeachStmt) stmt).getIterable());
				processStatement(depth + 1, block, ((ForeachStmt) stmt).getBody());

			} else if (stmt instanceof ForStmt) {
				processExpressions(depth + 1, block, ((ForStmt) stmt).getInit());
				processExpression(depth + 1, block, ((ForStmt) stmt).getCompare());
				processExpressions(depth + 1, block, ((ForStmt) stmt).getUpdate());
				processStatement(depth + 1, block, ((ForStmt) stmt).getBody());

			} else if (stmt instanceof IfStmt) {
				processExpression(depth + 1, block, ((IfStmt) stmt).getCondition());
				processStatement(depth + 1, block, ((IfStmt) stmt).getThenStmt());
				processStatement(depth + 1, block, ((IfStmt) stmt).getElseStmt());

			} else if (stmt instanceof LabeledStmt) {
				processStatement(depth + 1, block, ((LabeledStmt) stmt).getStmt());

			} else if (stmt instanceof ReturnStmt) {
				processExpression(depth + 1, block, ((ReturnStmt) stmt).getExpr());

			} else if (stmt instanceof SwitchEntryStmt) {
				processStatements(depth + 1, block, ((SwitchEntryStmt) stmt).getStmts());

			} else if (stmt instanceof SwitchStmt) {
				processExpression(depth + 1, block, ((SwitchStmt) stmt).getSelector());
				processStatements(depth + 1, block, new ArrayList<Statement>(((SwitchStmt) stmt).getEntries()));

			} else if (stmt instanceof SynchronizedStmt) {
				processExpression(depth + 1, block, ((SynchronizedStmt) stmt).getExpr());
				processStatement(depth + 1, block, ((SynchronizedStmt) stmt).getBlock());

			} else if (stmt instanceof ThrowStmt) {
				processExpression(depth + 1, block, ((ThrowStmt) stmt).getExpr());

			} else if (stmt instanceof TryStmt) {

				Block childTryBlock = new Block(block);
				block.addChildBlock(childTryBlock);
				processStatement(depth + 1, childTryBlock, ((TryStmt) stmt).getTryBlock());

				Block childCatchBlock = new Block(block);
				block.addChildBlock(childCatchBlock);
				processCatchClause(depth + 1, childCatchBlock, ((TryStmt) stmt).getCatchs());

				Block childFinallyBlock = new Block(block);
				block.addChildBlock(childFinallyBlock);
				processStatement(depth + 1, childFinallyBlock, ((TryStmt) stmt).getFinallyBlock());

			} else if (stmt instanceof TypeDeclarationStmt) {
				// glossing over the modifiers and name for now
				processBodyDeclarations(depth + 1, block.getParent(), ((TypeDeclarationStmt) stmt).getTypeDeclaration().getMembers());

			} else if (stmt instanceof WhileStmt) {
				processExpression(depth + 1, block, ((WhileStmt) stmt).getCondition());
				processStatement(depth + 1, block, ((WhileStmt) stmt).getBody());

			}
		}
	}

	private static String processType(int depth, DependentBase base, Type type) {
		if (type != null) {
			log(depth, "[" + type.getClass() + "] - " + type.toString());
			if (type instanceof ClassOrInterfaceType) {
				ClassOrInterfaceType t = (ClassOrInterfaceType) type;

				String typeStr = type.toString();
				if (typeStr.contains("<")) { // Generics

					String genericizedClass = typeStr.substring(0, typeStr.indexOf("<"));
					log(depth + 1, "Generic Type - " + genericizedClass);
					base.addUnresolvedClass(genericizedClass);

				} else {
					base.addUnresolvedClass(typeStr);
				}

				processType(depth + 1, base, t.getScope());
				processTypes(depth + 1, base, t.getTypeArgs());

				return type.toString();

			} else if (type instanceof PrimitiveType) {
				// Nothing to do here
				return type.toString();

			} else if (type instanceof ReferenceType) {
				ReferenceType t = (ReferenceType) type;

				return processType(depth + 1, base, t.getType());

			} else if (type instanceof VoidType) {
				VoidType t = (VoidType) type;

				// Nothing to do here
				return null;

			} else if (type instanceof WildcardType) {
				WildcardType t = (WildcardType) type;

				processType(depth + 1, base, t.getSuper());
				processType(depth + 1, base, t.getExtends());

				return null;

			} else {
				log(0, "!!![" + type.getClass() + "] - " + type.toString());
			}
		}
		return "";
	}

	private static void processTypeDeclaration(int depth, Project prj, Class parent, CompilationUnit cu, TypeDeclaration typeDeclaration) {
		if (typeDeclaration != null) {
			log(depth, "[" + typeDeclaration.getClass().getName() + "] - " + typeDeclaration.getName());

			Package pkg = prj.getOrCreateAndGetPackage(cu.getPackage().getName().toString(), true, true);

			Class base;
			if (parent != null) {
				base = pkg.getOrCreateAndGetClass(parent.getName() + "." + typeDeclaration.getName(), true);
			} else {
				base = pkg.getOrCreateAndGetClass(typeDeclaration.getName(), true);
			}

			log(depth, cu.getPackage().getName().toString());

			if (cu.getImports() != null) {
				processImports(0, base, cu.getImports());
			}

			if (typeDeclaration instanceof AnnotationDeclaration) {
				AnnotationDeclaration decl = (AnnotationDeclaration) typeDeclaration;

				log(depth, modifiersToString(decl.getModifiers()) + " annotation " + decl.getName());

				base.setIsAnnotation(true);

				if (decl.getAnnotations() != null) {
					processExpressions(depth + 1, base, new ArrayList<Expression>(decl.getAnnotations()));
				}
				processBodyDeclarations(depth + 1, base, decl.getMembers());

			} else if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) typeDeclaration;

				String classOrInterface = "class";
				if (decl.isInterface()) {
					classOrInterface = "interface";
				}

				base.setIsInterface(decl.isInterface());
				base.setIsAbstract(ModifierSet.isAbstract(decl.getModifiers()));

				log(depth, cu.getPackage().getName().toString());
				log(depth, modifiersToString(decl.getModifiers()) + " " + classOrInterface + " " + decl.getName());

				if (decl.getAnnotations() != null) {
					processExpressions(depth + 1, base, new ArrayList<Expression>(decl.getAnnotations()));
				}
				if (decl.getExtends() != null) {
					for (ClassOrInterfaceType type : decl.getExtends()) {
						base.setExtendsStr(type.getName());
						base.addUnresolvedClass(type.getName());
					}

					processTypes(depth + 1, base, new ArrayList<Type>(decl.getExtends()));
				}
				if (decl.getImplements() != null) {
					processTypes(depth + 1, base, new ArrayList<Type>(decl.getImplements()));
				}
				if (decl.getMembers() != null) {
					for (BodyDeclaration bd : decl.getMembers()) {
						if (bd instanceof TypeDeclaration) {
							processTypeDeclaration(depth + 1, prj, base, cu, (TypeDeclaration) bd);
						} else {
							processBodyDeclaration(depth + 1, base, bd);
						}
					}
				}
				if (decl.getTypeParameters() != null) {
					processTypeParameters(depth + 1, base, decl.getTypeParameters());
				}

			} else if (typeDeclaration instanceof EmptyTypeDeclaration) {
				EmptyTypeDeclaration decl = (EmptyTypeDeclaration) typeDeclaration;

				// I don't think there is anything to do here.

			} else if (typeDeclaration instanceof EnumDeclaration) {
				EnumDeclaration decl = (EnumDeclaration) typeDeclaration;

				base.setIsEnum(true);

				if (decl.getAnnotations() != null) {
					processExpressions(depth + 1, base, new ArrayList<Expression>(decl.getAnnotations()));
				}
				if (decl.getEntries() != null) {
					processBodyDeclarations(depth + 1, base, new ArrayList<BodyDeclaration>(decl.getEntries()));
				}
				if (decl.getImplements() != null) {
					processTypes(depth + 1, base, new ArrayList<Type>(decl.getImplements()));
				}
				if (decl.getMembers() != null) {
					processBodyDeclarations(depth + 1, base, decl.getMembers());
				}

			}
		}
	}

	private static void processTypeParameter(int depth, DependentBase base, TypeParameter parameter) {
		if (parameter != null) {
			processTypes(depth + 1, base, new ArrayList<Type>(parameter.getTypeBound()));
		}
	}

	private static void processVariableDeclarator(int depth, DependentBase base, VariableDeclarator variableDeclator) {
		if (variableDeclator != null) {
			log(depth, "[" + variableDeclator.getClass() + "] - " + variableDeclator.getId().getName());
			processExpression(depth + 1, base, variableDeclator.getInit());
		}
	}

	public static void log(int depth, String str) {
		if (LOG) {
			for (int i = 0; i < depth; i++) {
				System.out.print("\t");
			}
			System.out.println(str);
		}
	}
}
