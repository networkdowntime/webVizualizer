import java.util.ArrayList;
import java.util.List;



public class Block extends DependentBase {
	
	List<Block> childBlocks = new ArrayList<Block>();
	
	public Block(Block parent) {
		this.parent = parent;
		
		DependentBase b = this.parent;
		while (b != null) {
			System.out.print("\t");
			b = b.parent;
		}
		Viewer.log(1, "Creating Block: ");
	}
	
	public void addChildBlock(Block block) {
		this.childBlocks.add(block);
	}
	
}
