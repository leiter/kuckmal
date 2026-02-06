import SwiftUI

struct ThemeListView: View {
    let themes: [String]
    @Binding var selectedTheme: String?
    let onThemeSelected: (String) -> Void

    var body: some View {
        if themes.isEmpty {
            VStack {
                Text("Keine Themen verfuegbar")
                    .foregroundColor(.secondary)
                    .font(.title3)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                LazyVStack(spacing: 16) {
                    ForEach(themes, id: \.self) { theme in
                        Button {
                            print("Theme button tapped: \(theme)")
                            onThemeSelected(theme)
                        } label: {
                            HStack {
                                Rectangle()
                                    .fill(Color.blue.opacity(0.6))
                                    .frame(width: 6)
                                    .cornerRadius(3)

                                Text(theme)
                                    .font(.title3)
                                    .foregroundColor(.primary)

                                Spacer()
                            }
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.gray.opacity(0.1))
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(selectedTheme == theme ? Color.blue : Color.clear, lineWidth: 3)
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
    ThemeListView(
        themes: ["Tagesschau", "Tatort", "Terra X", "Dokumentation"],
        selectedTheme: .constant(nil),
        onThemeSelected: { _ in }
    )
}
