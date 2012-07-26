package test;
import lombok.Warning;
public class WarningPlain {
  public WarningPlain() {
    super();
  }
  public @Warning("See upstream issue #HHH-7476 for details") void read() {
  }
}