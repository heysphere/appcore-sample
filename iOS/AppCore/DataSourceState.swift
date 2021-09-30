import AppCoreObjC

public typealias DataSourceStateObjC = AppCoreObjC.DataSourceState

public enum DataSourceState<Value> {
  case loading
  case value(Value)
  case failure(Error)

  public init(_ state: DataSourceStateObjC<Value>) where Value: AnyObject {
    switch state {
    case let state as DataSourceStateValue<Value>:
      self = .value(state.value)
    case let state as DataSourceStateFailed:
      self = .failure(state.error)
    case is DataSourceStateLoading:
      self = .loading
    default:
      unrecognizedValue(state)
    }
  }

  internal init<Element>(_ state: DataSourceStateObjC<NSArray>) where Value == [Element] {
    switch DataSourceState<NSArray>(state) {
    case let .value(array):
      self = .value(array as! [Element])
    case let .failure(error):
      self = .failure(error)
    case .loading:
      self = .loading
    }
  }
}

@inline(never)
func unrecognizedValue(_ value: Any) -> Never {
  fatalError("Unrecognized value: \(value)")
}
