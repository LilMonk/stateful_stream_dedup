from confluent_kafka import Producer
import json
import time
import random
from datetime import datetime, timedelta

# Kafka configuration
import os

KAFKA_TOPIC = os.getenv("KAFKA_TOPIC", "iot_events")
KAFKA_BROKER = os.getenv("KAFKA_BROKER", "localhost:9092")
KAFKA_SASL_USERNAME = os.getenv("KAFKA_SASL_USERNAME", "username")
KAFKA_SASL_PASSWORD = os.getenv("KAFKA_SASL_PASSWORD", "password")
KAFKA_SECURITY_PROTOCOL = os.getenv("KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT")
KAFKA_SASL_MECHANISM = os.getenv("KAFKA_SASL_MECHANISM", "SCRAM-SHA-256")
NUM_PARTITIONS = os.getenv("NUM_PARTITIONS", 10)

print(f"KAFKA_TOPIC: {KAFKA_TOPIC}")
print(f"KAFKA_BROKER: {KAFKA_BROKER}")
print(f"KAFKA_SASL_USERNAME: {KAFKA_SASL_USERNAME}")
print(f"KAFKA_SASL_PASSWORD: {KAFKA_SASL_PASSWORD}")
print(f"KAFKA_SECURITY_PROTOCOL: {KAFKA_SECURITY_PROTOCOL}")
print(f"KAFKA_SASL_MECHANISM: {KAFKA_SASL_MECHANISM}")

# Initialize Kafka producer
producer = Producer(
    {
        "bootstrap.servers": KAFKA_BROKER,
        "security.protocol": KAFKA_SECURITY_PROTOCOL,
        "sasl.mechanisms": KAFKA_SASL_MECHANISM,
        "sasl.username": KAFKA_SASL_USERNAME,
        "sasl.password": KAFKA_SASL_PASSWORD,
    }
)


# Function to generate sensor data
def generate_sensor_data():
    return {
        "sensor_id": random.randint(1, NUM_PARTITIONS),
        "temperature": round(random.uniform(20.0, 30.0), 2),
        "humidity": round(random.uniform(30.0, 50.0), 2),
        "timestamp": datetime.now().isoformat(),
    }


def callback(err, event):
    if err:
        print(f'Produce to topic {event.topic()} failed for event: {event.key()}')
        raise Exception(f'Produce to topic {event.topic()} failed for event: {event.key()}')
    else:
        val = event.value().decode('utf8')
        print(f'{val} sent to partition {event.partition()}.')


# Function to simulate event generation
def generate_events():
    last_timestamp = datetime.now()
    while True:
        event = generate_sensor_data()

        # Randomly decide to use a previous timestamp or duplicate event
        if random.choice([True, False]):
            event["timestamp"] = (
                last_timestamp - timedelta(seconds=random.randint(1, 10))
            ).isoformat()
        elif random.choice([True, False]):
            event["timestamp"] = last_timestamp.isoformat()
        else:
            last_timestamp = datetime.now()
            event["timestamp"] = last_timestamp.isoformat()

        try:
            # Send event to Kafka
            producer.produce(
                KAFKA_TOPIC,
                key=str(event["sensor_id"]),
                value=json.dumps(event),
                on_delivery=callback
            )
            producer.flush()
            time.sleep(1)
        except Exception as e:
            print(f"Failed to send event: {event}, error: {e}")
            raise e


if __name__ == "__main__":
    generate_events()
