# lombok-pg

lombok-pg is a collection of extensions to [lombok](https://github.com/rzwitserloot/lombok). It is distributed together with lombok in one spicy package. lombok-pg is usually pretty much up to date with lombok, so you can enjoy all the funky stuff lombok offers ...and a bit more!

## Extensions to lombok found in this version:

#### Annotations:

- `@AutoGenMethodStub` [info](http://peichhorn.github.com/lombok-pg/AutoGenMethodStub.html)
- `@BoundPropertySupport` and `@BoundSetter` 
- `@Builder` and `@Builder.Extension` [info](http://peichhorn.github.com/lombok-pg/Builder.html)
- `@DoPrivileged` [info](http://peichhorn.github.com/lombok-pg/DoPrivileged.html)
- `@EnumId` [info](http://peichhorn.github.com/lombok-pg/EnumId.html)
- `@ExtensionMethod` [info](http://peichhorn.github.com/lombok-pg/ExtensionMethod.html)
- `@FluentSetter` [info](http://peichhorn.github.com/lombok-pg/FluentSetter.html)
- `@Function` (documentation coming soon...)
- `@LazyGetter` 
- `@ListenerSupport` [info](http://peichhorn.github.com/lombok-pg/ListenerSupport.html)
- `@WriteLock` and `@ReadLock` [info](http://peichhorn.github.com/lombok-pg/Lock.html)
- `@Await`, `@Signal` and `@AwaitBeforeAndSignalAfter` [info](http://peichhorn.github.com/lombok-pg/Condition.html)
- `@Rethrow` and `@Rethrows` [info](http://peichhorn.github.com/lombok-pg/Rethrow.html)
- `@Sanitize.Normalize` and `@Sanitize.With`
- `@Singleton` [info](http://peichhorn.github.com/lombok-pg/Singleton.html)
- `@SwingInvokeLater` and `@SwingInvokeAndWait` [info] (http://peichhorn.github.com/lombok-pg/SwingInvoke.html)
- `@Validate.NotEmpty`, `@Validate.NotNull` and `@Validate.With` (documentation coming soon...)
- `@VisibleForTesting` [info](http://peichhorn.github.com/lombok-pg/VisibleForTesting.html)

#### Interfaces:

- `Application` and `JVMAgent` [doc](http://peichhorn.github.com/lombok-pg/Entrypoint.html)

#### Methods:

- `tuple(expr1, expr2, ...)` [doc](http://peichhorn.github.com/lombok-pg/Tuple.html)
- `with(object, expr1, expr2, ...)` [doc](http://peichhorn.github.com/lombok-pg/With.html)
- `yield(object)` [doc](http://peichhorn.github.com/lombok-pg/Yield.html)


#### Base annotations from lombok:

- `@AllArgsConstructor, @RequiredArgsConstructor and @NoArgsConstructor` [info](http://projectlombok.org/features/Constructor.html)
- `@Cleanup` [info](http://projectlombok.org/features/Cleanup.html)
- `@Delegate` [info](http://projectlombok.org/features/Delegate.html)
- `@EqualsAndHashcode` [info](http://projectlombok.org/features/EqualsAndHashCode.html)
- `@Getter/Setter` [info](http://projectlombok.org/features/GetterSetter.html)
- `@Getter(lazy=true)` [info](http://projectlombok.org/features/GetterLazy.html)
- `@Log` [info](http://projectlombok.org/features/Log.html)
- `@SneakyThrows` [info](http://projectlombok.org/features/SneakyThrows.html)
- `@Synchronized` [info](http://projectlombok.org/features/Synchronized.html)
- `@ToString` [info](http://projectlombok.org/features/ToString.html)
- `val` [info](http://projectlombok.org/features/val.html)

## Grab the latest version:
[lombok-pg 0.10.0 (based on lombok 0.10.0)](http://cloud.github.com/downloads/peichhorn/lombok-pg/lombok-pg-0.10.0.jar)

## Documentation:
[Check out lombok features](http://projectlombok.org/features/)
[Check out lombok-pg extensions](http://peichhorn.github.com/lombok-pg/)

