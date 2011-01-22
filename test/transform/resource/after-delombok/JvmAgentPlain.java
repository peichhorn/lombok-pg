import java.lang.instrument.Instrumentation;

class JvmAgentPlain {
	JvmAgentPlain() {
		super();
	}
	@Override
	public void runAgent(boolean injected, String params, Instrumentation instrumentation) throws Throwable {
	}
	@java.lang.SuppressWarnings("all")
	public static void agentmain(final java.lang.String params, final java.lang.instrument.Instrumentation instrumentation) throws java.lang.Throwable {
		new JvmAgentPlain().runAgent(true, params, instrumentation);
	}
	@java.lang.SuppressWarnings("all")
	public static void premain(final java.lang.String params, final java.lang.instrument.Instrumentation instrumentation) throws java.lang.Throwable {
		new JvmAgentPlain().runAgent(false, params, instrumentation);
	}
}
