import SwiftUI
import AppCore

struct NotificationInfo: View {
  var body: some View {
    List {
      HStack {
        Text("Reason")
          .layoutPriority(1.0)
        Spacer()
        Text("state_change")
          .layoutPriority(0.2)
      }

      HStack {
        Text("Repository")
          .layoutPriority(1.0)
        Spacer()
        Text("owner/repository")
          .layoutPriority(0.2)
      }

      HStack {
        Text("Pull request ID")
          .layoutPriority(1.0)
        Spacer()
        Text("#1234")
          .layoutPriority(0.2)
      }

      HStack {
        Text("Pull request")
          .layoutPriority(1.0)
        Spacer()
        Text("This is a very long name")
          .layoutPriority(0.2)
      }
    }
    .navigationBarTitle(Text("Details"))
  }
}

struct NotificationInfo_Previews: PreviewProvider {
  static var previews: some View {
    NotificationInfo()
  }
}
