# Stateful Stream Deduplication

Example Stateful Stream Deduplication project. 

## Overview

### Problem Statement
IOT devices are generating a large volume of data that needs to be processed in real-time. The data is sent to a Kafka topic
and needs to be deduplicated before being consumed by downstream applications. The data contains a timestamp field that can
be used to determine if an event is a duplicate. The deduplication process should be efficient and maintain state to handle
large volumes of data.

The IOT data stream may contain events with the same timestamps, previous event's timestamp or new event's timestamp.

### Solution

Deduplicate events by:
- Sending only the first event with a given timestamp to the downstream application (sink).
- Discarding subsequent events with the same timestamp.
- Discarding events with timestamps less than the previous event.
- Sending new events with timestamps greater than the previous event to the downstream application (sink).

The deduplication process uses a state store to track the latest processed timestamp:
- If the current event's timestamp is greater than the stored timestamp, update the state store and send the event downstream.
- If multiple events share the same timestamp, only the first event is sent, and the rest are discarded.

## Setup

To setup the project, clone the repository and run the applications using docker:

```bash
git clone https://github.com/lilmonk/stateful_stream_dedup.git
cd stateful_stream_dedup

# To build the applications docker images
make docker-build

# To start the applications
make docker-run

# To stop the applications
make docker-stop
```

## Usage

There is a kafka-ui that is available at `http://localhost:8080` to view the messages in the Kafka topic.

The default Kafka source topic is `iot_events` and the sink topic is `iot_dedup_events`.

You can check the `env` folders for environment variables files. By default the `docker-compose.yaml` uses `env/.prod.env`.

For local development check the `docker/docker-compose.yaml` or run `make start-kafka-dev`. This will start a local Kafka cluster with the same topics, zookeeper and kafka-ui.

## Contributing

We welcome contributions to the project. Please follow these steps to contribute:

1. Fork the repository
2. Create a new branch (`git checkout -b feature-branch`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature-branch`)
5. Create a new Pull Request

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
