from confluent_kafka import Consumer, KafkaError
import sys
import os

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
                print(f"Received message: {message.value().decode('utf-8')}")
                try:
                        consumer.commit(message)
                except Exception as e:
                        print(f"Failed to commit: {e}")
except KeyboardInterrupt:
        pass
finally:
        consumer.close()