# Grab a small python release
FROM python:3.12-slim

# Sys dependencies to install
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working dir
WORKDIR /app

# Install library dependencies (ensure caching because of pytorch)
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt

# Copy and run the python nlp file
COPY . .
CMD ["python", "-u", "nlp_analysis.py"]