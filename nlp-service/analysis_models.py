from sqlalchemy import Column, BigInteger, Text, Integer, DateTime, ForeignKey, String, create_engine
from sqlalchemy.dialects.postgresql import ARRAY
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

""" Define the class to store job listing data (not all job postings will have all the information
which is why this class acts more of a guide than anything else of what information to store
"""
class Job_Listing(Base):
	__tablename__ = 'job_listing'
	id = Column(BigInteger, primary_key=True, index=True)
	page_id = Column(BigInteger, ForeignKey("crawled_page.id"))
	company = Column(String)
	job_title = Column(String)
	job_type = Column(String)
	skills = Column(ARRAY(String))
	experience_level = Column(String)
	job_description = Column(String)
	pay_range = Column(String)
	location = Column(String)
	posted_at = Column()
	source_site = Column(String)

Base.metadata.create_all(bind=engine)