# lombok-pg

lombok-pg is a collection of extensions to [lombok](http://projectlombok.org/index.html) [(source)](https://github.com/rzwitserloot/lombok) which further reduce boilerplate in Java. It is distributed together with lombok in one spicy package. lombok-pg is usually pretty much up to date with lombok, so you can enjoy all the funky stuff lombok offers ...and a bit more!

## Extensions to lombok found in this version:

#### Annotations:

- `@Action` [info](http://peichhorn.github.com/lombok-pg/Action.html)
- `@AutoGenMethodStub` [info](http://peichhorn.github.com/lombok-pg/AutoGenMethodStub.html)
- `@BoundPropertySupport` and `@BoundSetter` [info](http://peichhorn.github.com/lombok-pg/BoundProperties.html)
- `@Builder` and `@Builder.Extension` [info](http://peichhorn.github.com/lombok-pg/Builder.html)
- `@DoPrivileged` [info](http://peichhorn.github.com/lombok-pg/DoPrivileged.html)
- `@EnumId` [info](http://peichhorn.github.com/lombok-pg/EnumId.html)
- `@ExtensionMethod` [info](http://peichhorn.github.com/lombok-pg/ExtensionMethod.html)
- `@FluentSetter` [info](http://peichhorn.github.com/lombok-pg/FluentSetter.html)
- `@Function` [info](http://peichhorn.github.com/lombok-pg/Function.html)
- `@LazyGetter` [info](http://peichhorn.github.com/lombok-pg/LazyGetter.html)
- `@ListenerSupport` [info](http://peichhorn.github.com/lombok-pg/ListenerSupport.html)
- `@WriteLock` and `@ReadLock` [info](http://peichhorn.github.com/lombok-pg/Lock.html)
- `@Await`, `@Signal` and `@AwaitBeforeAndSignalAfter` [info](http://peichhorn.github.com/lombok-pg/Condition.html)
- `@Rethrow` and `@Rethrows` [info](http://peichhorn.github.com/lombok-pg/Rethrow.html)
- `@Sanitize.Normalize` and `@Sanitize.With` [info](http://peichhorn.github.com/lombok-pg/Sanitize.html)
- `@Singleton` [info](http://peichhorn.github.com/lombok-pg/Singleton.html)
- `@SwingInvokeLater` and `@SwingInvokeAndWait` [info] (http://peichhorn.github.com/lombok-pg/SwingInvoke.html)
- `@Validate.NotEmpty`, `@Validate.NotNull` and `@Validate.With` [info](http://peichhorn.github.com/lombok-pg/Validate.html)
- `@VisibleForTesting` [info](http://peichhorn.github.com/lombok-pg/VisibleForTesting.html)

#### Interfaces:

- `Application` and `JVMAgent` [info](http://peichhorn.github.com/lombok-pg/Entrypoint.html)

#### Methods:

- `tuple(expr1, expr2, ...)` [info](http://peichhorn.github.com/lombok-pg/Tuple.html)
- `yield(object)` [info](http://peichhorn.github.com/lombok-pg/Yield.html)


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


#### Base methods from lombok:

- `val` [info](http://projectlombok.org/features/val.html)

## Grab the latest version:

[Download page](https://github.com/peichhorn/lombok-pg/wiki/Grab-the-latest-version)

## Documentation:
- [Check out lombok features](http://projectlombok.org/features/)
- [Check out lombok-pg extensions](http://peichhorn.github.com/lombok-pg/)

## How-To prepare a lombok version that lombok-pg can run with:

- Clone lombok
- Merge the branch: [yield-friendly_@Cleanup](https://github.com/peichhorn/lombok/tree/yield-friendly_%40Cleanup)
- Merge the branch: [Issue_19_@Data_needs_a_callSuper](https://github.com/peichhorn/lombok/tree/Issue_19_%40Data_needs_a_callSuper)
