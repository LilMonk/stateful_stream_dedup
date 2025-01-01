.ONE_SHELL:
SHELL := /bin/bash
.DEFAULT_GOAL := help

CURRENT_DIR = $(shell pwd)
DOCKER_IMAGE_NAME = lilmonk/stateful_stream_dedup
DOCKER_TAG = latest
JAR_FILE = target/StatefulStreamDedup-1.0-SNAPSHOT.jar
LOG_CONFIG = log4j.properties
CONF_FILE = $(CURRENT_DIR)/src/main/resources/application.conf
DOT_ENV_FILE = $(CURRENT_DIR)/env/.local.env

##@ Commands
.PHONY: help
help:  ## Display this help
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make <command> \033[36m\033[0m\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

.PHONY: clean
clean:  ## Clean the project
	@echo "Cleaning the project"
	mvn clean

.PHONY: build
build:  ## Build the project
	@echo "Building the project"
	mvn clean package

.PHONY: test
test:  ## Run the tests
	@echo "Running the tests"
	mvn test
	
.PHONY: docker-build
docker-build: build  ## Build the docker image
	@echo "Building the docker image"
	docker compose build

.PHONY: docker-run
docker-run:  ## Run the docker image
	@echo "Running the docker image"
	docker compose up -d

.PHONY: docker-stop
docker-stop:  ## Stop the docker image
	@echo "Stopping the docker image"
	docker compose down

.PHONY: start-kafka-dev
start-kafka:  ## Start the kafka dev
	@echo "Starting the kafka"
	cd docker && docker compose up -d

.PHONY: stop-kafka-dev
stop-kafka:  ## Stop the kafka dev
	@echo "Stopping the kafka"
	cd docker && docker compose down