import lombok.JvmAgent;
import java.lang.instrument.Instrumentation;
class JvmAgentPlain implements JvmAgent {
  JvmAgentPlain() {
    super();
  }
  public void runAgent(boolean injected, String params, Instrumentation instrumentation) throws Throwable {
  }
  public static @java.lang.SuppressWarnings("all") void agentmain(final java.lang.String params, final java.lang.instrument.Instrumentation instrumentation) throws java.lang.Throwable {
    new JvmAgentPlain().runAgent(true, params, instrumentation);
  }
  public static @java.lang.SuppressWarnings("all") void premain(final java.lang.String params, final java.lang.instrument.Instrumentation instrumentation) throws java.lang.Throwable {
    new JvmAgentPlain().runAgent(false, params, instrumentation);
  }
}
