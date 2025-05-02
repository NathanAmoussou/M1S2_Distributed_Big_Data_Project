# pythonApiYahooFinance/Dockerfile

# Use an official Python runtime as a parent image
FROM python:3.12-slim

# Set the working directory in the container
WORKDIR /app

# Copy the requirements file into the container at /app
# Create a requirements.txt file if you don't have one
COPY requirements.txt .

# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy the current directory contents into the container at /app
COPY . .

# Make port 5000 available to the world outside this container
EXPOSE 5000

# Define environment variable (optional, good practice)
ENV FLASK_APP=api.py
ENV FLASK_RUN_HOST=0.0.0.0

# Run api.py when the container launches using Flask development server
CMD ["flask", "run"]
# Or if you run directly: CMD ["python", "api.py"]