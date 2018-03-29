
package net.jforum;

public class UrlPattern {

	private String name   = null;
	private String value  = null;
	private String[] vars = null;
	private int size      = 0;

	public UrlPattern(String name, String value) {
		this.name = name;
		this.value = value;
		processPattern();
	}

	private void processPattern() {
		String[] p = value.split(",");
		vars = new String[p.length];
		size = p[0].trim().equals("") ? 0 : p.length;
		for (int i = 0; i < size; i++) {
			vars[i] = p[i].trim();
		}
	}

	public String getName() {
		return name;
	}

	public int getSize() {
		return size;
	}

	public String[] getVars() {
		return vars;
	}
}