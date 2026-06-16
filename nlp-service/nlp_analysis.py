from confluent_kafka import Consumer, KafkaError
from transformers import pipeline
from analysis_models import SessionLocal, CrawledPage, Page_Analysis, Page_Entity
from datetime import datetime
import os
import json

# Fetch environemnt variables from .env file (create your own from the example)
broker = os.environ.get('KAFKA_BROKER', 'localhost:9094')
group_id = os.environ.get('KAFKA_GROUP_ID', 'default-nlp-group')
topic = os.environ.get('KAFKA_TOPIC_NLP_JOBS')

# Basic config made using the structure from user's own .env file
conf = {'bootstrap.servers': broker,
        'group.id': group_id,
        'auto.offset.reset': 'earliest'}

# Set up Kakfa consumer object to receive and commit messages
consumer = Consumer(conf)
consumer.subscribe([topic])

# Used to map out the sentiment of an analyzed page
SENTIMENT_MAP = {
    "LABEL_0": "NEGATIVE",
    "LABEL_1": "NEUTRAL",
    "LABEL_2": "POSITIVE"
}

# Initialize the three HuggingFace models (models are defined here)
summarizer = pipeline("summarization", model="facebook/bart-large-cnn")
sentiment_analyzer = pipeline("sentiment-analysis", model="cardiffnlp/twitter-roberta-base-sentiment")
ner_tagger = pipeline("ner", model="dslim/bert-base-NER", aggregation_strategy="simple")

def analyze_page(full_text):
    # Safe fallback values in case a model fails on an outlier page
    summary_text = "Analysis failed or text too dense."
    sentiment_text = "UNKNOWN"
    cleaned_entities = []
	# Attempt to extract a summary from the page contents
    try:
        # Grab BART's specific tokenizer
        tokenizer = summarizer.tokenizer
        # Enforce a 1024 token limit
        safe_tokens = tokenizer.encode(full_text[:4000], truncation=True, max_length=1024)
        # Decode back into a string to fit inside BART
        safe_summary_text = tokenizer.decode(safe_tokens, skip_special_tokens=True)
        # Extract the page summary using BART
        page_summary = summarizer(safe_summary_text, max_length=130, min_length=30, do_sample=False)
        summary_text = page_summary[0]['summary_text']
	# Print out exception if page summarization fails
    except Exception as e:
        print(f"    [!] Summarizer failed: {e}")
	# Attempt to extract sentiment from the page contents
    try:
        safe_sentiment_text = full_text[:2000]
		# Extract page sentiment using Roberta
        page_sentiment = sentiment_analyzer(safe_sentiment_text, truncation=True, max_length=512)
        raw_label = page_sentiment[0]['label']
		# Map the label to it's respective sentiment
        sentiment_text = SENTIMENT_MAP.get(raw_label, raw_label)
	# Print out exception if page sentiment fails
    except Exception as e:
        print(f"    [!] Sentiment Analyzer failed: {e}")

	# Attempt to extract entities from the page contents
    try:
        # Using the exact tokenizer math to stay under the 512 token limit
        tokenizer = ner_tagger.tokenizer
		# Enforce a 512 token limit
        safe_tokens = tokenizer.encode(full_text[:2000], truncation=True, max_length=510)
		# Decode back into a string to fit inside BERT
        safe_ner_text = tokenizer.decode(safe_tokens, skip_special_tokens=True)
        page_entities = ner_tagger(safe_ner_text)
		# Extract each entitey into a dict
        cleaned_entities = [{"type": entity['entity_group'], "word": entity['word']} for entity in page_entities]
	# Print out exception if page entity fails
    except Exception as e:
        print(f"    [!] NER Tagger failed: {e}")
    return {
        "page_summary": summary_text,
        "page_sentiment": sentiment_text,
        "page_entities": cleaned_entities
    }

# Create and return a Page_Analysis and a Page_Entity array utilizing a valid page's id and record
def create_page_stats(page_id, page_record):
	print(f"Processing Page ID: {page_id}")
	analysis = analyze_page(page_record.url_content)
	print(f"SUMMARY: {analysis['page_summary']}")
	print(f"SENTIMENT: {analysis['page_sentiment']}")
	print(f"ENTITIES FOUND: {len(analysis['page_entities'])}")
	page_analysis = Page_Analysis(page_id=page_id,
		page_summary=analysis["page_summary"], 
		page_sentiment=analysis["page_sentiment"],
		analyzed_at=datetime.now().isoformat(timespec='microseconds'))
	entities_collection = []
	dup_entities = set()
	for entity in analysis["page_entities"]:
		word = entity['word']
		type = entity['type']
		if word not in dup_entities:
			page_entity = Page_Entity(page_id=page_id,
				entity_word=word, entity_type=type)
			entities_collection.append(page_entity)
			dup_entities.add(word)
	return page_analysis, entities_collection

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
						page_analysis, page_entity = create_page_stats(page_id, page_record)
						db_session.add(page_analysis)
						db_session.add_all(page_entity)
						db_session.commit()
				except Exception as e:
					db_session.rollback()
					raise e
				finally:
					db_session.close()
			consumer.commit(message)
		except json.JSONDecodeError:
			print("Failed to decode payload as valid JSON")
		except Exception as db_error:
			print(f"Database operation failed: {db_error}")
except KeyboardInterrupt:
    pass
finally:
    consumer.close()