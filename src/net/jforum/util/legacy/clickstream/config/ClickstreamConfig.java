
package net.jforum.util.legacy.clickstream.config;

import java.util.ArrayList;
import java.util.List;

public class ClickstreamConfig {

	@SuppressWarnings("rawtypes")
	private List botAgents = new ArrayList();
	@SuppressWarnings("rawtypes")
	private List botHosts = new ArrayList();

	@SuppressWarnings("unchecked")
	public void addBotAgent(String agent) {
		botAgents.add(agent);
	}

	@SuppressWarnings("unchecked")
	public void addBotHost(String host) {
		botHosts.add(host);
	}

	@SuppressWarnings("rawtypes")
	public List getBotAgents() {
		return botAgents;
	}

	@SuppressWarnings("rawtypes")
	public List getBotHosts() {
		return botHosts;
	}
}
