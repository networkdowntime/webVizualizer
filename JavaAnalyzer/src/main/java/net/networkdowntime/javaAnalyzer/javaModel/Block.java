package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.List;

import net.networkdowntime.javaAnalyzer.JavaAnalyzer;


public class Block extends DependentBase {

	List<Block> childBlocks = new ArrayList<Block>();

	public Block(Block parent) {
		this.parent = parent;

		DependentBase b = this.parent;
		while (b != null) {
			System.out.print("\t");
			b = b.parent;
		}
		JavaAnalyzer.log(1, "Creating Block: ");
	}

	public void addChildBlock(Block block) {
		this.childBlocks.add(block);
	}

}
