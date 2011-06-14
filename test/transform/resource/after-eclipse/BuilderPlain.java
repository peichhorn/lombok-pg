import java.util.Map;
import java.util.HashMap;

@lombok.Builder class BuilderPlain0 {
  private final String text;
  private final int id;
  
  BuilderPlain0() {
    super();
  }
  
  public void builderPlain0() {
  }
}
@lombok.Builder(exclude = {"optionalVal3"},convenientMethods = false) class BuilderPlain1 {
  public static @java.lang.SuppressWarnings("all") interface $TextDef {
    public $IdDef text(final String text);
    
    public $OptionalDef idAndText(final String id, final String text);
  }
  
  public static @java.lang.SuppressWarnings("all") interface $IdDef {
    public $OptionalDef id(final int id);
  }
  
  public static @java.lang.SuppressWarnings("all") interface $OptionalDef {
    public $OptionalDef optionalVal1(final String optionalVal1);
    
    public $OptionalDef optionalVal2(final java.util.List<java.lang.Long> optionalVal2);
    
    public BuilderPlain1 build();
    
    public $OptionalDef optionalVal1(final Class<?> clazz);
  }
  
  private static @java.lang.SuppressWarnings("all") class $Builder implements $TextDef, $IdDef, $OptionalDef {
    private String text;
    private int id;
    private String optionalVal1 = "default";
    private java.util.List<java.lang.Long> optionalVal2;
    
    public $IdDef text(final String text) {
      this.text = text;
      return this;
    }
    
    public @lombok.Builder.Extension $OptionalDef idAndText(final String id, final String text) {
      this.id = java.lang.Integer.valueOf(id);
      this.text = text;
      return this;
    }
    
    public $OptionalDef id(final int id) {
      this.id = id;
      return this;
    }
    
    public $OptionalDef optionalVal1(final String optionalVal1) {
      this.optionalVal1 = optionalVal1;
      return this;
    }
    
    public $OptionalDef optionalVal2(final java.util.List<java.lang.Long> optionalVal2) {
      this.optionalVal2 = optionalVal2;
      return this;
    }
    
    public BuilderPlain1 build() {
      return new BuilderPlain1(this);
    }
    
    public @lombok.Builder.Extension $OptionalDef optionalVal1(final Class<?> clazz) {
      this.optionalVal1 = clazz.getSimpleName();
      return this;
    }
    
    private $Builder() {
      super();
    }
  }
  private final String text;
  private final int id;
  private String optionalVal1;
  private java.util.List<java.lang.Long> optionalVal2;
  private long optionalVal3;
  
  private @java.lang.SuppressWarnings("all") BuilderPlain1(final $Builder builder) {
    super();
    this.text = builder.text;
    this.id = builder.id;
    this.optionalVal1 = builder.optionalVal1;
    this.optionalVal2 = builder.optionalVal2;
  }
  
  public static @java.lang.SuppressWarnings("all") $TextDef builderPlain1() {
    return new $Builder();
  }
  
  private @lombok.Builder.Extension void brokenExtension() {
    this.id = 42;
  }
}
@lombok.Builder(prefix = "with") class BuilderPlain2 {
  public static @java.lang.SuppressWarnings("all") interface $OptionalDef {
    public $OptionalDef withOptionalVal1(final String optionalVal1);
    
    public $OptionalDef withOptionalVal2(final java.lang.Long arg0);
    
    public $OptionalDef withOptionalVal2(final java.util.Collection<? extends java.lang.Long> arg0);
    
    public $OptionalDef withOptionalVal3(final java.lang.String arg0, final java.lang.Long arg1);
    
    public $OptionalDef withOptionalVal3(final java.util.Map<? extends java.lang.String, ? extends java.lang.Long> arg0);
    
    public BuilderPlain2 build();
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
    
    public BuilderPlain2 build() {
      return new BuilderPlain2(this);
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
  private @java.lang.SuppressWarnings("all") BuilderPlain2(final $Builder builder) {
    super();
    this.optionalVal1 = builder.optionalVal1;
    this.optionalVal2 = builder.optionalVal2;
    this.optionalVal3 = builder.optionalVal3;
  }
  
  public static @java.lang.SuppressWarnings("all") $OptionalDef builderPlain2() {
    return new $Builder();
  }
}
@lombok.Builder(value = lombok.AccessLevel.PACKAGE,callMethods = {"toString", "bar"}) class BuilderPlain3 {
  private static class Test {
    private String ignoreInnerClasses;
    private Test() {
      super();
    }
  }
  public static @java.lang.SuppressWarnings("all") interface $TextDef {
    public $IdDef text(final String text);
  }
  
  public static @java.lang.SuppressWarnings("all") interface $IdDef {
    public $OptionalDef id(final int id);
  }
  
  public static @java.lang.SuppressWarnings("all") interface $OptionalDef {
    public BuilderPlain3 build();
    
    public java.lang.String toString();
    
    public void bar() throws Exception;
  }
  
  private static @java.lang.SuppressWarnings("all") class $Builder implements $TextDef, $IdDef, $OptionalDef {
    private String text;
    private int id;
    
    public $IdDef text(final String text) {
      this.text = text;
      return this;
    }
    
    public $OptionalDef id(final int id) {
      this.id = id;
      return this;
    }
    
    public BuilderPlain3 build() {
      return new BuilderPlain3(this);
    }
    
    public java.lang.String toString() {
      return this.build().toString();
    }
    
    public void bar() throws Exception {
      this.build().bar();
    }
    
    private $Builder() {
      super();
    }
  }
  private final String text;
  private final int id;
  
  private @java.lang.SuppressWarnings("all") BuilderPlain3(final $Builder builder) {
    super();
    this.text = builder.text;
    this.id = builder.id;
  }
  
  static @java.lang.SuppressWarnings("all") $TextDef builderPlain3() {
    return new $Builder();
  }
  
  private void bar() throws Exception {
  }
}