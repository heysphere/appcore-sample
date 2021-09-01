import SwiftUI

extension View {
  public func navigation<Value, Destination: View>(_ value: Binding<Value?>, destination: @escaping (Value) -> Destination) -> some View {
    modifier(NavigationModifier(value, destination: destination))
  }
}

private struct NavigationModifier<Value, Destination: View>: ViewModifier {
  @Binding var value: Value?
  let destination: (Value) -> Destination

  private var isActive: Binding<Bool> {
    Binding(get: { value != nil }, set: { newValue in value = newValue ? value : nil })
  }

  init(_ value: Binding<Value?>, destination: @escaping (Value) -> Destination) {
    self._value = value
    self.destination = destination
  }

  func body(content: Content) -> some View {
    content
      .background(
        NavigationLink(
          destination: makeDestination(),
          isActive: isActive,
          label: { EmptyView() }
        )
        .hidden()
      )
  }

  @ViewBuilder
  func makeDestination() -> some View {
    if let value = value {
      destination(value)
    }
  }
}
