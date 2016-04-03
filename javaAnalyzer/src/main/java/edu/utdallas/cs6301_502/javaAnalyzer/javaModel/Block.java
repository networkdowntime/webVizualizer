package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.List;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;

public class Block extends DependentBase {

	List<Block> childBlocks = new ArrayList<Block>();

	public Block(int depth, Block parent) {
		this.parent = parent;
		AstVisitor.log(depth, "Creating Block: ");
	}

	public void addChildBlock(Block block) {
		this.childBlocks.add(block);
	}

	@Override
	public void addUnresolvedClass(int depth, String className) {
//		System.out.println("Adding class to block: " + className);
		super.addUnresolvedClass(depth, className);
	}

	
}