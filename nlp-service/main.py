from confluent_kafka import Consumer, KafkaError
import sys
import os
import json
from sqlalchemy import create_engine, Column, BigInteger, Text, Integer, DateTime
from sqlalchemy.orm import declarative_base, sessionmaker

# Fetch environemnt variables from .env file (create your own from the example)
broker = os.environ.get('KAFKA_BROKER', 'localhost:9094')
group_id = os.environ.get('KAFKA_GROUP_ID', 'default-nlp-group')
topic = os.environ.get('KAFKA_TOPIC_NLP_JOBS')
database_url = os.getenv("DATABASE_URL")

engine = create_engine(database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

class CrawledPage(Base):
	__tablename__ = 'crawled_page'
	id = Column(BigInteger, primary_key=True, index=True)
	url = Column(Text, nullable=False)
	title = Column(Text)
	content = Column(Text)
	crawled_at = Column(DateTime)
	depth = Column(Integer)

# Basic config made using the structure from user's own .env file
conf = {'bootstrap.servers': broker,
        'group.id': group_id,
        'auto.offset.reset': 'earliest'}

# Set up Kakfa consumer object to receive and commit messages
consumer = Consumer(conf)
consumer.subscribe([topic])

try:
	while True:
		# Poll Kafka
		message = consumer.poll(1.0)
		# Check whether the message received from kakfa is valid                
		if message is None:
				continue                
		if message.error():
				print(f"Consumer error: {message.error()}")
				continue
		# Print out the received message and attempt to commit it.
		raw_payload = message.value().decode('utf-8')
		print(f"Received message: {raw_payload}")
		try:
			job_data = json.loads(raw_payload)
			page_id = job_data.get('pageId')
			if page_id:
				db_session = SessionLocal()
				try:
					page_record = db_session.query(CrawledPage).filter(CrawledPage.id == page_id).first()
					if page_record:
						print("Content: {page_record.content}")
				finally:
					db_session.close()
			consumer.commit(message)
		except json.JSONDecodeError:
			print("Failed to decode payload as valid JSON")
		except Exception as db_error:
			print("Database operation failed: {db_error}")
except KeyboardInterrupt:
    pass
finally:
    consumer.close()