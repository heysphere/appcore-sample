import AppCoreObjC
import Combine
import os

public func publisher<Value>(for dataSource: DataSource<Value>) -> Publishers.AppCoreState<DataSourceState<Value>> {
  Publishers.AppCoreState { subject in
    let job = dataSource.state.launch(
      preference: .main,
      onEach: { subject.send(DataSourceState($0 as! DataSourceStateObjC<Value>)) },
      onCompletion: { _ in subject.send(completion: .finished) }
    )

    return AnyCancellable { job.cancel(cause: nil) }
  }
}
public func publisher<Value>(for dataSource: ListDataSource<Value>) -> Publishers.AppCoreState<DataSourceState<[Value]>> {
  Publishers.AppCoreState { subject in
    let job = dataSource.state.launch(
      preference: .main,
      onEach: { subject.send(DataSourceState($0 as! DataSourceStateObjC<NSArray>)) },
      onCompletion: { _ in subject.send(completion: .finished) }
    )

    return AnyCancellable { job.cancel(cause: nil) }
  }
}

public func publisher<Value>(for projection: Projection<Value>) -> Publishers.AppCoreState<Value> {
  Publishers.AppCoreState { subject in
    let job = projection.launch(
      preference: .main,
      onEach: { subject.send($0 as! Value) },
      onCompletion: { _ in subject.send(completion: .finished) }
    )

    return AnyCancellable { job.cancel(cause: nil) }
  }
}

public func publisher<Value>(for projection: ListProjection<Value>) -> Publishers.AppCoreState<[Value]> {
  Publishers.AppCoreState { subject in
    let job = projection.launch(
      preference: .main,
      onEach: { subject.send($0 as! [Value]) },
      onCompletion: { _ in subject.send(completion: .finished) }
    )

    return AnyCancellable { job.cancel(cause: nil) }
  }
}

public func publisher<Value>(for single: Single<Value>) -> Publishers.AppCoreSingle<Value, Error> {
  Publishers.AppCoreSingle { subject in
    let job = single.launch(
      preference: .main,
      onSuccess: {
        subject.send($0 as! Value)
        subject.send(completion: .finished)
      },
      onFailure: { error in
        subject.send(completion: .failure(error))
      }
    )

    return AnyCancellable { job.cancel(cause: nil) }
  }
}

extension Publishers {
  public struct AppCoreState<Value>: Publisher {
    public typealias Output = Value
    public typealias Failure = Never

    let onStart: (PassthroughSubject<Value, Never>) -> AnyCancellable

    public func receive<S>(subscriber: S) where S : Subscriber, Failure == S.Failure, Output == S.Input {
      let subject = PassthroughSubject<Value, Never>()
      let cancellable = OnceCancellable()
      subject
        .handleEvents(
          receiveCompletion: { _ in cancellable.cancel() },
          receiveCancel: { cancellable.cancel() }
        )
        .subscribe(subscriber)

      cancellable.inner = onStart(subject)
    }
  }

  public struct AppCoreSingle<Output: AnyObject, Failure: Error>: Publisher {
    let onStart: (PassthroughSubject<Output, Failure>) -> AnyCancellable

    public func receive<S>(subscriber: S) where S : Subscriber, Failure == S.Failure, Output == S.Input {
      let subject = PassthroughSubject<Output, Failure>()
      let cancellable = OnceCancellable()

      subject
        .handleEvents(
          receiveCompletion: { _ in cancellable.cancel() },
          receiveCancel: { cancellable.cancel() }
        )
        .subscribe(subscriber)

      cancellable.inner = onStart(subject)
    }
  }
}

private final class OnceCancellable: Cancellable {
  let isCancelled = UnsafeMutablePointer<Int32>.allocate(capacity: 1)
  var inner: AnyCancellable? {
    didSet {
      if isCancelled.pointee == 1 {
        inner?.cancel()
      }
    }
  }

  init() {
    isCancelled.initialize(to: 0)
  }

  deinit {
    isCancelled.deinitialize(count: 1)
    isCancelled.deallocate()
  }

  func cancel() {
    if OSAtomicCompareAndSwap32(0, 1, isCancelled) {
      inner?.cancel()
      inner = nil
    }
  }
}

