import SwiftUI
import AppCore
import Combine

final class NotificationListViewModel {
  typealias State = PagingState<AppCoreObjC.Notification>
  @Published var notificationState = State(items: [], status: .loading)

  private let useCase: NotificationListUseCase
  private var subscriptions = Set<AnyCancellable>()

  init(useCase: NotificationListUseCase) {
    self.useCase = useCase

    publisher(for: useCase.notifications().state)
      .eraseToAnyPublisher()
      .assign(to: \.notificationState, on: self)
      .store(in: &subscriptions)
  }

  func next() {
    useCase.notifications().next()
  }
}

struct NotificationList: View {
  @State private var activeDetail: AppCoreObjC.Notification?
  private let viewModel: NotificationListViewModel

  init(viewModel: NotificationListViewModel) {
    self.viewModel = viewModel
  }

  var body: some View {
    switch viewModel.notificationState.status {
    case .loading, .hasMore, .failed:
      ProgressView()
        .progressViewStyle(CircularProgressViewStyle())
    default:
      List {
        ForEach(viewModel.notificationState.items) { notification in
          Button {
            activeDetail = notification
          } label: {
            NotificationRow(
              caption: notification.repositoryName,
              title: notification.title,
              trailingLabel: notification.subjectId
            )
          }
        }
        Button(action: loadMore) {
          Text("")
        }
        .onAppear {
          DispatchQueue.global(qos: .background).asyncAfter(deadline: DispatchTime(uptimeNanoseconds: 10)) {
            self.loadMore()
          }
        }
      }
      .navigation($activeDetail) { _ in
        NotificationInfo()
      }
      .navigationTitle(Text("Notifications"))
    }
  }

  private func loadMore() {
    viewModel.next()
  }
}

struct NotificationList_Previews: PreviewProvider {
  static var previews: some View {
    NavigationView {
        // TODO
//        NotificationList(
//            viewModel: .init(
//                useCase:
//            )
//        )
    }
  }
}

extension AppCoreObjC.Notification: Identifiable {}
