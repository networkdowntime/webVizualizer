package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Used as a partent class for Methods and for Lambdas
 * @author rwiles
 *
 */
public class Block extends DependentBase {

	Map<String, String> paramMap = new LinkedHashMap<String, String>();
	List<Block> childBlocks = new ArrayList<Block>();

	public Block(int depth, DependentBase parent) {
		this.parent = parent;
		logIndented(depth, "Creating Block: ");
	}

	public void accept(ModelVisitor visitor) {
		visitor.visit(this);
	}
	
	public void addChildBlock(Block block) {
		this.childBlocks.add(block);
	}

	public void setParamMap(int depth, Map<String, String> paramMap) {
		assert (paramMap != null);

		this.paramMap = paramMap;

		for (String name : paramMap.keySet()) {
			logIndented(depth, "Adding Method Parameter: " + name);
			this.addVariable(depth, name, paramMap.get(name));
		}
	}

	@Override
	public void addUnresolvedClass(int depth, String className) {
		super.addUnresolvedClass(depth, className);
	}

	@Override
	public void validatePassOne(int depth) {
		logIndented(depth, "Validating " + this.getClass().getName() + ": ");

		for (String varName : paramMap.keySet()) {
			for (String type : splitType(paramMap.get(varName))) {
				logIndented(depth + 1, this.getClass().getName() + ": Searching for class for variable: " + type + " " + varName + "; isPrimative=" + isPrimative(type) + "; is \"this\"=" + "this".equals(type));

				if (!(isPrimative(type) || "this".equals(type))) {
					Class clazz = searchForUnresolvedClass(depth + 2, type);

					if (clazz != null) {
						logIndented(depth + 2, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
						if (!"this".equals(type))
							addResolvedClass(clazz);
						getVarNameClassMap().put(varName, clazz);
					}
				}
			}
		}
		super.validatePassOne(depth + 1);
	}
}