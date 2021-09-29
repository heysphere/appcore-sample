final public class EmptyBackgroundTaskRunner: NSObject, BackgroundTaskRunner {
  public func beginTask(name: String) -> UInt64 {
    0
  }

  public func endTask(identifier: UInt64) {}
}
