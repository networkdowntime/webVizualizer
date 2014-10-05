import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class JSTest {

	public static void main(String[] args) {

		System.out.println("JS Test");

		File f = new File("src/main/webapp/js/viz.js");

		System.out.println(f.exists());

		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

		try {
			engine.eval("print(Math.random());");
			engine.eval(new FileReader(f));

		} catch (ScriptException | FileNotFoundException e) {
			e.printStackTrace();
		}

	}

}
