# lombok-pg

lombok-pg is a collection of extensions to [lombok](http://projectlombok.org/index.html) [(source)](https://github.com/rzwitserloot/lombok) which further reduce boilerplate in Java. It is distributed together with lombok in one spicy package. lombok-pg is usually pretty much up to date with lombok, so you can enjoy all the funky stuff lombok offers ...and a bit more!

[![Build Status](https://secure.travis-ci.org/peichhorn/lombok-pg.png?branch=master)](http://travis-ci.org/peichhorn/lombok-pg)

## Extensions to lombok found in this version:

#### Annotations:

- `@Action` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Action)
- `@AutoGenMethodStub` [info](https://github.com/peichhorn/lombok-pg/wiki/%40AutoGenMethodStub)
- `@BoundPropertySupport` and `@BoundSetter` [info](https://github.com/peichhorn/lombok-pg/wiki/%40BoundPropertySupport-%40BoundSetter)
- `@Builder` and `@Builder.Extension` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Builder-%40Builder.Extension)
- `@DoPrivileged` [info](https://github.com/peichhorn/lombok-pg/wiki/%40DoPrivileged)
- `@EnumId` [info](https://github.com/peichhorn/lombok-pg/wiki/%40EnumId)
- `@ExtensionMethod` [info](https://github.com/peichhorn/lombok-pg/wiki/%40ExtensionMethod)
- `@FluentSetter` [info](https://github.com/peichhorn/lombok-pg/wiki/%40FluentSetter)
- `@Function` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Function)
- `@LazyGetter` [info](https://github.com/peichhorn/lombok-pg/wiki/%40LazyGetter)
- `@ListenerSupport` [info](https://github.com/peichhorn/lombok-pg/wiki/%40ListenerSupport)
- `@WriteLock` and `@ReadLock` [info](https://github.com/peichhorn/lombok-pg/wiki/%40WriteLock-%40ReadLock)
- `@Await`, `@Signal` and `@AwaitBeforeAndSignalAfter` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Await-%40Signal-%40SignalBeforeAwaitAfter)
- `@Predicate` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Predicate)
- `@Rethrow` and `@Rethrows` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Rethrow-%40Rethrows)
- `@Sanitize.Normalize` and `@Sanitize.With` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Sanitize)
- `@Singleton` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Singleton)
- `@SwingInvokeLater` and `@SwingInvokeAndWait` [info] (https://github.com/peichhorn/lombok-pg/wiki/%40SwingInvokeLater-%40SwingInvokeAndWait)
- `@Validate.NotEmpty`, `@Validate.NotNull` and `@Validate.With` [info](https://github.com/peichhorn/lombok-pg/wiki/%40Validate)
- `@VisibleForTesting` [info](https://github.com/peichhorn/lombok-pg/wiki/%40VisibleForTesting)

#### Interfaces:

- `Application` and `JVMAgent` [info](https://github.com/peichhorn/lombok-pg/wiki/Application-JVMAgent)

#### Methods:

- `tuple(expr1, expr2, ...)` [info](https://github.com/peichhorn/lombok-pg/wiki/Tupel)
- `yield(object)` [info](https://github.com/peichhorn/lombok-pg/wiki/Yield)


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
- [Check out lombok-pg extensions](https://github.com/peichhorn/lombok-pg/wiki)([old version](http://peichhorn.github.com/lombok-pg/))
- [How-To prepare a lombok version, that lombok-pg can run with](https://github.com/peichhorn/lombok-pg/wiki/Prepare-lombok)
