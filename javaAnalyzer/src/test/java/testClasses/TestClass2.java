package testClasses;

import java.util.ArrayList;
import java.util.Collections;

public class TestClass2 extends TestClass1 {

	static {
		Collections.sort(new ArrayList<String>(), (String s1, String s2) -> s1.compareTo(s2));
	}
}