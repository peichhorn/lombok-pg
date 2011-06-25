import java.util.Map;
import java.util.HashMap;

@lombok.Builder(prefix = "with") class BuilderPlain {
  public static @java.lang.SuppressWarnings("all") interface $OptionalDef {
    public $OptionalDef withOptionalVal1(final String optionalVal1);
    
    public $OptionalDef withOptionalVal2(final java.lang.Long arg0);
    
    public $OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0);
    
    public $OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1);
    
    public $OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0);
    
    public BuilderPlain build();
  }
  
  private static @java.lang.SuppressWarnings("all") class $Builder implements $OptionalDef {
    private String optionalVal1;
    private java.util.List<java.lang.Long> optionalVal2 = new java.util.ArrayList<java.lang.Long>();
    private Map<java.lang.String, java.lang.Long> optionalVal3 = new HashMap<java.lang.String, java.lang.Long>();
    
    public $OptionalDef withOptionalVal1(final String optionalVal1) {
      this.optionalVal1 = optionalVal1;
      return this;
    }
    
    public $OptionalDef withOptionalVal2(final java.lang.Long arg0) {
      this.optionalVal2.add(arg0);
      return this;
    }
    
    public $OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0) {
      this.optionalVal2.addAll(arg0);
      return this;
    }
    
    public $OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1) {
      this.optionalVal3.put(arg0, arg1);
      return this;
    }
    
    public $OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0) {
      this.optionalVal3.putAll(arg0);
      return this;
    }
    
    public BuilderPlain build() {
      return new BuilderPlain(this);
    }
    
    private $Builder() {
      super();
    }
  }
  
  public static final int IGNORE = 2;
  private String optionalVal1;
  private java.util.List<java.lang.Long> optionalVal2;
  private Map<java.lang.String, java.lang.Long> optionalVal3;
  
  <clinit>() {
  }
  
  private @java.lang.SuppressWarnings("all") BuilderPlain(final $Builder builder) {
    super();
    this.optionalVal1 = builder.optionalVal1;
    this.optionalVal2 = builder.optionalVal2;
    this.optionalVal3 = builder.optionalVal3;
  }
  
  public static @java.lang.SuppressWarnings("all") $OptionalDef builderPlain() {
    return new $Builder();
  }
}
