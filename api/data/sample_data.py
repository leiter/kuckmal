"""Sample data generator for testing."""

import time
import random
from typing import List

from models import MediaEntry

CHANNELS = [
    "3Sat", "ARD", "ARTE.DE", "BR", "HR", "KiKA", "MDR",
    "NDR", "ORF", "PHOENIX", "RBB", "SR", "SRF", "SWR",
    "WDR", "ZDF", "ZDF-tivi"
]

THEMES_BY_CHANNEL = {
    "ARD": ["Tagesschau", "Tatort", "Sportschau", "Weltspiegel", "Hart aber fair", "Tagesthemen", "Report Mainz"],
    "ZDF": ["heute", "Terra X", "ZDF Magazin Royale", "Markus Lanz", "aspekte", "heute-show", "Frontal"],
    "3Sat": ["Kulturzeit", "nano", "scobel", "37 Grad", "Dokumentarfilm", "Buchzeit"],
    "ARTE.DE": ["Tracks", "Karambolage", "ARTE Journal", "Xenius", "ARTE Reportage", "ARTE Kurzschluss"],
    "NDR": ["Panorama", "extra 3", "Markt", "Kulturjournal", "NDR Dokumentation", "Nordtour"],
    "WDR": ["Aktuelle Stunde", "Monitor", "Quarks", "Die Story", "Lokalzeit", "WDR Reisen"],
    "BR": ["Rundschau", "quer", "Kontrovers", "Abendschau", "Capriccio", "Stationen"],
    "HR": ["hessenschau", "defacto", "maintower", "hr-fernsehen", "Alles Wissen"],
    "MDR": ["MDR aktuell", "Fakt ist!", "Selbstbestimmt", "Riverboat", "Sport im Osten"],
    "SWR": ["SWR Aktuell", "Zur Sache Baden-Württemberg", "Landesschau", "Marktcheck", "Odysso"],
    "RBB": ["Abendschau", "Brandenburg aktuell", "Kontraste", "rbb24"],
    "SR": ["aktueller bericht", "Wir im Saarland", "SAAR3"],
    "KiKA": ["KiKA LIVE", "logo!", "Wissen macht Ah!", "Die Sendung mit der Maus", "Checker Tobi"],
    "PHOENIX": ["phoenix der tag", "phoenix runde", "Dokumentationen", "phoenix history"],
    "ZDF-tivi": ["JoNaLu", "PUR+", "1, 2 oder 3", "Löwenzahn", "Die Biene Maja"],
    "ORF": ["Zeit im Bild", "ORF Thema", "Universum", "Report", "Kulturmontag"],
    "SRF": ["Tagesschau", "10vor10", "Arena", "Einstein", "Puls"],
}

DESCRIPTIONS = [
    "Eine spannende Reportage über aktuelle Ereignisse in Deutschland und der Welt.",
    "Hintergrundinformationen und Analysen zu den wichtigsten Themen des Tages.",
    "Wissenschaft verständlich erklärt: Forscher präsentieren ihre neuesten Erkenntnisse.",
    "Eine Dokumentation über Menschen, die Außergewöhnliches leisten.",
    "Unterhaltung und Information in einer einzigartigen Mischung.",
    "Kritische Berichterstattung über gesellschaftliche und politische Entwicklungen.",
    "Einblicke in Kultur und Kunst aus aller Welt.",
    "Sport-Highlights und Hintergrundberichte.",
    "Natur und Umwelt: Beeindruckende Bilder aus der Tier- und Pflanzenwelt.",
    "Geschichte zum Anfassen: Historische Ereignisse neu erzählt.",
]


def generate_sample_data(count: int = 500) -> List[dict]:
    """Generate realistic sample data for testing."""
    entries = []
    base_timestamp = int(time.time()) - (30 * 24 * 60 * 60)  # 30 days ago
    used_keys = set()

    for i in range(count):
        channel = random.choice(CHANNELS)
        themes = THEMES_BY_CHANNEL.get(channel, ["Allgemein"])
        theme = random.choice(themes)

        # Generate unique title
        episode_num = random.randint(1, 500)
        title = f"{theme} - Folge {episode_num}"

        # Ensure uniqueness
        key = (channel, theme, title)
        while key in used_keys:
            episode_num = random.randint(1, 9999)
            title = f"{theme} - Folge {episode_num}"
            key = (channel, theme, title)
        used_keys.add(key)

        entry = {
            "channel": channel,
            "theme": theme,
            "title": title,
            "date": f"{random.randint(1, 28):02d}.{random.randint(1, 12):02d}.2024",
            "time": f"{random.randint(6, 23):02d}:{random.choice(['00', '15', '30', '45'])} Uhr",
            "duration": f"{random.randint(15, 90)} Min",
            "sizeMB": f"{random.randint(200, 2000)} MB",
            "description": random.choice(DESCRIPTIONS),
            "url": f"https://example.com/video/{channel.lower()}/{theme.lower().replace(' ', '_')}/{episode_num}.mp4",
            "website": f"https://www.{channel.lower()}.de/sendungen/{theme.lower().replace(' ', '-')}",
            "subtitleUrl": "",
            "smallUrl": f"45|video_low_{episode_num}.mp4",
            "hdUrl": f"45|video_hd_{episode_num}.mp4",
            "timestamp": base_timestamp + (i * 3600),  # 1 hour apart
            "geo": random.choice(["DE", "DE-AT-CH", "DE-AT", ""]),
            "isNew": (i >= count - 50)  # Last 50 are "new"
        }
        entries.append(entry)

    return entries


def load_sample_data(db_session, count: int = 500) -> int:
    """Load sample data into the database."""
    entries = generate_sample_data(count)

    for entry_data in entries:
        entry = MediaEntry(**entry_data)
        db_session.merge(entry)

    db_session.commit()
    return len(entries)
