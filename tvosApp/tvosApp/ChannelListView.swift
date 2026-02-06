import SwiftUI

struct ChannelListView: View {
    let channels: [Channel]
    @Binding var selectedChannel: Channel?
    let onChannelSelected: (Channel) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(channels) { channel in
                    Button {
                        print("Channel button tapped: \(channel.name)")
                        if selectedChannel?.id == channel.id {
                            onChannelSelected(channel) // Toggle off handled by parent
                        } else {
                            onChannelSelected(channel)
                        }
                    } label: {
                        Text(channel.displayName)
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 60)
                            .background(channel.brandColor)
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(selectedChannel?.id == channel.id ? Color.white : Color.clear, lineWidth: 4)
                            )
                    }
                    .buttonStyle(.card)
                }
            }
            .padding()
        }
    }
}

#Preview {
    ChannelListView(
        channels: [
            Channel(name: "ARD", displayName: "ARD", brandColor: .blue),
            Channel(name: "ZDF", displayName: "ZDF", brandColor: .orange)
        ],
        selectedChannel: .constant(nil),
        onChannelSelected: { _ in }
    )
    .frame(width: 300)
}
