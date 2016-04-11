package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.List;

import edu.utdallas.cs6301_502.javaAnalyzer.JavaAnalyzer;


public class Block extends DependentBase {

	List<Block> childBlocks = new ArrayList<Block>();

	public Block(Block parent) {
		this.parent = parent;
		JavaAnalyzer.log(1, "Creating Block: ");
	}

	public void addChildBlock(Block block) {
		this.childBlocks.add(block);
	}

	@Override
	public void addUnresolvedClass(String className) {
		System.out.println("Adding class to block: " + className);
		super.addUnresolvedClass(className);
	}

	
}
