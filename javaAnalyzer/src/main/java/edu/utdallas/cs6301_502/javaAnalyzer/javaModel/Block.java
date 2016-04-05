package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

/**
 * Used as a partent class for Methods and for Lambdas
 * @author rwiles
 *
 */
public class Block extends DependentBase {

	LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>();
	List<Block> childBlocks = new ArrayList<Block>();

	public Block(int depth, DependentBase parent) {
		this.parent = parent;
		AstVisitor.log(depth, "Creating Block: ");
	}

	public void addChildBlock(Block block) {
		this.childBlocks.add(block);
	}

	public void setParamMap(int depth, LinkedHashMap<String, String> paramMap) {
		assert (paramMap != null);

		this.paramMap = paramMap;

		for (String name : paramMap.keySet()) {
			AstVisitor.log(depth, "Adding Method Parameter: " + name);
			this.addVariable(depth, name, paramMap.get(name));
		}
	}

	@Override
	public void addUnresolvedClass(int depth, String className) {
		super.addUnresolvedClass(depth, className);
	}

	@Override
	public void validatePassOne(int depth) {
		if (this instanceof Method) {
			AstVisitor.log(depth, "Validating " + this.getClass().getName() + ": " + ((Method) this).getName());
		} else {
			AstVisitor.log(depth, "Validating " + this.getClass().getName() + ": ");
		}

		for (String varName : paramMap.keySet()) {
			for (String type : splitType(paramMap.get(varName))) {
				if (this instanceof Method) {
					AstVisitor.log(depth + 1, this.getClass().getName() + " " + ((Method) this).getName() + ": Searching for class for variable: " + type + " " + varName + "; isPrimative=" + isPrimative(type) + "; is \"this\"=" + "this".equals(type));
				} else {
					AstVisitor.log(depth + 1, this.getClass().getName() + ": Searching for class for variable: " + type + " " + varName + "; isPrimative=" + isPrimative(type) + "; is \"this\"=" + "this".equals(type));
				}

				if (!(isPrimative(type) || "this".equals(type))) {
					Class clazz = searchForUnresolvedClass(depth + 2, type);

					if (clazz != null) {
						AstVisitor.log(depth + 2, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
						if (!"this".equals(type))
							addResolvedClass(clazz);
						varNameClassMap.put(varName, clazz);
					}
				}
			}
		}
		super.validatePassOne(depth + 1);
	}
}