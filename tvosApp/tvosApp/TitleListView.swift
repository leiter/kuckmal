import SwiftUI

struct TitleListView: View {
    let titles: [String]
    @Binding var selectedTitle: String?
    let onTitleSelected: (String) -> Void

    var body: some View {
        if titles.isEmpty {
            VStack {
                Text("Keine Titel verfuegbar")
                    .foregroundColor(.secondary)
                    .font(.title3)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                LazyVStack(spacing: 16) {
                    ForEach(titles, id: \.self) { title in
                        Button {
                            print("Title button tapped: \(title)")
                            onTitleSelected(title)
                        } label: {
                            HStack {
                                Rectangle()
                                    .fill(selectedTitle == title ? Color.blue : Color.gray.opacity(0.5))
                                    .frame(width: 6)
                                    .cornerRadius(3)

                                Text(title)
                                    .font(.title3)
                                    .foregroundColor(.primary)

                                Spacer()

                                Image(systemName: "play.circle.fill")
                                    .font(.title2)
                                    .foregroundColor(.gray)
                            }
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.gray.opacity(0.1))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(selectedTitle == title ? Color.blue : Color.clear, lineWidth: 3)
                            )
                        }
                        .buttonStyle(.card)
                    }
                }
                .padding()
            }
        }
    }
}

#Preview {
    TitleListView(
        titles: ["Tatort - Folge 1", "Tatort - Folge 2", "Tatort - Spezial"],
        selectedTitle: .constant(nil),
        onTitleSelected: { _ in }
    )
}
