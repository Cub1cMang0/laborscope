from sqlalchemy import Column, BigInteger, Text, Integer, DateTime, ForeignKey, String, create_engine
from sqlalchemy.orm import declarative_base, sessionmaker
import os

database_url = os.getenv("DATABASE_URL")
engine = create_engine(database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# Define the class to pull the data from the CrawledPage published info from Kafka
class CrawledPage(Base):
	__tablename__ = 'crawled_page'
	id = Column(BigInteger, primary_key=True, index=True)
	url = Column(Text, nullable=False)
	title = Column(Text)
	url_content = Column(Text)
	crawled_at = Column(DateTime)
	depth = Column(Integer)

# Define the relationship between a crawled page and it's summarized content and sentiment
class Page_Analysis(Base):
	__tablename__ = 'page_analysis'
	id = Column(BigInteger, primary_key=True, index=True)
	page_id = Column(BigInteger, ForeignKey("crawled_page.id"))
	page_summary = Column(Text)
	page_sentiment = Column(String)
	analyzed_at = Column(DateTime)

# Define the relationship between a crawled page and it's entities
class Page_Entity(Base):
	__tablename__ = 'page_entities'
	id = Column(BigInteger, primary_key=True, index=True)
	page_id = Column(BigInteger, ForeignKey("crawled_page.id"))
	entity_word = Column(String)
	entity_type = Column(String)

Base.metadata.create_all(bind=engine)