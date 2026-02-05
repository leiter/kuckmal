"""SQLAlchemy models for the media database."""

from sqlalchemy import Column, Integer, String, Boolean, Index
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class MediaEntry(Base):
    """Media entry model matching the Filmliste schema."""

    __tablename__ = "media_entries"

    id = Column(Integer, primary_key=True, autoincrement=True)
    channel = Column(String, nullable=False, index=True)
    theme = Column(String, nullable=False, index=True)
    title = Column(String, nullable=False)
    date = Column(String, default="")
    time = Column(String, default="")
    duration = Column(String, default="")
    sizeMB = Column(String, default="")
    description = Column(String, default="")
    url = Column(String, default="")
    website = Column(String, default="")
    subtitleUrl = Column(String, default="")
    smallUrl = Column(String, default="")
    hdUrl = Column(String, default="")
    timestamp = Column(Integer, default=0, index=True)
    geo = Column(String, default="")
    isNew = Column(Boolean, default=False)

    __table_args__ = (
        Index('idx_unique_entry', 'channel', 'theme', 'title', unique=True),
        Index('idx_channel_theme', 'channel', 'theme'),
    )

    def to_dict(self):
        """Convert model to dictionary."""
        return {
            "id": self.id,
            "channel": self.channel,
            "theme": self.theme,
            "title": self.title,
            "date": self.date,
            "time": self.time,
            "duration": self.duration,
            "sizeMB": self.sizeMB,
            "description": self.description,
            "url": self.url,
            "website": self.website,
            "subtitleUrl": self.subtitleUrl,
            "smallUrl": self.smallUrl,
            "hdUrl": self.hdUrl,
            "timestamp": self.timestamp,
            "geo": self.geo,
            "isNew": self.isNew
        }
