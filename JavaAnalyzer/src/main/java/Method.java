import java.util.HashMap;
import java.util.LinkedHashMap;


public class Method extends Block {

	String name;
	LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>();

	public Method(Class clazz, String name) {
		super(null);
		this.parent = clazz;
		this.name = name;
		Viewer.log(3, "Creating Method: " + clazz.pkg.getName() + "." + clazz.name + "." + name);
	}

	public String getName() {
		return this.name;
	}

	public void setParamMap(LinkedHashMap<String, String> paramMap) {
		assert (paramMap != null);

		this.paramMap = paramMap;

		for (String name : paramMap.keySet()) {
			this.addVariable(name, paramMap.get(name));
		}
	}

	public void validate() {
		Viewer.log(1, "Validating Method: " + getName());
		super.validate();
	}
}
