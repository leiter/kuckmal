import SwiftUI

// MARK: - Data Models

struct Channel: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let displayName: String
    let brandColor: Color
}

struct MediaItem: Identifiable {
    let id = UUID()
    let channel: String
    let theme: String
    let title: String
    let date: String
    let time: String
    let duration: String
    let size: String
    let description: String
}

// MARK: - Sample Data

struct SampleData {
    static let channels: [Channel] = [
        Channel(name: "3Sat", displayName: "3sat", brandColor: Color(red: 0.2, green: 0.2, blue: 0.2)),
        Channel(name: "ARD", displayName: "ARD", brandColor: Color(red: 0.0, green: 0.3, blue: 0.6)),
        Channel(name: "ARTE.DE", displayName: "ARTE", brandColor: Color(red: 0.9, green: 0.3, blue: 0.1)),
        Channel(name: "BR", displayName: "BR", brandColor: Color(red: 0.0, green: 0.4, blue: 0.7)),
        Channel(name: "HR", displayName: "HR", brandColor: Color(red: 0.0, green: 0.35, blue: 0.65)),
        Channel(name: "KiKA", displayName: "KiKA", brandColor: Color(red: 0.0, green: 0.6, blue: 0.3)),
        Channel(name: "MDR", displayName: "MDR", brandColor: Color(red: 0.0, green: 0.45, blue: 0.75)),
        Channel(name: "NDR", displayName: "NDR", brandColor: Color(red: 0.0, green: 0.35, blue: 0.6)),
        Channel(name: "PHOENIX", displayName: "phoenix", brandColor: Color(red: 0.9, green: 0.5, blue: 0.0)),
        Channel(name: "RBB", displayName: "RBB", brandColor: Color(red: 0.7, green: 0.0, blue: 0.2)),
        Channel(name: "SWR", displayName: "SWR", brandColor: Color(red: 0.0, green: 0.4, blue: 0.2)),
        Channel(name: "WDR", displayName: "WDR", brandColor: Color(red: 0.0, green: 0.3, blue: 0.5)),
        Channel(name: "ZDF", displayName: "ZDF", brandColor: Color(red: 0.9, green: 0.4, blue: 0.0))
    ]

    static let allThemes: [String] = [
        "Tagesschau", "Tatort", "Terra X", "Dokumentation",
        "Nachrichten", "Sport", "Kultur", "Wissenschaft",
        "Politik", "Wirtschaft", "Geschichte", "Natur"
    ]

    static let themesPerChannel: [String: [String]] = [
        "ARD": ["Tagesschau", "Tatort", "Sportschau", "Weltspiegel", "Hart aber fair"],
        "ZDF": ["heute", "Terra X", "ZDF Magazin Royale", "Markus Lanz", "aspekte"],
        "3Sat": ["Kulturzeit", "nano", "scobel", "37 Grad", "Dokumentarfilm"],
        "ARTE.DE": ["Tracks", "Karambolage", "ARTE Journal", "Xenius", "Kreatur"],
        "BR": ["Rundschau", "quer", "Puls", "Capriccio", "Kontrovers"],
        "NDR": ["Panorama", "extra 3", "Markt", "Kulturjournal", "Nordmagazin"],
        "WDR": ["Aktuelle Stunde", "Westpol", "Monitor", "Quarks", "Die Story"],
        "SWR": ["Landesschau", "Marktcheck", "Nachtcafe", "Kunscht!", "Sport im Dritten"],
        "HR": ["hessenschau", "defacto", "Maintower", "alles wissen", "Herkules"],
        "MDR": ["MDR aktuell", "Fakt ist!", "Riverboat", "MDR Garten", "Sachsenspiegel"],
        "RBB": ["Abendschau", "Kontraste", "rbb24", "Theodor", "Klartext"],
        "PHOENIX": ["phoenix runde", "phoenix plus", "Dokumentation", "History"],
        "KiKA": ["logo!", "Wissen macht Ah!", "Checker Tobi", "Die Sendung mit der Maus"]
    ]

    static func getThemesForChannel(_ channel: String) -> [String] {
        return themesPerChannel[channel] ?? allThemes
    }

    static func getTitlesForTheme(_ theme: String) -> [String] {
        return [
            "\(theme) - Folge 1",
            "\(theme) - Folge 2",
            "\(theme) - Spezial",
            "\(theme) - Best of",
            "\(theme) vom 25.07.2024",
            "\(theme) - Hintergrund",
            "\(theme) - Interview"
        ]
    }

    static func createMediaItem(channel: String, theme: String, title: String) -> MediaItem {
        return MediaItem(
            channel: channel,
            theme: theme,
            title: title,
            date: "25.07.2024",
            time: "20:15 Uhr",
            duration: "45 Min",
            size: "750 MB",
            description: "Eine spannende Sendung mit interessanten Inhalten und informativen Beitraegen zum Thema \(theme). Produziert von \(channel)."
        )
    }
}
