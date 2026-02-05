"""SQLite database setup and session management."""

import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, scoped_session
from sqlalchemy.pool import StaticPool

from config import DATABASE_PATH


def get_engine():
    """Create SQLAlchemy engine for SQLite."""
    # Ensure data directory exists
    os.makedirs(os.path.dirname(DATABASE_PATH), exist_ok=True)

    engine = create_engine(
        f"sqlite:///{DATABASE_PATH}",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        echo=False
    )
    return engine


engine = get_engine()
SessionLocal = scoped_session(sessionmaker(bind=engine))


def init_db():
    """Initialize the database, creating tables if they don't exist."""
    from models import Base
    Base.metadata.create_all(bind=engine)


def get_db():
    """Get a database session. Use as a context manager or generator."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_db_session():
    """Get a database session directly (not a generator)."""
    return SessionLocal()
