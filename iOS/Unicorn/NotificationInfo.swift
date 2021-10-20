import SwiftUI
import AppCore
import Combine

final class NotificationInfoViewModel: ObservableObject {
  typealias State = DataSourceState<AppCore.NotificationInfo>
  @Published var notificationState = State.loading

  private let sphereStore: SphereStore
  private var subscriptions = Set<AnyCancellable>()

  init(sphereStore: SphereStore, notificationId: String) {
    self.sphereStore = sphereStore
    let useCase = sphereStore.notificationInfoUseCase

    publisher(for: useCase.info(id: notificationId).state)
      .receive(on: DispatchQueue.main)
      .eraseToAnyPublisher()
      .map(DataSourceState.init)
      .sink { [weak self] state in
        self?.notificationState = state
      }
      .store(in: &subscriptions)
  }
}

struct NotificationInfo: View {
  @ObservedObject private var viewModel: NotificationInfoViewModel

  init(viewModel: NotificationInfoViewModel) {
    self.viewModel = viewModel
  }

  var body: some View {
    switch viewModel.notificationState {
    case .loading:
      ProgressView()
        .progressViewStyle(CircularProgressViewStyle())
    case .failure:
      Text("Failed to fetch details")
    case let .value(info):
      List {
        HStack {
          Text("Reason")
            .layoutPriority(1.0)
          Spacer()
          Text(info.reason)
            .layoutPriority(0.2)
        }

        HStack {
          Text("Repository")
            .layoutPriority(1.0)
          Spacer()
          Text(info.repositoryName)
            .layoutPriority(0.2)
        }

        HStack {
          Text("Pull Request ID")
            .layoutPriority(1.0)
          Spacer()
          Text("#\(info.subjectId)")
            .layoutPriority(0.2)
        }

        HStack {
          Text("Pull request")
            .layoutPriority(1.0)
          Spacer()
          Text(info.title)
            .layoutPriority(0.2)
            .multilineTextAlignment(.trailing)
        }
      }
      .navigationBarTitle(Text("Details"))
    }
  }
}

struct NotificationInfo_Previews: PreviewProvider {
  static var previews: some View {
    // TODO
//    NotificationInfo()
    EmptyView()
  }
}
