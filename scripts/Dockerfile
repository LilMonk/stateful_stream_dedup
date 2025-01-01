FROM python:3.12-slim-bullseye

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY requirements.txt /app
COPY kafka_samples_ingestion.py /app
COPY entrypoint.sh /app

# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Run the script
CMD ["./entrypoint.sh"]